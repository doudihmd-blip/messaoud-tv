package com.messaoud.tv.pro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ServerAccount(
    val name: String,
    val host: String,
    val user: String,
    val pass: String,
    val expDate: String = "2027-11-07"
)

data class MediaItem(
    val id: String,
    val name: String,
    val streamType: String, // live, movie, series
    val streamId: String,
    val containerExt: String = "mp4"
)

class MainActivity : ComponentActivity() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeoApp()
        }
    }

    private fun playVideo(url: String) {
        val uri = Uri.parse(url)
        val jpIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            setPackage("com.brouken.player")
        }
        try {
            startActivity(jpIntent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri).apply {
                setDataAndType(uri, "video/*")
            }, "تشغيل عبر Just Player:")
            startActivity(chooser)
        }
    }

    @Composable
    fun NeoApp() {
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
        val active = accounts.getOrElse(selectedIndex) { ServerAccount("سيرفر", "", "", "") }

        var currentView by remember { mutableStateOf("HOME") } // HOME, LIVE, SPORTS, VOD, SERIES
        var viewTitle by remember { mutableStateOf("") }

        BackHandler(enabled = currentView != "HOME") {
            currentView = "HOME"
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color(0xFF141D2F), BgDark), radius = 1600f))
        ) {
            if (currentView == "HOME") {
                HomeScreen(
                    accounts = accounts,
                    activeIndex = selectedIndex,
                    onSelectAccount = { idx ->
                        selectedIndex = idx
                        sp.edit().putString("accounts_json", gson.toJson(accounts)).putInt("active_acc_idx", idx).apply()
                    },
                    onOpenCategory = { cat, title ->
                        currentView = cat
                        viewTitle = title
                    }
                )
            } else {
                MediaListScreen(
                    account = active,
                    categoryType = currentView,
                    title = viewTitle,
                    onBack = { currentView = "HOME" },
                    onItemClick = { item ->
                        val streamUrl = when (item.streamType) {
                            "live" -> "${active.host}/live/${active.user}/${active.pass}/${item.streamId}.m3u8"
                            "movie" -> "${active.host}/movie/${active.user}/${active.pass}/${item.streamId}.${item.containerExt}"
                            "series" -> "${active.host}/series/${active.user}/${active.pass}/${item.streamId}.mp4"
                            else -> "${active.host}/live/${active.user}/${active.pass}/${item.streamId}.ts"
                        }
                        playVideo(streamUrl)
                    }
                )
            }
        }
    }

    @Composable
    fun HomeScreen(
        accounts: MutableList<ServerAccount>,
        activeIndex: Int,
        onSelectAccount: (Int) -> Unit,
        onOpenCategory: (String, String) -> Unit
    ) {
        var showServerManager by remember { mutableStateOf(false) }
        var showNewDialog by remember { mutableStateOf(false) }
        val active = accounts.getOrElse(activeIndex) { ServerAccount("سيرفر", "", "", "") }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MESSAOUD", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = GoldAccent) {
                            Text("TV PRO 4K", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("السيرفر الحالي: ${active.name}", fontSize = 13.sp, color = TextMuted)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF101B2B), modifier = Modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("صلاحية: ${active.expDate}", fontSize = 12.sp, color = Color(0xFFE2E8F0), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { showServerManager = true }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent), shape = RoundedCornerShape(10.dp)) {
                        Text("الاشتراكات ⚙️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Neo 4K 4 Cards
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NeoCard("بث مباشر", "LIVE CHANNELS", "📺", LiveColor, Modifier.weight(1f)) {
                    onOpenCategory("LIVE", "البث المباشر والقنوات")
                }
                NeoCard("مباريات ورياضة", "SPORTS ARENA", "⚽", SportColor, Modifier.weight(1f)) {
                    onOpenCategory("SPORTS", "قنوات الرياضة والمباريات")
                }
                NeoCard("مكتبة الأفلام", "VOD MOVIES", "🎬", VodColor, Modifier.weight(1f)) {
                    onOpenCategory("VOD", "مكتبة الأفلام الحديثة")
                }
                NeoCard("المسلسلات", "TV SERIES", "🎞️", SeriesColor, Modifier.weight(1f)) {
                    onOpenCategory("SERIES", "عالم المسلسلات")
                }
            }
        }

        if (showServerManager) {
            AlertDialog(
                onDismissRequest = { showServerManager = false },
                containerColor = CardDark,
                title = { Text("قائمة الاشتراكات", color = GoldAccent, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            itemsIndexed(accounts) { idx, item ->
                                val isCur = idx == activeIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCur) Color(0xFF1E293B) else Color(0xFF0F1522))
                                        .border(1.dp, if (isCur) GoldAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectAccount(idx)
                                            showServerManager = false
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
                        Button(onClick = { showNewDialog = true; showServerManager = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))) {
                            Text("+ إضافة اشتراك جديد", color = Color.White)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showServerManager = false }) { Text("إغلاق", color = GoldAccent) } }
            )
        }

        if (showNewDialog) {
            var sName by remember { mutableStateOf("") }
            var sHost by remember { mutableStateOf("http://") }
            var sUser by remember { mutableStateOf("") }
            var sPass by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showNewDialog = false },
                containerColor = CardDark,
                title = { Text("إضافة اشتراك جديد", color = GoldAccent, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم السيرفر") })
                        OutlinedTextField(value = sHost, onValueChange = { sHost = it }, label = { Text("رابط السيرفر") })
                        OutlinedTextField(value = sUser, onValueChange = { sUser = it }, label = { Text("اسم المستخدم") })
                        OutlinedTextField(value = sPass, onValueChange = { sPass = it }, label = { Text("كلمة المرور") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (sName.isNotEmpty() && sHost.isNotEmpty()) {
                            accounts.add(ServerAccount(sName, sHost, sUser, sPass))
                            onSelectAccount(accounts.size - 1)
                            showNewDialog = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) {
                        Text("حفظ", color = Color.Black)
                    }
                },
                dismissButton = { TextButton(onClick = { showNewDialog = false }) { Text("إلغاء", color = Color.White) } }
            )
        }
    }

    @Composable
    fun MediaListScreen(
        account: ServerAccount,
        categoryType: String,
        title: String,
        onBack: () -> Unit,
        onItemClick: (MediaItem) -> Unit
    ) {
        val itemsList = remember { mutableStateListOf<MediaItem>() }
        var isLoading by remember { mutableStateOf(true) }
        var searchQuery by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        LaunchedEffect(categoryType) {
            scope.launch {
                isLoading = true
                itemsList.clear()
                val fetched = fetchXtreamMedia(account, categoryType)
                itemsList.addAll(fetched)
                isLoading = false
            }
        }

        val filteredItems = itemsList.filter { it.name.contains(searchQuery, ignoreCase = true) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(8.dp)) {
                        Text("← رجوع", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم القناة أو الفيلم...", color = TextMuted) },
                    modifier = Modifier.width(280.dp).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldAccent)
                }
            } else if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد عناصر متاحة حالياً، تحقق من صحة بيانات السيرفر أو الاتصال", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onItemClick(item) },
                            color = CardDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when (item.streamType) {
                                            "live" -> "📺"
                                            "movie" -> "🎬"
                                            else -> "🎞️"
                                        },
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(item.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("تشغيل ▶", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchXtreamMedia(acc: ServerAccount, catType: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<MediaItem>()
        val action = when (catType) {
            "LIVE", "SPORTS" -> "get_live_streams"
            "VOD" -> "get_vod_streams"
            "SERIES" -> "get_series"
            else -> "get_live_streams"
        }

        val url = "${acc.host}/player_api.php?username=${acc.user}&password=${acc.pass}&action=$action"
        try {
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val array = JSONArray(body)

            for (i in 0 until array.length().coerceAtMost(300)) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "عنصر بدون اسم")
                if (catType == "SPORTS" && !name.contains("bein", ignoreCase = true) && !name.contains("sport", ignoreCase = true) && !name.contains("ssc", ignoreCase = true)) {
                    continue
                }
                val streamId = obj.optString("stream_id", obj.optString("series_id", ""))
                val ext = obj.optString("container_extension", "mp4")
                val type = when (catType) {
                    "VOD" -> "movie"
                    "SERIES" -> "series"
                    else -> "live"
                }
                result.add(MediaItem(id = "$i", name = name, streamType = type, streamId = streamId, containerExt = ext))
            }
        } catch (_: Exception) {}
        result
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
                    .background(Brush.verticalGradient(listOf(CardDark, Color(0xFF0A0E17))))
                    .padding(18.dp)
            ) {
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
                    Surface(shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = 0.15f), modifier = Modifier.size(76.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 38.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextMuted, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}
