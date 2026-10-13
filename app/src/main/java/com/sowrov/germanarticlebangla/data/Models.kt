package com.sowrov.germanarticlebangla.data

import java.text.Normalizer

enum class Article { der, die, das }

val ARTICLES: List<Article> = listOf(Article.der, Article.die, Article.das)

enum class LevelFilter(val id: String, val short: String, val label: String) {
    ALL("all", "All", "All Levels"),
    A1("A1", "A1", "Beginner"),
    A2("A2", "A2", "Elementary"),
    B1("B1", "B1", "Intermediate");

    companion object {
        fun fromId(id: String?): LevelFilter = entries.firstOrNull { it.id == id } ?: A1
    }
}

data class Word(
    val id: String,
    val word: String,
    /** Every accepted article: dual-gender nouns like "die/der Verwandte" have two. */
    val articles: List<Article>,
    /** Display form, e.g. "die" or "die/der". */
    val articleLabel: String,
    val plural: Boolean,
    val en: String,
    val bn: String,
    val category: String,
    val categoryLabel: String,
    val level: String,
)

val CATEGORY_LABELS: Map<String, String> = linkedMapOf(
    "food" to "Food & Drinks",
    "family" to "Family & People",
    "home" to "Home & Housing",
    "daily" to "Daily Life",
    "education" to "Education",
    "work" to "Work & Career",
    "business" to "Business & Trade",
    "travel" to "Travel & Transport",
    "health" to "Health & Body",
    "shopping" to "Shopping & Money",
    "time" to "Time & Calendar",
    "nature" to "Nature & Weather",
    "government" to "Government & Paperwork",
    "technology" to "Technology",
    "communication" to "Communication",
    "society" to "Society & Culture",
)

val LEGACY_BN: Map<String, String> = mapOf(
    "food" to "খাবার ও পানীয়",
    "family" to "পরিবার ও মানুষ",
    "home" to "বাড়ি ও বাসস্থান",
    "daily" to "দৈনন্দিন জীবন",
    "education" to "শিক্ষা",
    "work" to "কাজ ও ক্যারিয়ার",
    "business" to "ব্যবসা ও বাণিজ্য",
    "travel" to "ভ্রমণ ও যাতায়াত",
    "health" to "স্বাস্থ্য ও শরীর",
    "shopping" to "কেনাকাটা ও টাকা",
    "time" to "সময় ও ক্যালেন্ডার",
    "nature" to "প্রকৃতি ও আবহাওয়া",
    "government" to "সরকারি কাজ ও কাগজপত্র",
    "technology" to "প্রযুক্তি",
    "communication" to "যোগাযোগ",
    "society" to "সমাজ ও সংস্কৃতি",
)

private val CATEGORY_BY_LABEL: Map<String, String> =
    CATEGORY_LABELS.entries.associate { it.value.lowercase() to it.key }

fun isLegacyCategoryId(id: String): Boolean = CATEGORY_LABELS.containsKey(id)

private val SLUG_NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")
private val SLUG_EDGE = Regex("^-|-$")

fun slug(value: String): String {
    val normalized = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFKD)
    return normalized.replace(SLUG_NON_ALNUM, "-").replace(SLUG_EDGE, "")
}

/** Case-insensitive label to category id. Legacy labels map to their old id, anything else to a slug. */
fun categoryIdForLabel(label: String): String {
    val key = label.trim().lowercase()
    if (key.isEmpty()) return "daily"
    return CATEGORY_BY_LABEL[key] ?: slug(label)
}

fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (inQuotes) {
            if (ch == '"' && i + 1 < text.length && text[i + 1] == '"') {
                field.append('"')
                i++
            } else if (ch == '"') {
                inQuotes = false
            } else {
                field.append(ch)
            }
        } else if (ch == '"') {
            inQuotes = true
        } else if (ch == ',') {
            row.add(field.toString())
            field.setLength(0)
        } else if (ch == '\n' || ch == '\r') {
            if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
            row.add(field.toString())
            rows.add(row)
            row = mutableListOf()
            field.setLength(0)
        } else {
            field.append(ch)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    return rows
}

private val ARTICLE_REGEX = Regex("\\b(der|die|das)\\b")
private val PLURAL_REGEX = Regex("\\bpl(\\.|ural)?\\b", RegexOption.IGNORE_CASE)

private fun parseArticles(raw: String): List<Article> =
    ARTICLE_REGEX.findAll(raw.lowercase()).map { Article.valueOf(it.value) }.distinct().toList()

