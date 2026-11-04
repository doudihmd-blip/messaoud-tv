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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
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
    val streamType: String,
    val streamId: String,
    val containerExt: String = "mp4"
)

class MainActivity : ComponentActivity() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val configUrl = "https://raw.githubusercontent.com/doudihmd-blip/messaoud-tv/main/version.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }

    private fun playVideo(url: String) {
        val uri = Uri.parse(url)
        val jpIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            setPackage("com.brouken.player")
            putExtra("android.media.intent.extra.HTTP_HEADERS", arrayOf("User-Agent: IPTVSmartersPro/1.0.0"))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    fun AppRoot() {
        var isBlocked by remember { mutableStateOf(false) }
        var blockMsg by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().header("User-Agent", "IPTVSmartersPro/1.0.0 (Linux; Android 12)").url(configUrl).build()
                    val resp = httpClient.newCall(req).execute()
                    val body = resp.body?.string()
                    if (body != null) {
                        val obj = JSONObject(body)
                        val active = obj.optBoolean("is_active", true)
                        if (!active) {
                            isBlocked = true
                            blockMsg = obj.optString("blocked_message", "عذراً، الخدمة متوقفة حالياً.")
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        if (isBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDark)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardDark,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                    modifier = Modifier.widthIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("تم إيقاف الخدمة", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(blockMsg, color = Color(0xFFE2E8F0), fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { finishAffinity() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("خروج", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            NeoApp()
        }
    }

    @Composable
    fun NeoApp() {
        val sp = getSharedPreferences("messaoud_tv_data", Context.MODE_PRIVATE)
        val gson = Gson()

        val accounts = remember {
            val raw = sp.getString("accounts_json", null)
            val list: MutableList<ServerAccount> = if (raw != null) {
                gson.fromJson(raw, object : TypeToken<MutableList<ServerAccount>>() {}.type)
            } else {
                mutableListOf(
                    ServerAccount("FALCON VIP 4K", "http://falcon-sa.xyz:80", "9951535481", "7076609071", "2027-11-07"),
                    ServerAccount("NEO 4K", "http://neo-aps.me:80", "8890766bc1dd", "6e4om10jzz", "2026-12-31"),
                    ServerAccount("SHARK VIP 4K", "http://vip2.sharkhost.xyz:80", "9143315182", "7352465335", "2027-01-01")
                )
            }
            mutableStateListOf<ServerAccount>().apply { addAll(list) }
        }

        var selectedIndex by remember { mutableStateOf(sp.getInt("active_acc_idx", 0).coerceIn(0, (accounts.size - 1).coerceAtLeast(0))) }
        val active = accounts.getOrElse(selectedIndex) { ServerAccount("سيرفر", "", "", "") }

        var currentView by remember { mutableStateOf("HOME") }
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
                            "live" -> "${active.host}/live/${active.user}/${active.pass}/${item.streamId}.ts"
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
        val sp = getSharedPreferences("messaoud_tv_data", Context.MODE_PRIVATE)
        val gson = Gson()
        var showServerManager by remember { mutableStateOf(false) }
        var showNewDialog by remember { mutableStateOf(false) }
        var showPinDialog by remember { mutableStateOf(false) }
        val active = accounts.getOrElse(activeIndex) { ServerAccount("سيرفر", "", "", "") }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
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
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF334155),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(containerColor = CardDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                            sp.edit().putInt("active_acc_idx", idx).apply()
                                            onSelectAccount(idx)
                                            showServerManager = false
                                        }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (idx >= 2) {
                                            IconButton(
                                                onClick = {
                                                    if (idx < accounts.size) {
                                                        accounts.removeAt(idx)
                                                        sp.edit().putString("accounts_json", gson.toJson(accounts.toList())).apply()
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Text("✕", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showNewDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("+ إضافة يدوي", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showServerManager = false }) {
                        Text("إغلاق", color = Color.White)
                    }
                }
            )
        }

        if (showPinDialog) {
            var pin by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = CardDark,
                title = { Text("فتح حسابك الخاص", color = GoldAccent, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("أدخل رمز الحماية لإظهار سيرفر 2027 الخاص بك:", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("رمز الدخول") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pin == "7755") {
                                val privateAcc = ServerAccount("SHARK VIP 4K (حسابك 2027)", "http://vip2.sharkhost.xyz:80", "9143315182", "7352465335", "2027-03-15")
                                if (accounts.none { it.host.contains("viupru4k") }) {
                                    accounts.add(privateAcc)
                                }
                                onSelectAccount(accounts.indexOfFirst { it.host.contains("viupru4k") })
                                sp.edit().putString("accounts_json", gson.toJson(accounts)).apply()
                                showPinDialog = false
                                Toast.makeText(this@MainActivity, "تم تفعيل سيرفرك الخاص بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "الرمز غير صحيح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) { Text("تأكيد", color = Color.Black) }
                },
                dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("إلغاء", color = Color.White) } }
            )
        }

        if (showNewDialog) {
            var sName by remember { mutableStateOf("") }
            var sHost by remember { mutableStateOf("http://") }
            var sUser by remember { mutableStateOf("") }
            var sPass by remember { mutableStateOf("") }

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = Color(0xFF334155),
                focusedLabelColor = GoldAccent,
                unfocusedLabelColor = TextMuted,
                cursorColor = GoldAccent
            )

            AlertDialog(
                onDismissRequest = { showNewDialog = false },
                containerColor = CardDark,
                modifier = Modifier.widthIn(max = 620.dp),
                title = { Text("إضافة اشتراك جديد", color = GoldAccent, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sName,
                                onValueChange = { sName = it },
                                label = { Text("اسم السيرفر") },
                                colors = fieldColors,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sHost,
                                onValueChange = { sHost = it },
                                label = { Text("رابط السيرفر") },
                                colors = fieldColors,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sUser,
                                onValueChange = { sUser = it },
                                label = { Text("اسم المستخدم") },
                                colors = fieldColors,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sPass,
                                onValueChange = { sPass = it },
                                label = { Text("كلمة المرور") },
                                colors = fieldColors,
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
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
                        Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text("بحث...", color = TextMuted) },
                    modifier = Modifier.width(260.dp).height(50.dp),
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
                    Text("لا توجد عناصر متاحة حالياً، تأكد من الاتصال أو بيانات السيرفر", color = TextMuted)
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
                                        fontSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(item.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPro/1.0.0 (Linux; Android 12)")
                .header("Accept", "*/*")
                .build()
            val resp = httpClient.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val trimmed = body.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONArray()
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "بدون اسم")
                if (catType == "SPORTS") {
                    val n = name.lowercase()
                    val isSport = n.contains("bein") || n.contains("sport") || n.contains("ssc") || n.contains("alkass") || n.contains("riyadiya") || n.contains("أبوظبي") || n.contains("كأس")
                    if (!isSport) continue
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
