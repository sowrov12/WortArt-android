package com.sowrov.germanarticlebangla.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

const val PREFS_NAME = "wortart_prefs"

/** Cards to answer each day to keep the streak going. */
const val DAILY_GOAL = 10
private const val MAX_TIME_PER_CARD_MS = 30_000L

// ---------------------------------------------------------------------------------------------
// Limits & plans
// ---------------------------------------------------------------------------------------------

enum class DailyCounter { FAVORITES_ADDED, CARDS_PRACTICED, RULES_OPENED, VOICE_PRACTICED }

/** Free-plan daily limits (guests and logged-in free users). Premium = unlimited. */
fun freeLimit(counter: DailyCounter): Int = when (counter) {
    DailyCounter.FAVORITES_ADDED -> 10
    DailyCounter.CARDS_PRACTICED -> 500
    DailyCounter.RULES_OPENED -> 10
    DailyCounter.VOICE_PRACTICED -> 50
}

class LimitCopy(val title: String, val body: String)

fun limitCopy(counter: DailyCounter): LimitCopy = when (counter) {
    DailyCounter.FAVORITES_ADDED -> LimitCopy(
        "Daily favorites limit reached",
        "You've reached today's free limit for favorites.",
    )
    DailyCounter.CARDS_PRACTICED -> LimitCopy(
        "Daily practice limit reached",
        "You've reached today's free practice limit.",
    )
    DailyCounter.RULES_OPENED -> LimitCopy(
        "Daily rule limit reached",
        "You've reached today's free limit for rule look-ups.",
    )
    DailyCounter.VOICE_PRACTICED -> LimitCopy(
        "Daily pronunciation limit reached",
        "You've reached today's free pronunciation limit.",
    )
}

class Plan(val id: String, val label: String, val price: Int, val days: Int, val badge: String? = null)

val PLANS: List<Plan> = listOf(
    Plan("week", "1 Week", 30, 7),
    Plan("month", "1 Month", 100, 30, "Popular"),
    Plan("year", "1 Year", 500, 365, "Best value"),
)

const val WHATSAPP_NUMBER = "8801840648182"

fun whatsappLink(plan: Plan, uid: String, email: String): String {
    val text = "Hello, I want to buy the ${plan.label} plan (${plan.price}৳). My User ID: $uid, Email: $email"
    return "https://wa.me/$WHATSAPP_NUMBER?text=" + java.net.URLEncoder.encode(text, "UTF-8")
}

const val SUPPORT_EMAIL = "sowrovhin@gmail.com"
const val PRIVACY_URL = "https://wortart.vercel.app/privacy-policy.html"
const val DELETE_ACCOUNT_URL = "https://wortart.vercel.app/delete-account.html"

/** Neutral support chat (no plan or price in the text). */
fun supportWhatsappLink(): String =
    "https://wa.me/$WHATSAPP_NUMBER?text=" + java.net.URLEncoder.encode("Hello, I have feedback about the app.", "UTF-8")

fun activationWhatsappLink(email: String?): String {
    val who = if (email.isNullOrEmpty()) "" else " My account email: $email"
    return "https://wa.me/$WHATSAPP_NUMBER?text=" +
        java.net.URLEncoder.encode("Hello, I would like help activating my member access.$who", "UTF-8")
}

const val FREE_CUSTOM_DECKS = 1
const val FREE_CATEGORIES_PER_DECK = 2

// ---------------------------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------------------------

data class Stats(
    val cardsPlayed: Int = 0,
    val correct: Int = 0,
    val currentRun: Int = 0,
    val bestRun: Int = 0,
    val timeSpentMs: Long = 0L,
    val streak: Int = 0,
    val lastPlayedDay: String? = null,
    /** Last day the 10-card daily goal was reached; the streak only grows on this. */
    val streakDay: String? = null,
    /** Every recognized pronunciation attempt / the ones that were perfect. */
    val pronTotal: Int = 0,
    val pronPerfect: Int = 0,
)

data class Settings(
    /** Any combination of A1 / A2 / B1 (never empty). */
    val levels: Set<String> = LEVEL_IDS.toSet(),
    val reminders: Boolean = false,
    val readAloud: Boolean = true,
)

