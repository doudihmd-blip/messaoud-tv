package com.messaoud.tv.pro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class ServerAccount(
    val name: String,
    val host: String,
    val user: String,
    val pass: String,
    var expDate: String = "جاري الفحص..."
)

class MainActivity : ComponentActivity() {

    private val currentAppVersion = "1.0.0"
    // رابط فحص التحديثات على مستودعك الخاص
    private val updateCheckUrl = "https://raw.githubusercontent.com/doudihmd-blip/messaoud-tv/main/version.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkForUpdates()

        setContent {
            Neo4KDashboard()
        }
    }

    private fun checkForUpdates() {
        val client = OkHttpClient()
        val request = Request.Builder().url(updateCheckUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonStr ->
                    try {
                        val obj = JSONObject(jsonStr)
                        val latestVer = obj.optString("latest_version")
                        val apkUrl = obj.optString("apk_url")
                        if (latestVer.isNotEmpty() && latestVer != currentAppVersion) {
                            runOnUiThread {
                                showUpdateDialog(latestVer, apkUrl)
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        })
    }

    private fun showUpdateDialog(newVersion: String, downloadUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
        Toast.makeText(this, "يوجد تحديث جديد متاح ($newVersion)! جاري الفتح...", Toast.LENGTH_LONG).show()
        startActivity(intent)
    }

    private fun openVlcStream(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(Uri.parse(url), "video/*")
            setPackage("org.videolan.vlc")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndTypeAndNormalize(Uri.parse(url), "video/*")
            }
            startActivity(Intent.createChooser(genericIntent, "تشغيل عبر:"))
        }
    }

    @Composable
    fun Neo4KDashboard() {
        val sp = getSharedPreferences("messaoud_tv_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        var accountsList by remember {
            val savedJson = sp.getString("accounts", null)
            val list: MutableList<ServerAccount> = if (savedJson != null) {
                gson.fromJson(savedJson, object : TypeToken<MutableList<ServerAccount>>() {}.type)
            } else {
                mutableListOf(
                    ServerAccount("Falcon سيرفر", "http://falcon-sa.xyz:80", "9951535481", "7076609071")
                )
            }
            mutableStateOf(list)
        }

        var activeIndex by remember { mutableStateOf(sp.getInt("active_idx", 0).coerceIn(0, (accountsList.size - 1).coerceAtLeast(0))) }
        var showServerDialog by remember { mutableStateOf(false) }
        var showAddDialog by remember { mutableStateOf(false) }

        val activeAccount = accountsList.getOrNull(activeIndex) ?: ServerAccount("لا يوجد سيرفر", "", "", "")

        fun saveAccounts() {
            sp.edit().putString("accounts", gson.toJson(accountsList)).putInt("active_idx", activeIndex).apply()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F141E), Color(0xFF06090E))
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // شريط الرأس: الشعار، بيانات السيرفر، وتاريخ الصلاحية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MESSAOUD TV PRO",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = "السيرفر النشط: ${activeAccount.name}",
                            fontSize = 13.sp,
                            color = Color(0xFF90A4AE)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E2638)
                        ) {
                            Text(
                                text = "الصلاحية: 07/11/2027",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { showServerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("الاشتراكات ⚙️", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // بطاقات واجهة Neo 4K الرئيسية
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CategoryCard("بث مباشر", "📺", Color(0xFF1565C0), Modifier.weight(1f)) {
                        val m3u = "${activeAccount.host}/get.php?username=${activeAccount.user}&password=${activeAccount.pass}&type=m3u_plus&output=m3u8"
                        openVlcStream(m3u)
                    }
                    CategoryCard("Sports رياضة", "⚽", Color(0xFF2E7D32), Modifier.weight(1f)) {
                        val m3u = "${activeAccount.host}/get.php?username=${activeAccount.user}&password=${activeAccount.pass}&type=m3u_plus&output=m3u8"
                        openVlcStream(m3u)
                    }
                    CategoryCard("أفلام VOD", "🎬", Color(0xFFC62828), Modifier.weight(1f)) {
                        Toast.makeText(this@MainActivity, "جاري فتح قائمة أفلام ${activeAccount.name}...", Toast.LENGTH_SHORT).show()
                    }
                    CategoryCard("مسلسلات", "🎞️", Color(0xFF6A1B9A), Modifier.weight(1f)) {
                        Toast.makeText(this@MainActivity, "جاري فتح قائمة مسلسلات ${activeAccount.name}...", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // نافذة التبديل بين الاشتراكات
            if (showServerDialog) {
                AlertDialog(
                    onDismissRequest = { showServerDialog = false },
                    title = { Text("إدارة الاشتراكات والسيرفرات", color = Color(0xFFFF9800)) },
                    text = {
                        Column {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(accountsList.indices.toList()) { idx ->
                                    val acc = accountsList[idx]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(
                                                if (idx == activeIndex) Color(0xFF263238) else Color(0xFF131924),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                activeIndex = idx
                                                saveAccounts()
                                                showServerDialog = false
                                                Toast.makeText(this@MainActivity, "تم التحويل إلى ${acc.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(acc.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (idx == activeIndex) Text("نشط ✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddDialog = true; showServerDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                            ) {
                                Text("+ إضافة سيرفر جديد", color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showServerDialog = false }) { Text("إغلاق", color = Color(0xFFFF9800)) }
                    }
                )
            }

            // نافذة إضافة اشتراك جديد
            if (showAddDialog) {
                var sName by remember { mutableStateOf("") }
                var sHost by remember { mutableStateOf("http://") }
                var sUser by remember { mutableStateOf("") }
                var sPass by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة اشتراك جديد", color = Color(0xFFFF9800)) },
                    text = {
                        Column {
                            OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم السيرفر") })
                            OutlinedTextField(value = sHost, onValueChange = { sHost = it }, label = { Text("رابط السيرفر مع المنفذ") })
                            OutlinedTextField(value = sUser, onValueChange = { sUser = it }, label = { Text("اسم المستخدم") })
                            OutlinedTextField(value = sPass, onValueChange = { sPass = it }, label = { Text("كلمة المرور") })
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (sName.isNotEmpty() && sHost.isNotEmpty()) {
                                    accountsList.add(ServerAccount(sName, sHost, sUser, sPass))
                                    activeIndex = accountsList.size - 1
                                    saveAccounts()
                                    showAddDialog = false
                                    Toast.makeText(this@MainActivity, "تمت إضافة السيرفر وتشغيله بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) { Text("حفظ وتشغيل", color = Color.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = Color.White) }
                    }
                )
            }
        }
    }

    @Composable
    fun CategoryCard(title: String, icon: String, borderColor: Color, modifier: Modifier, onClick: () -> Unit) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .clickable { onClick() }
                .border(2.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141A29)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
