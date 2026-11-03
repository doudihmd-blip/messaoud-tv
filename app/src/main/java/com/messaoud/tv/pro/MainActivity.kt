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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.messaoud.tv.pro.ui.theme.*

data class ServerAccount(
    val name: String,
    val host: String,
    val user: String,
    val pass: String,
    val expDate: String = "2027-11-07"
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeoDashboard()
        }
    }

    private fun launchJustPlayer(url: String) {
        val uri = Uri.parse(url)
        val jpIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            setPackage("com.brouken.player")
        }
        try {
            startActivity(jpIntent)
        } catch (_: Exception) {
            val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
            }
            try {
                startActivity(Intent.createChooser(genericIntent, "تشغيل عبر Just Player أو VLC:"))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }
    }

    @Composable
    fun NeoDashboard() {
        val sp = getSharedPreferences("messaoud_tv_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val accounts = remember {
            val raw = sp.getString("accounts_json", null)
            val initial: MutableList<ServerAccount> = if (raw != null) {
                gson.fromJson(raw, object : TypeToken<MutableList<ServerAccount>>() {}.type)
            } else {
                mutableListOf(
                    ServerAccount("FALCON VIP 4K", "http://falcon-sa.xyz:80", "9951535481", "7076609071")
                )
            }
            mutableStateListOf<ServerAccount>().apply { addAll(initial) }
        }

        var selectedIndex by remember { mutableStateOf(sp.getInt("active_acc_idx", 0).coerceIn(0, (accounts.size - 1).coerceAtLeast(0))) }
        var showServerManager by remember { mutableStateOf(false) }
        var showNewAccountDialog by remember { mutableStateOf(false) }

        val active = accounts.getOrElse(selectedIndex) { ServerAccount("سيرفر جديد", "", "", "") }

        fun saveState() {
            sp.edit().putString("accounts_json", gson.toJson(accounts)).putInt("active_acc_idx", selectedIndex).apply()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF141D2F), BgDark),
                        radius = 1600f
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar (شريط علوي احترافي)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "MESSAOUD",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldAccent
                            ) {
                                Text(
                                    "TV PRO 4K",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "السيرفر الحالي: ${active.name}",
                            fontSize = 13.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF101B2B),
                            modifier = Modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "صلاحية: ${active.expDate}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { showServerManager = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("الاشتراكات ⚙️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // بطاقات Neo 4K الأربعة الفخمة
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NeoCard(
                        title = "بث مباشر",
                        sub = "LIVE CHANNELS",
                        icon = "📺",
                        accent = LiveColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        launchJustPlayer("${active.host}/live/${active.user}/${active.pass}/1.ts")
                    }

                    NeoCard(
                        title = "مباريات ورياضة",
                        sub = "SPORTS ARENA",
                        icon = "⚽",
                        accent = SportColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        launchJustPlayer("${active.host}/live/${active.user}/${active.pass}/1.ts")
                    }

                    NeoCard(
                        title = "مكتبة الأفلام",
                        sub = "VOD MOVIES",
                        icon = "🎬",
                        accent = VodColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Toast.makeText(this@MainActivity, "مكتبة الأفلام - ${active.name}", Toast.LENGTH_SHORT).show()
                    }

                    NeoCard(
                        title = "المسلسلات",
                        sub = "TV SERIES",
                        icon = "🎞️",
                        accent = SeriesColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Toast.makeText(this@MainActivity, "مكتبة المسلسلات - ${active.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // مودال إدارة الاشتراكات
            if (showServerManager) {
                AlertDialog(
                    onDismissRequest = { showServerManager = false },
                    containerColor = CardDark,
                    title = { Text("قائمة الاشتراكات المحفوظة", color = GoldAccent, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                itemsIndexed(accounts) { idx, item ->
                                    val isCur = idx == selectedIndex
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCur) Color(0xFF1E293B) else Color(0xFF0F1522))
                                            .border(1.dp, if (isCur) GoldAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedIndex = idx
                                                saveState()
                                                showServerManager = false
                                                Toast.makeText(this@MainActivity, "تم تفعيل: ${item.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        if (isCur) Text("نشط ✓", color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showNewAccountDialog = true; showServerManager = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ إضافة اشتراك جديد", color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showServerManager = false }) { Text("إغلاق", color = GoldAccent) }
                    }
                )
            }

            // مودال إضافة اشتراك
            if (showNewAccountDialog) {
                var sName by remember { mutableStateOf("") }
                var sHost by remember { mutableStateOf("http://") }
                var sUser by remember { mutableStateOf("") }
                var sPass by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showNewAccountDialog = false },
                    containerColor = CardDark,
                    title = { Text("إضافة سيرفر جديد", color = GoldAccent, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم السيرفر") })
                            OutlinedTextField(value = sHost, onValueChange = { sHost = it }, label = { Text("رابط السيرفر") })
                            OutlinedTextField(value = sUser, onValueChange = { sUser = it }, label = { Text("اسم المستخدم") })
                            OutlinedTextField(value = sPass, onValueChange = { sPass = it }, label = { Text("كلمة المرور") })
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (sName.isNotEmpty() && sHost.isNotEmpty()) {
                                    accounts.add(ServerAccount(sName, sHost, sUser, sPass))
                                    selectedIndex = accounts.size - 1
                                    saveState()
                                    showNewAccountDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) { Text("حفظ وتشغيل", color = Color.Black, fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewAccountDialog = false }) { Text("إلغاء", color = Color.White) }
                    }
                )
            }
        }
    }

    @Composable
    fun NeoCard(title: String, sub: String, icon: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .shadow(12.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .clickable { onClick() }
                .border(1.5.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.8f), Color(0xFF1E293B))), RoundedCornerShape(18.dp)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CardDark,
                                Color(0xFF0A0E17)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                // شريط التوهج اللوني العلوي
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, accent, Color.Transparent)))
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.15f),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 38.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        sub,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