private fun parseLevel(raw: String): String {
    val level = raw.trim().uppercase()
    return if (level == "A2" || level == "B1") level else "A1"
}

/** Columns: Level, German, Article, Bengali, English, Category. */
fun parseWordsCsv(text: String): List<Word> {
    val rows = parseCsv(text.removePrefix("\uFEFF"))
    val seen = HashSet<String>()
    val words = ArrayList<Word>()
    for ((index, cells) in rows.withIndex()) {
        if (index == 0 && cells.getOrNull(1)?.trim()?.lowercase() == "german") continue
        val levelRaw = cells.getOrElse(0) { "" }
        val wordRaw = cells.getOrElse(1) { "" }
        val articleRaw = cells.getOrElse(2) { "" }
        val bn = cells.getOrElse(3) { "" }
        val en = cells.getOrElse(4) { "" }
        val categoryRaw = cells.getOrElse(5) { "" }

        val word = wordRaw.trim()
        val articles = parseArticles(articleRaw)
        if (word.isEmpty() || articles.isEmpty()) continue

        val category = categoryIdForLabel(categoryRaw)
        val id = "${slug(word)}-$category"
        if (!seen.add(id)) continue

        words.add(
            Word(
                id = id,
                word = word,
                articles = articles,
                articleLabel = articles.joinToString("/") { it.name },
                plural = PLURAL_REGEX.containsMatchIn(articleRaw),
                en = en.trim(),
                bn = bn.trim(),
                category = category,
                categoryLabel = categoryRaw.trim(),
                level = parseLevel(levelRaw),
            )
        )
    }
    return words
}

fun isCorrectArticle(word: Word, picked: Article): Boolean = word.articles.contains(picked)

const val SHEET_CSV_URL =
    "https://docs.google.com/spreadsheets/d/19TzPdY2_33vw5jXD4vo4IGckNnFLVpxI/export?format=csv"

// ---------------------------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------------------------

data class CategoryInfo(
    val id: String,
    val label: String,
    val bn: String? = null,
    val bgImage: String? = null,
    val iconImage: String? = null,
    val premium: Boolean = false,
)

private fun makeInfo(
    id: String,
    label: String,
    bgImage: String? = null,
    iconImage: String? = null,
    premium: Boolean = false,
): CategoryInfo {
    val legacy = isLegacyCategoryId(id)
    return CategoryInfo(
        id = id,
        label = if (legacy) CATEGORY_LABELS.getValue(id) else label,
        bn = if (legacy) LEGACY_BN[id] else null,
        bgImage = bgImage,
        iconImage = iconImage,
        premium = premium,
    )
}

/** Used when the sheet has no side table or cannot be fetched: the original 16, all Free. */
val DEFAULT_CATEGORIES: List<CategoryInfo> =
    CATEGORY_LABELS.keys.map { makeInfo(it, CATEGORY_LABELS.getValue(it)) }

/**
 * Side table in the Words tab: G=Category, H=Background Image, I=Icon Image, J=Access.
 * Returns null when the header in G1 is missing.
 */
fun parseCategoryTable(csv: String): List<CategoryInfo>? {
    val rows = parseCsv(csv.removePrefix("\uFEFF"))
    if (rows.firstOrNull()?.getOrNull(6)?.trim()?.lowercase() != "category") return null

    val byId = LinkedHashMap<String, CategoryInfo>()
    for (cells in rows.drop(1)) {
        val label = cells.getOrNull(6)?.trim().orEmpty()
        if (label.isEmpty()) continue
        val id = categoryIdForLabel(label)
        val info = makeInfo(
            id = id,
            label = label,
            bgImage = cells.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() },
            iconImage = cells.getOrNull(8)?.trim()?.takeIf { it.isNotEmpty() },
            premium = cells.getOrNull(9)?.trim()?.lowercase() == "premium",
        )
        if (!byId.containsKey(id)) byId[id] = info
    }
    return if (byId.isNotEmpty()) byId.values.toList() else null
}

/** Sheet order first, then any word category that is missing from column G (Free, no images). */
fun buildCategoryList(words: List<Word>, table: List<CategoryInfo>?): List<CategoryInfo> {
    val list = ArrayList<CategoryInfo>(table ?: DEFAULT_CATEGORIES)
    val known = list.map { it.id }.toHashSet()
    for (word in words) {
        if (!known.add(word.category)) continue
        list.add(makeInfo(word.category, word.categoryLabel.ifEmpty { word.category }))
    }
    return list
}

