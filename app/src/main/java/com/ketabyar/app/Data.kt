package com.ketabyar.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class Section(val title: String, val text: String)

data class Book(
    val id: String, val title: String, val titleEn: String, val author: String,
    val category: String, val minutes: Int, val color: Long, val intro: String,
    val sections: List<Section>, val lessons: List<String>, val plan: List<String>,
    val conclusion: String, val sources: List<String>
)

object Repo {
    fun load(ctx: Context): List<Book> {
        val text = ctx.assets.open("content/catalog.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("books")
        return (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }

    private fun strs(a: JSONArray): List<String> = (0 until a.length()).map { a.getString(it) }

    private fun parse(o: JSONObject): Book {
        val secs = o.getJSONArray("sections")
        return Book(
            id = o.getString("id"), title = o.getString("title"), titleEn = o.getString("titleEn"),
            author = o.getString("author"), category = o.getString("category"),
            minutes = o.getInt("minutes"),
            color = android.graphics.Color.parseColor(o.getString("color")).toLong() and 0xFFFFFFFFL,
            intro = o.getString("intro"),
            sections = (0 until secs.length()).map {
                val s = secs.getJSONObject(it); Section(s.getString("title"), s.getString("text"))
            },
            lessons = strs(o.getJSONArray("lessons")), plan = strs(o.getJSONArray("plan")),
            conclusion = o.getString("conclusion"), sources = strs(o.getJSONArray("sources"))
        )
    }
}

/** ذخیره‌سازی تنظیمات، نشانک‌ها و پیشرفت مطالعه (ماندگار پس از بستن برنامه) */
class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("ketabyar", Context.MODE_PRIVATE)
    var dark by mutableStateOf(p.getBoolean("dark", true)); private set
    var fontSize by mutableStateOf(p.getFloat("font", 18f)); private set
    var lineH by mutableStateOf(p.getFloat("line", 1.8f)); private set
    val bookmarks = mutableStateListOf<String>().apply { addAll(p.getStringSet("bm", emptySet())!!) }
    val saved = mutableStateListOf<String>().apply { addAll(p.getStringSet("saved", emptySet())!!) }

    fun setDark(v: Boolean) { dark = v; p.edit().putBoolean("dark", v).apply() }
    fun setFont(v: Float) { fontSize = v; p.edit().putFloat("font", v).apply() }
    fun setLine(v: Float) { lineH = v; p.edit().putFloat("line", v).apply() }

    fun toggleBookmark(key: String) {
        if (key in bookmarks) bookmarks.remove(key) else bookmarks.add(key)
        p.edit().putStringSet("bm", bookmarks.toSet()).apply()
    }
    fun toggleSaved(id: String) {
        if (id in saved) saved.remove(id) else saved.add(id)
        p.edit().putStringSet("saved", saved.toSet()).apply()
    }
    fun setLast(id: String, sec: Int) { p.edit().putString("lastBook", id).putInt("lastSec_$id", sec).apply() }
    fun lastBook(): String? = p.getString("lastBook", null)
    fun lastSec(id: String): Int = p.getInt("lastSec_$id", 0)
}