/** Free-plan daily usage. [date] is a local YYYY-MM-DD key; a new day means all counters are 0. */
data class DailyUsage(
    val date: String = "",
    val favoritesAdded: Int = 0,
    val cardsPracticed: Int = 0,
    val rulesOpened: Int = 0,
    val voicePracticed: Int = 0,
) {
    fun valueOf(counter: DailyCounter): Int = when (counter) {
        DailyCounter.FAVORITES_ADDED -> favoritesAdded
        DailyCounter.CARDS_PRACTICED -> cardsPracticed
        DailyCounter.RULES_OPENED -> rulesOpened
        DailyCounter.VOICE_PRACTICED -> voicePracticed
    }
}

/** One calendar week (Monday to Sunday) of practice, kept for the German Score. */
data class WeekStats(
    val cards: Int = 0,
    val correct: Int = 0,
    val timeMs: Long = 0L,
    /** Days of the week on which the 10-card daily goal was reached. */
    val goalDays: Int = 0,
    val bestRun: Int = 0,
    val pronTotal: Int = 0,
    val pronPerfect: Int = 0,
)

data class Progress(
    val stats: Stats = Stats(),
    val favorites: List<String> = emptyList(),
    val settings: Settings = Settings(),
    val daily: DailyUsage = DailyUsage(),
    /** Recent weeks by their Monday date (YYYY-MM-DD). */
    val weeks: Map<String, WeekStats> = emptyMap(),
)

/** Local-midnight date key (YYYY-MM-DD) for the daily limit counters. */
fun isoDayKey(cal: Calendar = Calendar.getInstance()): String =
    String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )

/** Day key used for streaks and reminders (not zero padded). */
fun dayKey(cal: Calendar = Calendar.getInstance()): String =
    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"

private fun yesterdayKey(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, -1)
    return dayKey(cal)
}

/** Today's usage; counters from a previous day read as zero (reset at local midnight). */
fun getTodayUsage(progress: Progress): DailyUsage {
    val today = isoDayKey()
    return if (progress.daily.date == today) progress.daily else DailyUsage(date = today)
}

private fun bump(progress: Progress, counter: DailyCounter): DailyUsage {
    val usage = getTodayUsage(progress)
    return when (counter) {
        DailyCounter.FAVORITES_ADDED -> usage.copy(favoritesAdded = usage.favoritesAdded + 1)
        DailyCounter.CARDS_PRACTICED -> usage.copy(cardsPracticed = usage.cardsPracticed + 1)
        DailyCounter.RULES_OPENED -> usage.copy(rulesOpened = usage.rulesOpened + 1)
        DailyCounter.VOICE_PRACTICED -> usage.copy(voicePracticed = usage.voicePracticed + 1)
    }
}

/** Monday of the current week (shifted back by [weeksAgo] weeks) as YYYY-MM-DD. */
fun weekKey(weeksAgo: Int = 0): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, -7 * weeksAgo)
    // Calendar: Sunday = 1 ... Saturday = 7, so Monday = 0 after this shift.
    val sinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_MONTH, -sinceMonday)
    return isoDayKey(cal)
}

// German Score (0-100), most important first: cards played 40, time practiced 30, success rate 20, daily goal days 10.
private const val SCORE_CARDS_GOAL = 250.0
private const val SCORE_MINUTES_GOAL = 15.0
private const val SCORE_DAYS_GOAL = 7.0

fun germanScore(week: WeekStats): Int {
    val cards = minOf(week.cards / SCORE_CARDS_GOAL, 1.0)
    val time = minOf(week.timeMs / (SCORE_MINUTES_GOAL * 60_000.0), 1.0)
    val success = if (week.cards > 0) week.correct.toDouble() / week.cards else 0.0
    val days = minOf(week.goalDays / SCORE_DAYS_GOAL, 1.0)
    return Math.round(40.0 * cards + 30.0 * time + 20.0 * success + 10.0 * days).toInt().coerceIn(0, 100)
}

private fun updateWeek(weeks: Map<String, WeekStats>, change: (WeekStats) -> WeekStats): Map<String, WeekStats> {
    val key = weekKey()
    val updated = weeks + (key to change(weeks[key] ?: WeekStats()))
    val oldest = weekKey(3)
    return updated.filterKeys { it >= oldest }
}

