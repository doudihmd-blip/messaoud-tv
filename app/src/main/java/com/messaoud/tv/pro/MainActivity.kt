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
import androidx.compose.foundation.lazy.itemsIndexed
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

data class ServerAccount(
    val name: String,
    val host: String,
    val user: String,
    val pass: String
)

class MainActivity : ComponentActivity() {

    private val currentAppVersion = "1.0.0"
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                                Toast.makeText(this@MainActivity, "تحديث متاح: $latestVer", Toast.LENGTH_LONG).show()
                                startActivity(intent)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        })
    }

    private fun openStream(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndTypeAndNormalize(Uri.parse(url), "video/*")
        }
        try {
            intent.setPackage("org.videolan.vlc")
            startActivity(intent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(intent, "تشغيل عبر المشغل:")
            startActivity(chooser)
        }
    }

    @Composable
    fun Neo4KDashboard() {
        val sp = getSharedPreferences("messaoud_tv_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        val savedAccounts = remember {
            val json = sp.getString("accounts", null)
            val list: MutableList<ServerAccount> = if (json != null) {
                gson.fromJson(json, object : TypeToken<MutableList<ServerAccount>>() {}.type)
            } else {
                mutableListOf(
                    ServerAccount("Falcon سيرفر", "http://falcon-sa.xyz:80", "9951535481", "7076609071")
                )
            }
            mutableStateListOf<ServerAccount>().apply { addAll(list) }
        }

        var activeIndex by remember { mutableStateOf(sp.getInt("active_idx", 0).coerceIn(0, (savedAccounts.size - 1).coerceAtLeast(0))) }
        var showServerDialog by remember { mutableStateOf(false) }
        var showAddDialog by remember { mutableStateOf(false) }

        val activeAccount = savedAccounts.getOrElse(activeIndex) { ServerAccount("سيرفر جديد", "http://falcon-sa.xyz:80", "", "") }

        fun persistData() {
            sp.edit().putString("accounts", gson.toJson(savedAccounts)).putInt("active_idx", activeIndex).apply()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0D131F), Color(0xFF05080E))))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MESSAOUD TV PRO", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        Text("السيرفر: ${activeAccount.name}", fontSize = 13.sp, color = Color(0xFF90A4AE))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1B2436)) {
                            Text("الصلاحية: 07/11/2027", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(onClick = { showServerDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), shape = RoundedCornerShape(8.dp)) {
                            Text("الاشتراكات ⚙️", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard("بث مباشر", "📺", Color(0xFF1976D2), Modifier.weight(1f)) {
                        openStream("${activeAccount.host}/get.php?username=${activeAccount.user}&password=${activeAccount.pass}&type=m3u_plus&output=m3u8")
                    }
                    DashboardCard("Sports رياضة", "⚽", Color(0xFF388E3C), Modifier.weight(1f)) {
                        openStream("${activeAccount.host}/get.php?username=${activeAccount.user}&password=${activeAccount.pass}&type=m3u_plus&output=m3u8")
                    }
                    DashboardCard("أفلام VOD", "🎬", Color(0xFFD32F2F), Modifier.weight(1f)) {
                        Toast.makeText(this@MainActivity, "مكتبة الأفلام - ${activeAccount.name}", Toast.LENGTH_SHORT).show()
                    }
                    DashboardCard("مسلسلات", "🎞️", Color(0xFF7B1FA2), Modifier.weight(1f)) {
                        Toast.makeText(this@MainActivity, "مكتبة المسلسلات - ${activeAccount.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (showServerDialog) {
                AlertDialog(
                    onDismissRequest = { showServerDialog = false },
                    title = { Text("قائمة الاشتراكات", color = Color(0xFFFF9800)) },
                    text = {
                        Column {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                itemsIndexed(savedAccounts) { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(if (index == activeIndex) Color(0xFF263238) else Color(0xFF141923), RoundedCornerShape(8.dp))
                                            .clickable {
                                                activeIndex = index
                                                persistData()
                                                showServerDialog = false
                                                Toast.makeText(this@MainActivity, "تم التحويل إلى ${item.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        if (index == activeIndex) Text("نشط ✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { showAddDialog = true; showServerDialog = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))) {
                                Text("+ إضافة سيرفر جديد", color = Color.White)
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showServerDialog = false }) { Text("إغلاق", color = Color(0xFFFF9800)) } }
                )
            }

            if (showAddDialog) {
                var name by remember { mutableStateOf("") }
                var host by remember { mutableStateOf("http://") }
                var user by remember { mutableStateOf("") }
                var pass by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة سيرفر جديد", color = Color(0xFFFF9800)) },
                    text = {
                        Column {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم السيرفر") })
                            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("رابط السيرفر") })
                            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("اسم المستخدم") })
                            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("كلمة المرور") })
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (name.isNotEmpty() && host.isNotEmpty()) {
                                    savedAccounts.add(ServerAccount(name, host, user, pass))
                                    activeIndex = savedAccounts.size - 1
                                    persistData()
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) { Text("حفظ وتشغيل", color = Color.Black) }
                    },
                    dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = Color.White) } }
                )
            }
        }
    }

    @Composable
    fun DashboardCard(title: String, icon: String, borderClr: Color, modifier: Modifier, onClick: () -> Unit) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .clickable { onClick() }
                .border(2.dp, borderClr.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF131826)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 42.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
