package com.ketabyar.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OnlineBook(val title: String, val authors: String, val year: String, val source: String, val link: String)

/** جست‌وجوی اطلاعات کتاب از APIهای رسمی Google Books و Open Library (فقط مشخصات، نه متن کتاب) */
object Online {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "KetabYar/0.2 (Android)")
        try {
            if (c.responseCode != 200) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    fun search(q: String): List<OnlineBook> {
        val enc = URLEncoder.encode(q, "UTF-8")
        val out = mutableListOf<OnlineBook>()
        var failures = 0
        try {
            val items = JSONObject(get("https://www.googleapis.com/books/v1/volumes?q=$enc&maxResults=10")).optJSONArray("items")
            if (items != null) for (i in 0 until items.length()) {
                val v = items.getJSONObject(i).optJSONObject("volumeInfo") ?: continue
                val au = v.optJSONArray("authors")
                val authors = if (au == null) "" else (0 until au.length()).joinToString("، ") { au.getString(it) }
                out.add(OnlineBook(v.optString("title"), authors, v.optString("publishedDate").take(4),
                    "Google Books", v.optString("infoLink")))
            }
        } catch (e: Exception) { failures++ }
        try {
            val docs = JSONObject(get("https://openlibrary.org/search.json?q=$enc&limit=10")).optJSONArray("docs")
            if (docs != null) for (i in 0 until docs.length()) {
                val d = docs.getJSONObject(i)
                val au = d.optJSONArray("author_name")
                val authors = if (au == null) "" else (0 until au.length()).joinToString("، ") { au.getString(it) }
                val year = if (d.has("first_publish_year")) d.optInt("first_publish_year").toString() else ""
                out.add(OnlineBook(d.optString("title"), authors, year, "Open Library", "https://openlibrary.org" + d.optString("key")))
            }
        } catch (e: Exception) { failures++ }
        if (failures == 2) throw java.io.IOException("offline")
        return out.filter { it.title.isNotBlank() }
            .distinctBy { it.title.lowercase().trim() + "|" + it.authors.lowercase().take(12) }
    }
}
