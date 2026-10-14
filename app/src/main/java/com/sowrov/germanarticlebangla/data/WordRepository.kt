package com.sowrov.germanarticlebangla.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Words (columns A-F) and categories (side table G-J) come from one Google Sheet CSV.
 * Order: the last downloaded copy (or the bundled snapshot) is used immediately,
 * then the live sheet is fetched in the background.
 */
class WordRepository(context: Context) {
    private val appContext = context.applicationContext
    private val cacheFile = File(appContext.filesDir, "words_cache.csv")
    private var version = 0

    private fun buildCatalog(csv: String): Catalog? {
        val words = parseWordsCsv(csv)
        if (words.isEmpty()) return null
        version += 1
        return Catalog(words, buildCategoryList(words, parseCategoryTable(csv)), version)
    }

    /** true when a live copy of the sheet was downloaded on an earlier launch. */
    fun hasCache(): Boolean = cacheFile.exists()

    /** Fast, no network. Never returns null (the bundled snapshot is always valid). */
    fun loadLocal(): Catalog {
        if (cacheFile.exists()) {
            try {
                val cached = buildCatalog(cacheFile.readText(Charsets.UTF_8))
                if (cached != null) return cached
            } catch (e: Exception) {
                // fall through to the bundled snapshot
            }
        }
        val csv = appContext.assets.open("words_fallback.csv").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return buildCatalog(csv) ?: Catalog(emptyList(), DEFAULT_CATEGORIES, ++version)
    }

    /** Blocking network call: call from a background dispatcher. Returns null on any failure. */
    fun fetchRemote(): Catalog? {
        return try {
            val csv = download(SHEET_CSV_URL)
            val catalog = buildCatalog(csv)
            if (catalog != null) cacheFile.writeText(csv, Charsets.UTF_8)
            catalog
        } catch (e: Exception) {
            null
        }
    }

    private fun download(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) WortArt")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Sheet responded with $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
