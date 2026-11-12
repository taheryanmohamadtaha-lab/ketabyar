package com.ketabyar.app

import androidx.activity.compose.BackHandler
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
                        is Route.Search -> SearchScreen(books, go)
                        is Route.Bookmarks -> BookmarksScreen(books, store, go)
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
    val key = "${b.id}|$sec"
    val marked = key in store.bookmarks
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { go(Route.Detail(b.id)) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(b.title, style = MaterialTheme.typography.bodySmall)
                Text(b.sections[sec].title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { store.toggleBookmark(key) }) {
                Icon(if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "نشانک", tint = Amber)
            }
        }
        key(sec) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Text(b.sections[sec].text, fontSize = store.fontSize.sp, lineHeight = (store.fontSize * store.lineH).sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
fun SearchScreen(books: List<Book>, go: (Route) -> Unit) {
    var q by remember { mutableStateOf("") }
    val res = if (q.isBlank()) emptyList() else books.filter {
        it.title.contains(q, true) || it.titleEn.contains(q, true) || it.author.contains(q, true) || it.category.contains(q, true)
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(value = q, onValueChange = { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("نام کتاب، نویسنده یا موضوع") },
                leadingIcon = { Icon(Icons.Filled.Search, null) })
        }
        if (q.isNotBlank() && res.isEmpty()) item {
            Text("کتابی با این مشخصات در کتابخانه پیدا نشد. جست‌وجوی آنلاین در نسخه‌های بعد اضافه می‌شود.", color = Color.Gray)
        }
        items(res) { b -> BookRow(b) { go(Route.Detail(b.id)) } }
    }
}

@Composable
fun SettingsScreen(store: Store, count: Int) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Title("تنظیمات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("حالت شب"); Switch(checked = store.dark, onCheckedChange = { store.setDark(it) })
        }
        Text("اندازه فونت مطالعه: ${store.fontSize.roundToInt()}")
        Slider(value = store.fontSize, onValueChange = { store.setFont(it) }, valueRange = 14f..30f)
        Text("فاصله خطوط: ${"%.1f".format(store.lineH)}")
        Slider(value = store.lineH, onValueChange = { store.setLine(it) }, valueRange = 1.4f..2.4f)
        Text("نسخه برنامه: 0.1.0 · تعداد کتاب‌های کتابخانه: $count", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}
