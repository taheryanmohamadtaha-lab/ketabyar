package com.ketabyar.app

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val Amber = Color(0xFFF5B301)
val PanelColor = Color(0xFF2B2B2E)
val Vazir = FontFamily(Font(R.font.vazir))

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f)
)

sealed class Route {
    object Home : Route()
    object Settings : Route()
    object Search : Route()
    object Bookmarks : Route()
    object Saved : Route()
    data class Detail(val id: String) : Route()
    data class Reader(val id: String, val sec: Int) : Route()
    data class Contents(val id: String) : Route()
}

@Composable
fun App(store: Store, books: List<Book>) {
    var route by remember { mutableStateOf<Route>(Route.Home) }
    val scheme = if (store.dark)
        darkColorScheme(primary = Amber, onPrimary = Color.Black, background = Color(0xFF1C1C1E),
            surface = Color(0xFF2B2B2E), onBackground = Color.White, onSurface = Color.White)
    else
        lightColorScheme(primary = Color(0xFFB07C00), background = Color.White, surface = Color(0xFFF1F1F1),
            onBackground = Color(0xFF1C1C1E), onSurface = Color(0xFF1C1C1E))

    MaterialTheme(colorScheme = scheme, typography = Typography().withFont(Vazir)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BackHandler(enabled = route != Route.Home) {
                route = when (val r = route) {
                    is Route.Reader -> Route.Detail(r.id)
                    is Route.Contents -> Route.Reader(r.id, store.lastSec(r.id))
                    else -> Route.Home
                }
            }
            val go: (Route) -> Unit = { route = it }
            val showBar = route is Route.Home || route is Route.Settings || route is Route.Search
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        NavItem("محتوا", Icons.Filled.MenuBook, route is Route.Home) { go(Route.Home) }
                        NavItem("تنظیمات", Icons.Filled.Settings, route is Route.Settings) { go(Route.Settings) }
                        NavItem("جست‌وجو", Icons.Filled.Search, route is Route.Search) { go(Route.Search) }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (val r = route) {
                        is Route.Home -> HomeScreen(books, store, go)
                        is Route.Settings -> SettingsScreen(store, books.size)
                        is Route.Search -> SearchScreen(books, store, go)
                        is Route.Bookmarks -> BookmarksScreen(books, store, go)
                        is Route.Saved -> SavedScreen(books, store, go)
                        is Route.Detail -> books.find { it.id == r.id }?.let { DetailScreen(it, store, go) }
                        is Route.Reader -> books.find { it.id == r.id }?.let { ReaderScreen(it, r.sec, store, go) }
                        is Route.Contents -> books.find { it.id == r.id }?.let { ContentsScreen(it, store, go) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected, onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Amber, selectedTextColor = Amber, indicatorColor = Color.Transparent)
    )
}

@Composable
private fun Cover(b: Book, w: Int, h: Int) {
    Box(
        Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(b.color), Color(b.color).copy(alpha = 0.6f)))),
        contentAlignment = Alignment.Center
    ) {
        Text(b.title, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            fontSize = (w / 7).sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun BookRow(b: Book, subtitle: String = b.author, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(b, 56, 76)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${b.category} · حدود ${b.minutes} دقیقه", style = MaterialTheme.typography.bodySmall, color = Amber)
            }
        }
    }
}