/** A deck shown in the app: "All words", "Favorites", or one real category. */
data class Category(
    val id: String,
    val label: String,
    val bn: String?,
    val bgImage: String?,
    val iconImage: String?,
    val premium: Boolean,
    /** true for real categories (not the "All words" / "Favorites" decks). */
    val isTopic: Boolean,
)

val ALL_DECK = Category("all", "All words", "সব শব্দ", null, null, false, false)
val FAVORITES_DECK = Category("favorites", "Favorites", "প্রিয়", null, null, false, false)

fun CategoryInfo.toCategory() = Category(id, label, bn, bgImage, iconImage, premium, true)

class Catalog(val words: List<Word>, categories: List<CategoryInfo>, val version: Int) {
    /** Real categories in sheet order. */
    val topics: List<Category> = categories.map { it.toCategory() }

    /** "All words", "Favorites", then every topic. */
    val decks: List<Category> = listOf(ALL_DECK, FAVORITES_DECK) + topics
    private val byId: Map<String, Category> = decks.associateBy { it.id }

    fun getCategory(id: String): Category = byId[id] ?: ALL_DECK
}

fun filterByLevel(words: List<Word>, level: LevelFilter): List<Word> =
    if (level == LevelFilter.ALL) words else words.filter { it.level == level.id }

/**
 * Words in [lockedIds] (Premium categories for a free user) are excluded from every deck,
 * including Favorites.
 */
fun wordsForDeck(
    words: List<Word>,
    deck: String,
    favorites: List<String>,
    level: LevelFilter,
    lockedIds: Set<String>,
    customDecks: List<CustomDeck>,
): List<Word> {
    if (deck == "favorites") {
        return words.filter { favorites.contains(it.id) && !lockedIds.contains(it.category) }
    }
    if (lockedIds.contains(deck)) return emptyList()
    val atLevel = filterByLevel(words, level)
    if (deck == "all") return atLevel.filter { !lockedIds.contains(it.category) }
    val custom = customDecks.firstOrNull { it.id == deck }
    if (custom != null) {
        val ids = custom.categoryIds.filter { !lockedIds.contains(it) }.toHashSet()
        return atLevel.filter { ids.contains(it.category) }
    }
    return atLevel.filter { it.category == deck }
}

// ---------------------------------------------------------------------------------------------
// Images (Google Drive links in the sheet)
// ---------------------------------------------------------------------------------------------

private val DRIVE_PATTERNS = listOf(
    Regex("""(?:drive|docs)\.google\.com/(?:file/)?d/([\w-]+)"""),
    Regex("""(?:drive|docs)\.google\.com/(?:open|uc)\?(?:[^#]*&)?id=([\w-]+)"""),
    Regex("""lh3\.googleusercontent\.com/d/([\w-]+)"""),
)

private fun driveFileId(url: String?): String? {
    val trimmed = url?.trim()
    if (trimmed.isNullOrEmpty()) return null
    for (pattern in DRIVE_PATTERNS) {
        val id = pattern.find(trimmed)?.groupValues?.getOrNull(1)
        if (!id.isNullOrEmpty()) return id
    }
    return null
}

/** Turns a Google Drive share link into a direct image URL (the file must be shared "Anyone with the link"). */
fun toDirectImageUrl(url: String?): String? {
    val trimmed = url?.trim()
    if (trimmed.isNullOrEmpty()) return null
    val id = driveFileId(trimmed)
    return if (id != null) "https://lh3.googleusercontent.com/d/$id" else trimmed
}

/** Small resized version of a Drive image: loads much faster than the full-size file. */
fun toThumbnailUrl(url: String?, width: Int): String? {
    val id = driveFileId(url) ?: return null
    return "https://drive.google.com/thumbnail?id=$id&sz=w$width"
}

private fun rawCategoryImage(category: Category, background: Boolean): String? =
    if (!category.isTopic) null else if (background) category.bgImage else category.iconImage

/** Fast, resized image first (backgrounds 800px, icons 256px). */
fun categoryImageUrl(category: Category, background: Boolean): String? {
    val raw = rawCategoryImage(category, background)
    return toThumbnailUrl(raw, if (background) 800 else 256) ?: toDirectImageUrl(raw)
}

/** Full-size link, tried only if the resized one fails. */
fun categoryImageBackupUrl(category: Category, background: Boolean): String? =
    toDirectImageUrl(rawCategoryImage(category, background))