fun getActiveStreak(stats: Stats): Int {
    val streakDay = stats.streakDay ?: return 0
    return if (streakDay == dayKey() || streakDay == yesterdayKey()) stats.streak else 0
}

private fun readLevels(st: JSONObject): Set<String> {
    val arr = st.optJSONArray("levels")
    if (arr != null) {
        val picked = LinkedHashSet<String>()
        for (i in 0 until arr.length()) {
            val id = arr.optString(i)
            if (LEVEL_IDS.contains(id)) picked.add(id)
        }
        if (picked.isNotEmpty()) return picked
    }
    // Older versions stored a single level (or "all").
    return when (st.optString("level", "all")) {
        "all" -> LEVEL_IDS.toSet()
        "A2" -> setOf("A2")
        "B1" -> setOf("B1")
        else -> setOf("A1")
    }
}

private fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).ifEmpty { null }

class ProgressStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _progress = MutableStateFlow(decode(prefs.getString(KEY, null)))
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private fun write(next: Progress) {
        _progress.value = next
        prefs.edit().putString(KEY, encode(next)).apply()
    }

    fun recordAnswer(isCorrect: Boolean, elapsedMs: Long) {
        val current = _progress.value
        val stats = current.stats
        val today = dayKey()
        // Each answered card counts toward the free daily practice limit and the daily goal.
        val daily = bump(current, DailyCounter.CARDS_PRACTICED)
        // The streak grows only once per day, when the daily goal (10 cards) is reached.
        var streak = stats.streak
        var streakDay = stats.streakDay
        if (daily.cardsPracticed >= DAILY_GOAL && stats.streakDay != today) {
            streak = if (stats.streakDay == yesterdayKey()) stats.streak + 1 else 1
            streakDay = today
        }
        val currentRun = if (isCorrect) stats.currentRun + 1 else 0
        val goalReachedNow = daily.cardsPracticed >= DAILY_GOAL && stats.streakDay != today
        val weeks = updateWeek(current.weeks) { w ->
            w.copy(
                cards = w.cards + 1,
                correct = w.correct + if (isCorrect) 1 else 0,
                timeMs = w.timeMs + elapsedMs.coerceIn(0L, MAX_TIME_PER_CARD_MS),
                bestRun = maxOf(w.bestRun, currentRun),
                goalDays = w.goalDays + if (goalReachedNow) 1 else 0,
            )
        }
        write(
            current.copy(
                stats = stats.copy(
                    cardsPlayed = stats.cardsPlayed + 1,
                    correct = stats.correct + if (isCorrect) 1 else 0,
                    currentRun = currentRun,
                    bestRun = maxOf(stats.bestRun, currentRun),
                    timeSpentMs = stats.timeSpentMs + elapsedMs.coerceIn(0L, MAX_TIME_PER_CARD_MS),
                    streak = streak,
                    lastPlayedDay = today,
                    streakDay = streakDay,
                ),
                daily = daily,
                weeks = weeks,
            )
        )
    }

    fun recordPronunciation(perfect: Boolean) {
        val current = _progress.value
        val s = current.stats
        write(
            current.copy(
                stats = s.copy(
                    pronTotal = s.pronTotal + 1,
                    pronPerfect = s.pronPerfect + if (perfect) 1 else 0,
                ),
                // Every recognized attempt counts toward the free daily pronunciation limit.
                daily = bump(current, DailyCounter.VOICE_PRACTICED),
                weeks = updateWeek(current.weeks) { w ->
                    w.copy(
                        pronTotal = w.pronTotal + 1,
                        pronPerfect = w.pronPerfect + if (perfect) 1 else 0,
                    )
                },
            )
        )
    }

    fun toggleFavorite(wordId: String) {
        val current = _progress.value
        val isRemoving = current.favorites.contains(wordId)
        val favorites = if (isRemoving) {
            current.favorites.filter { it != wordId }
        } else {
            current.favorites + wordId
        }
        // Only newly added favorites count toward the daily limit (removing never refunds).
        write(
            current.copy(
                favorites = favorites,
                daily = if (isRemoving) current.daily else bump(current, DailyCounter.FAVORITES_ADDED),
            )
        )
    }

    fun countRuleOpen() {
        val current = _progress.value
        write(current.copy(daily = bump(current, DailyCounter.RULES_OPENED)))
    }

    fun resetStats() {
        write(_progress.value.copy(stats = Stats(), weeks = emptyMap()))
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val current = _progress.value
        write(current.copy(settings = transform(current.settings)))
    }

    companion object {
        private const val KEY = "progress_v1"

        /** Read-only snapshot, used by the reminder worker. */
        fun readSnapshot(context: Context): Progress {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return decode(prefs.getString(KEY, null))
        }

        private fun encode(p: Progress): String {
            val stats = JSONObject()
            stats.put("cardsPlayed", p.stats.cardsPlayed)
            stats.put("correct", p.stats.correct)
            stats.put("currentRun", p.stats.currentRun)
            stats.put("bestRun", p.stats.bestRun)
            stats.put("timeSpentMs", p.stats.timeSpentMs)
            stats.put("streak", p.stats.streak)
            stats.put("lastPlayedDay", p.stats.lastPlayedDay ?: JSONObject.NULL)
            stats.put("streakDay", p.stats.streakDay ?: JSONObject.NULL)
            stats.put("pronTotal", p.stats.pronTotal)
            stats.put("pronPerfect", p.stats.pronPerfect)

            val settings = JSONObject()
            val levelsArray = JSONArray()
            for (id in LEVEL_IDS) if (p.settings.levels.contains(id)) levelsArray.put(id)
            settings.put("levels", levelsArray)
            settings.put("levelsVersion", 2)
            settings.put("reminders", p.settings.reminders)
            settings.put("readAloud", p.settings.readAloud)

            val daily = JSONObject()
            daily.put("date", p.daily.date)
            daily.put("favoritesAdded", p.daily.favoritesAdded)
            daily.put("cardsPracticed", p.daily.cardsPracticed)
            daily.put("rulesOpened", p.daily.rulesOpened)
            daily.put("voicePracticed", p.daily.voicePracticed)

            val weeks = JSONObject()
            for ((key, w) in p.weeks) {
                val o = JSONObject()
                o.put("cards", w.cards)
                o.put("correct", w.correct)
                o.put("timeMs", w.timeMs)
                o.put("goalDays", w.goalDays)
                o.put("bestRun", w.bestRun)
                o.put("pronTotal", w.pronTotal)
                o.put("pronPerfect", w.pronPerfect)
                weeks.put(key, o)
            }

            val favorites = JSONArray()
            for (id in p.favorites) favorites.put(id)

            val root = JSONObject()
            root.put("stats", stats)
            root.put("favorites", favorites)
            root.put("settings", settings)
            root.put("daily", daily)
            root.put("weeks", weeks)
            return root.toString()
        }

        private fun decode(raw: String?): Progress {
            if (raw.isNullOrEmpty()) return Progress()
            return try {
                val root = JSONObject(raw)
                val s = root.optJSONObject("stats")
                val st = root.optJSONObject("settings")
                val d = root.optJSONObject("daily")
                val fav = root.optJSONArray("favorites")
                val favorites = ArrayList<String>()
                if (fav != null) {
                    for (i in 0 until fav.length()) favorites.add(fav.getString(i))
                }
                val weeks = LinkedHashMap<String, WeekStats>()
                val weeksObj = root.optJSONObject("weeks")
                if (weeksObj != null) {
                    val keys = weeksObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val o = weeksObj.optJSONObject(key) ?: continue
                        weeks[key] = WeekStats(
                            cards = o.optInt("cards", 0),
                            correct = o.optInt("correct", 0),
                            timeMs = o.optLong("timeMs", 0L),
                            goalDays = o.optInt("goalDays", 0),
                            bestRun = o.optInt("bestRun", 0),
                            pronTotal = o.optInt("pronTotal", 0),
                            pronPerfect = o.optInt("pronPerfect", 0),
                        )
                    }
                }
                Progress(
                    stats = if (s == null) Stats() else Stats(
                        cardsPlayed = s.optInt("cardsPlayed", 0),
                        correct = s.optInt("correct", 0),
                        currentRun = s.optInt("currentRun", 0),
                        bestRun = s.optInt("bestRun", 0),
                        timeSpentMs = s.optLong("timeSpentMs", 0L),
                        streak = s.optInt("streak", 0),
                        lastPlayedDay = s.stringOrNull("lastPlayedDay"),
                        streakDay = s.stringOrNull("streakDay"),
                        pronTotal = s.optInt("pronTotal", 0),
                        pronPerfect = s.optInt("pronPerfect", 0),
                    ),
                    favorites = favorites,
                    settings = if (st == null) Settings() else Settings(
                        // Settings saved before "all levels" became the default are moved to it once.
                        levels = if (st.optInt("levelsVersion", 0) >= 2) readLevels(st) else LEVEL_IDS.toSet(),
                        reminders = st.optBoolean("reminders", false),
                        readAloud = st.optBoolean("readAloud", true),
                    ),
                    daily = if (d == null) DailyUsage() else DailyUsage(
                        date = d.optString("date", ""),
                        favoritesAdded = d.optInt("favoritesAdded", 0),
                        cardsPracticed = d.optInt("cardsPracticed", 0),
                        rulesOpened = d.optInt("rulesOpened", 0),
                        voicePracticed = d.optInt("voicePracticed", 0),
                    ),
                    weeks = weeks,
                )
            } catch (e: Exception) {
                Progress()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Custom decks ("multiple category" decks), stored on this device
// ---------------------------------------------------------------------------------------------

data class CustomDeck(val id: String, val name: String, val categoryIds: List<String>)

const val CUSTOM_DECK_PREFIX = "custom:"
private const val DECK_NAME_MAX = 30

fun isCustomDeckId(id: String): Boolean = id.startsWith(CUSTOM_DECK_PREFIX)

/** "multiple category 1", then the first unused number. */
fun nextDeckName(decks: List<CustomDeck>): String {
    val used = decks.map { it.name.lowercase() }.toHashSet()
    var n = 1
    while (used.contains("multiple category $n")) n++
    return "multiple category $n"
}

class CustomDeckStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _decks = MutableStateFlow(load())
    val decks: StateFlow<List<CustomDeck>> = _decks.asStateFlow()

    private fun load(): List<CustomDeck> {
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<CustomDeck>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ids = ArrayList<String>()
                val idsArr = o.optJSONArray("categoryIds")
                if (idsArr != null) {
                    for (j in 0 until idsArr.length()) ids.add(idsArr.getString(j))
                }
                list.add(CustomDeck(o.getString("id"), o.getString("name"), ids))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun write(next: List<CustomDeck>) {
        _decks.value = next
        val arr = JSONArray()
        for (deck in next) {
            val o = JSONObject()
            o.put("id", deck.id)
            o.put("name", deck.name)
            val ids = JSONArray()
            for (id in deck.categoryIds) ids.put(id)
            o.put("categoryIds", ids)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun create(): CustomDeck {
        val current = _decks.value
        val deck = CustomDeck(
            id = CUSTOM_DECK_PREFIX + java.lang.Long.toString(System.currentTimeMillis(), 36),
            name = nextDeckName(current),
            categoryIds = emptyList(),
        )
        write(current + deck)
        return deck
    }

    fun rename(id: String, name: String) {
        val trimmed = name.trim().take(DECK_NAME_MAX)
        val current = _decks.value
        write(
            current.map { deck ->
                if (deck.id == id) {
                    deck.copy(name = trimmed.ifEmpty { nextDeckName(current.filter { it.id != id }) })
                } else {
                    deck
                }
            }
        )
    }

    fun setCategories(id: String, categoryIds: List<String>) {
        write(_decks.value.map { if (it.id == id) it.copy(categoryIds = categoryIds) else it })
    }

    fun remove(id: String) {
        write(_decks.value.filter { it.id != id })
    }

    private companion object {
        const val KEY = "custom_decks_v1"
    }
}

// ---------------------------------------------------------------------------------------------
// Theme preference
// ---------------------------------------------------------------------------------------------

/** Dark is the default; SYSTEM follows the phone's light/dark setting. */
enum class ThemePref(val id: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromId(id: String?): ThemePref = entries.firstOrNull { it.id == id } ?: DARK
    }
}

class ThemeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pref = MutableStateFlow(ThemePref.fromId(prefs.getString(KEY, null)))
    val pref: StateFlow<ThemePref> = _pref.asStateFlow()

    fun set(value: ThemePref) {
        _pref.value = value
        prefs.edit().putString(KEY, value.id).apply()
    }

    private companion object {
        const val KEY = "theme_pref"
    }
}