@Composable
private fun Title(t: String) = Text(t, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeScreen(books: List<Book>, store: Store, go: (Route) -> Unit) {
    var cat by remember { mutableStateOf<String?>(null) }
    val daily = books[((System.currentTimeMillis() / 86_400_000L) % books.size).toInt()]
    val last = store.lastBook()?.let { id -> books.find { it.id == id } }
    val cats = books.map { it.category }.distinct()
    val shown = books.filter { cat == null || it.category == cat }
    val savedBooks = books.filter { it.id in store.saved }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("کتاب‌یار زندگی", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("هر روز کمی بیشتر بدان، بهتر زندگی کن.", style = MaterialTheme.typography.bodyMedium, color = Amber)
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().clickable { go(Route.Search) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = Amber)
                    Spacer(Modifier.width(10.dp))
                    Text("جست‌وجوی کتاب یا نویسنده…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().clickable { go(Route.Saved) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Amber)
                    Spacer(Modifier.width(10.dp))
                    Text("بخش‌های مورد علاقه (${store.passages.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Title("کتاب روز") }
        item { BookRow(daily) { go(Route.Detail(daily.id)) } }
        if (last != null) {
            item { Title("ادامه مطالعه") }
            item {
                val s = store.lastSec(last.id).coerceIn(0, last.sections.size - 1)
                BookRow(last, "ادامه از: ${last.sections[s].title}") { go(Route.Reader(last.id, s)) }
            }
        }
        item { Title("دسته‌بندی موضوعی") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("همه", cat == null) { cat = null } }
                items(cats) { c -> Chip(c, cat == c) { cat = if (cat == c) null else c } }
            }
        }
        item { Title(if (cat == null) "همه کتاب‌ها" else cat!!) }
        items(shown) { b -> BookRow(b) { go(Route.Detail(b.id)) } }
        if (savedBooks.isNotEmpty()) {
            item { Title("کتاب‌های ذخیره‌شده") }
            items(savedBooks) { b -> BookRow(b) { go(Route.Detail(b.id)) } }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp),
        color = if (selected) Amber else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clickable(onClick = onClick)) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DetailScreen(b: Book, store: Store, go: (Route) -> Unit) {
    val isSaved = b.id in store.saved
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TopBar("معرفی کتاب") { go(Route.Home) } }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(b, 110, 150)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(b.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(b.titleEn, style = MaterialTheme.typography.bodySmall)
                    Text(b.author, style = MaterialTheme.typography.bodyMedium)
                    Text("${b.category} · حدود ${b.minutes} دقیقه", style = MaterialTheme.typography.bodySmall, color = Amber)
                }
            }
        }
        item { Text(b.intro, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { go(Route.Reader(b.id, store.lastSec(b.id).coerceIn(0, b.sections.size - 1))) },
                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)) { Text("شروع / ادامه مطالعه") }
                OutlinedButton(onClick = { store.toggleSaved(b.id) }) {
                    Icon(if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = Amber)
                }
            }
        }
        item { Title("مهم‌ترین درس‌های کتاب") }
        items(b.lessons.size) { i ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    Text("${i + 1}", color = Amber, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(b.lessons[i], style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Title("برنامه عملی ۷ روزه") }
        items(b.plan) { Text("• $it", style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp) }
        item { Title("جمع‌بندی نهایی") }
        item { Text(b.conclusion, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp) }
        item { Title("منابع و مراجع") }
        items(b.sources) { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun ReaderScreen(b: Book, startSec: Int, store: Store, go: (Route) -> Unit) {
    val n = b.sections.size
    var sec by remember { mutableIntStateOf(startSec.coerceIn(0, n - 1)) }
    LaunchedEffect(sec) { store.setLast(b.id, sec) }
    val bmKey = "${b.id}|$sec"
    val marked = bmKey in store.bookmarks
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go(Route.Detail(b.id)) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(b.title, style = MaterialTheme.typography.bodySmall)
                Text(b.sections[sec].title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { store.toggleBookmark(bmKey) }) {
                Icon(if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "نشانک", tint = Amber)
            }
        }
        key(sec) {
            val paras = remember(sec) { b.sections[sec].text.split("\n\n").filter { it.isNotBlank() } }
            var active by remember { mutableIntStateOf(-1) }
            val ctx = LocalContext.current
            val clip = LocalClipboardManager.current
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                Text("برای ذخیره، کپی یا ارسال، روی هر پاراگراف بزنید.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp))
                paras.forEachIndexed { i, para ->
                    val pk = "${b.id}|$sec|$i"
                    val isSaved = pk in store.passages
                    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (isSaved) Amber.copy(alpha = 0.18f) else if (active == i) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { active = if (active == i) -1 else i }.padding(8.dp)) {
                        Text(para, fontSize = store.fontSize.sp, lineHeight = (store.fontSize * store.lineH).sp)
                        if (active == i) Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { store.togglePassage(pk) }) {
                                Icon(if (isSaved) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = Amber, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text(if (isSaved) "حذف از علاقه‌ها" else "ذخیره", fontSize = 12.sp)
                            }
                            TextButton(onClick = { copyQuote(ctx, clip, quoteOf(b, para)) }) {
                                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text("کپی", fontSize = 12.sp)
                            }
                            TextButton(onClick = { shareQuote(ctx, quoteOf(b, para)) }) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text("ارسال", fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (sec > 0) OutlinedButton(onClick = { sec -= 1 }) { Text("بخش قبل") }
                    if (sec < n - 1) Button(onClick = { sec += 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)) { Text("بخش بعد") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        Surface(color = PanelColor, contentColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(b.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(b.author, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                if (n > 1) Slider(value = sec.toFloat(), onValueChange = { sec = it.roundToInt() },
                    valueRange = 0f..(n - 1).toFloat(), steps = (n - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = Amber, activeTrackColor = Amber))
                else Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PanelButton(Icons.Filled.Favorite, "ذخیره", if (b.id in store.saved) "✓" else "", Modifier.weight(1f)) { store.toggleSaved(b.id) }
                    PanelButton(Icons.Filled.FormatListBulleted, "فهرست", "$n", Modifier.weight(1f)) { go(Route.Contents(b.id)) }
                    PanelButton(Icons.Filled.Bookmark, "نشانک‌ها", "${store.bookmarks.size}", Modifier.weight(1f)) { go(Route.Bookmarks) }
                }
            }
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, badge: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF3A3A3E), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                if (badge.isNotEmpty()) { Spacer(Modifier.width(4.dp)); Text(badge, fontSize = 11.sp, color = Amber) }
            }
            Text(label, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun ContentsScreen(b: Book, store: Store, go: (Route) -> Unit) {
    val rainbow = listOf(Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA))
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(rainbow)))
        Column(Modifier.weight(1f)) {
            TopBar("فهرست") { go(Route.Reader(b.id, store.lastSec(b.id))) }
            LazyColumn {
                items(b.sections.size) { i ->
                    Row(Modifier.fillMaxWidth().clickable { go(Route.Reader(b.id, i)) }.padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(b.sections[i].title, style = MaterialTheme.typography.bodyMedium)
                        Text("${i + 1}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(books: List<Book>, store: Store, go: (Route) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("نشانک‌ها") { go(Route.Home) }
        if (store.bookmarks.isEmpty()) {
            Text("هنوز نشانکی ذخیره نکرده‌اید.", Modifier.padding(20.dp), color = Color.Gray)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(store.bookmarks.toList()) { key ->
                val id = key.substringBefore("|"); val sec = key.substringAfter("|").toIntOrNull() ?: 0
                val b = books.find { it.id == id }
                if (b != null && sec in b.sections.indices) {
                    Column(Modifier.fillMaxWidth().clickable { go(Route.Reader(id, sec)) }) {
                        Text("${b.title} · ${b.sections[sec].title}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(b.sections[sec].text.take(110) + "…", style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { store.toggleBookmark(key) }) { Icon(Icons.Filled.Bookmark, "حذف نشانک", tint = Amber) }
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(books: List<Book>, store: Store, go: (Route) -> Unit) {
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<OnlineBook>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trigger by remember { mutableIntStateOf(0) }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            loading = true; error = null; online = null
            try { online = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Online.search(q.trim()) } }
            catch (e: Exception) { error = "اتصال به اینترنت برقرار نشد یا سرویس پاسخ نداد. دوباره تلاش کنید." }
            loading = false
        }
    }
    val res = if (q.isBlank()) emptyList() else books.filter {
        it.title.contains(q, true) || it.titleEn.contains(q, true) || it.author.contains(q, true) || it.category.contains(q, true)
    }
    fun localMatch(o: OnlineBook) = books.find {
        val a = it.titleEn.lowercase().trim(); val b = o.title.lowercase().trim()
        a.isNotEmpty() && b.isNotEmpty() && (a == b || a.startsWith(b) || b.startsWith(a))
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(value = q, onValueChange = { q = it; online = null; error = null }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("نام فارسی/انگلیسی کتاب، نویسنده یا موضوع") },
                leadingIcon = { Icon(Icons.Filled.Search, null) })
        }
        if (res.isNotEmpty()) item { Title("در کتابخانه") }
        items(res) { b -> BookRow(b) { go(Route.Detail(b.id)) } }
        if (q.isNotBlank()) item {
            Button(onClick = { trigger++ }, enabled = !loading, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)) {
                Text(if (loading) "در حال جست‌وجو…" else "جست‌وجوی آنلاین کتاب")
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Amber) }
        error?.let { e -> item { Text(e, color = Color(0xFFE57373)) } }
        online?.let { list ->
            if (list.isEmpty()) item { Text("در منابع آنلاین کتابی با این مشخصات پیدا نشد. عبارت دیگری را امتحان کنید.", color = Color.Gray) }
            else {
                item { Title("نتایج آنلاین") }
                item { Text("این نتایج فقط مشخصات کتاب هستند و خلاصه معتبر محسوب نمی‌شوند.", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                items(list) { o ->
                    val local = localMatch(o)
                    val key = o.title + " — " + o.authors
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(o.title, fontWeight = FontWeight.Bold)
                            Text(listOf(o.authors, o.year).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            Text("منبع: ${o.source}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (local != null) Button(onClick = { go(Route.Detail(local.id)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Color.Black)) { Text("خلاصه موجود است") }
                                else OutlinedButton(onClick = { store.toggleRequest(key) }) {
                                    Text(if (key in store.requests) "✓ در فهرست درخواست‌ها" else "درخواست افزودن خلاصه")
                                }
                                if (o.link.isNotBlank()) TextButton(onClick = { uri.openUri(o.link) }) { Text("مشاهده منبع") }
                            }
                        }
                    }
                }
            }
        }
        if (store.requests.isNotEmpty()) {
            item { Title("درخواست‌های من") }
            items(store.requests.toList()) { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(r, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { store.toggleRequest(r) }) { Icon(Icons.Filled.Bookmark, "حذف", tint = Amber) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(store: Store, count: Int) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("تنظیمات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("حالت شب"); Switch(checked = store.dark, onCheckedChange = { store.updateDark(it) })
        }
        Text("اندازه فونت مطالعه: ${store.fontSize.roundToInt()}")
        Slider(value = store.fontSize, onValueChange = { store.updateFont(it) }, valueRange = 14f..30f)
        Text("فاصله خطوط: ${"%.1f".format(store.lineH)}")
        Slider(value = store.lineH, onValueChange = { store.updateLine(it) }, valueRange = 1.4f..2.4f)
        Text("نسخه برنامه: 0.3.0 · تعداد کتاب‌های کتابخانه: $count", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}


fun quoteOf(b: Book, text: String) = "«$text»\n— ${b.title}، ${b.author}\n(کتاب‌یار زندگی)"

fun copyQuote(ctx: android.content.Context, clip: androidx.compose.ui.platform.ClipboardManager, text: String) {
    clip.setText(AnnotatedString(text))
    Toast.makeText(ctx, "کپی شد", Toast.LENGTH_SHORT).show()
}

fun shareQuote(ctx: android.content.Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    ctx.startActivity(Intent.createChooser(i, "ارسال با"))
}

@Composable
fun SavedScreen(books: List<Book>, store: Store, go: (Route) -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxSize()) {
        TopBar("بخش‌های مورد علاقه") { go(Route.Home) }
        if (store.passages.isEmpty()) Text("هنوز بخشی ذخیره نکرده‌اید. هنگام مطالعه روی یک پاراگراف بزنید و «ذخیره» را انتخاب کنید.",
            Modifier.padding(20.dp), color = Color.Gray)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(store.passages.toList()) { key ->
                val parts = key.split("|")
                val b = books.find { it.id == parts.getOrNull(0) }
                val sec = parts.getOrNull(1)?.toIntOrNull() ?: -1
                val idx = parts.getOrNull(2)?.toIntOrNull() ?: -1
                val para = b?.sections?.getOrNull(sec)?.text?.split("\n\n")?.filter { it.isNotBlank() }?.getOrNull(idx)
                if (b != null && para != null) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${b.title} · ${b.sections[sec].title}", style = MaterialTheme.typography.bodySmall, color = Amber)
                            Spacer(Modifier.height(6.dp))
                            Text(para, style = MaterialTheme.typography.bodyMedium, lineHeight = 26.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { copyQuote(ctx, clip, quoteOf(b, para)) }) { Text("کپی") }
                                TextButton(onClick = { shareQuote(ctx, quoteOf(b, para)) }) { Text("ارسال") }
                                TextButton(onClick = { go(Route.Reader(b.id, sec)) }) { Text("باز کردن در کتاب") }
                                TextButton(onClick = { store.togglePassage(key) }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }
}
