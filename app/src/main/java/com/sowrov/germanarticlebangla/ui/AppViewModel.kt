package com.sowrov.germanarticlebangla.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import coil.imageLoader
import coil.request.ImageRequest
import com.sowrov.germanarticlebangla.data.categoryImageUrl
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sowrov.germanarticlebangla.BuildConfig
import com.sowrov.germanarticlebangla.auth.AuthManager
import com.sowrov.germanarticlebangla.auth.AuthState
import com.sowrov.germanarticlebangla.data.Catalog
import com.sowrov.germanarticlebangla.data.CustomDeck
import com.sowrov.germanarticlebangla.data.CustomDeckStore
import com.sowrov.germanarticlebangla.data.DailyCounter
import com.sowrov.germanarticlebangla.data.LevelFilter
import com.sowrov.germanarticlebangla.data.Progress
import com.sowrov.germanarticlebangla.data.ProgressStore
import com.sowrov.germanarticlebangla.data.RuleTarget
import com.sowrov.germanarticlebangla.data.Settings
import com.sowrov.germanarticlebangla.data.ThemePref
import com.sowrov.germanarticlebangla.data.ThemeStore
import com.sowrov.germanarticlebangla.data.WordRepository
import com.sowrov.germanarticlebangla.data.freeLimit
import com.sowrov.germanarticlebangla.data.getTodayUsage
import com.sowrov.germanarticlebangla.data.isCustomDeckId
import com.sowrov.germanarticlebangla.data.limitCopy
import com.sowrov.germanarticlebangla.ui.practice.PracticeSession
import com.sowrov.germanarticlebangla.data.Word
import com.sowrov.germanarticlebangla.util.Reminders
import com.sowrov.germanarticlebangla.util.Speaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab { Practice, Explore, Stats, Premium }

enum class ExploreSection { Categories, Rules, Search }

/** How the search text is matched: anywhere, or only at the end / start of the German word. */
enum class SearchMode { Contains, EndsWith, StartsWith }

class SearchPreset(val text: String, val mode: SearchMode)

/** Premium status plus the categories a free user cannot open. */
data class Access(val isPremium: Boolean, val lockedIds: Set<String>)

class LimitNotice(val title: String, val body: String, val resets: Boolean)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext

    private val progressStore = ProgressStore(ctx)
    private val deckStore = CustomDeckStore(ctx)
    private val themeStore = ThemeStore(ctx)
    private val repository = WordRepository(ctx)
    private val auth = AuthManager(ctx, viewModelScope)
    private val speaker = Speaker(ctx)

    val progress: StateFlow<Progress> = progressStore.progress
    val customDecks: StateFlow<List<CustomDeck>> = deckStore.decks
    val themePref: StateFlow<ThemePref> = themeStore.pref
    val authState: StateFlow<AuthState> = auth.state

    private val _catalog = MutableStateFlow(repository.loadLocal())
    val catalog: StateFlow<Catalog> = _catalog.asStateFlow()

    val access: StateFlow<Access> = combine(_catalog, auth.state) { cat, authSt ->
        Access(
            isPremium = authSt.isPremium,
            lockedIds = if (authSt.isPremium) {
                emptySet()
            } else {
                cat.topics.filter { it.premium }.map { it.id }.toSet()
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Access(false, _catalog.value.topics.filter { it.premium }.map { it.id }.toSet()),
    )

    /**
     * First launch has only the bundled word list. Practice waits (at most a few seconds) for the live list,
     * so the first card is not swapped a moment later.
     */
    var catalogReady by mutableStateOf(true)
        private set
    /** Set when a grammar-rule ending is tapped: Search opens with that ending filled in. */
    var searchPreset by mutableStateOf<SearchPreset?>(null)
        private set

    /** Set when the menu sends the user to the "German cases" table in Rules. */
    var casesTarget by mutableStateOf(false)
        private set
    var tab by mutableStateOf(Tab.Practice)
        private set
    var selectedDeck by mutableStateOf("all")
        private set
    var exploreSection by mutableStateOf(ExploreSection.Categories)
        private set
    var ruleTarget by mutableStateOf<RuleTarget?>(null)
        private set
    var limitNotice by mutableStateOf<LimitNotice?>(null)
        private set
    var showAdmin by mutableStateOf(false)
        private set
    var signInPending by mutableStateOf(false)
        private set
    var signInError by mutableStateOf<String?>(null)
        private set
    var deletePending by mutableStateOf(false)
        private set
    var deleteMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Refresh the words from the live Google Sheet in the background.
        viewModelScope.launch {
            val remote = withContext(Dispatchers.IO) { repository.fetchRemote() }
            if (remote != null) {
                _catalog.value = remote
                prefetchImages(remote)
            }
            catalogReady = true
        }
        viewModelScope.launch {
            delay(4000)
            catalogReady = true
        }
        prefetchImages(_catalog.value)
        if (progressStore.progress.value.settings.reminders) Reminders.schedule(ctx)
    }

    private var prefetchJob: Job? = null

    /**
     * Downloads the other categories' pictures quietly, after the first card has had the network to itself:
     * small icons first, then backgrounds, one at a time.
     */
    private fun prefetchImages(catalog: Catalog) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            delay(8000)
            val urls = catalog.topics.mapNotNull { categoryImageUrl(it, false) } +
                catalog.topics.mapNotNull { categoryImageUrl(it, true) }
            for (url in urls) {
                ctx.imageLoader.enqueue(ImageRequest.Builder(ctx).data(url).build())
                delay(150)
            }
        }
    }

    // ----- navigation -----------------------------------------------------------------------

    fun selectTab(value: Tab) {
        tab = value
    }

    fun openPremiumTab() {
        tab = Tab.Premium
    }

    fun openSearchPreset(text: String, mode: SearchMode) {
        ruleTarget = null
        casesTarget = false
        searchPreset = SearchPreset(text, mode)
        exploreSection = ExploreSection.Search
        tab = Tab.Explore
    }

    fun consumeSearchPreset() {
        searchPreset = null
    }

    fun openCases() {
        ruleTarget = null
        exploreSection = ExploreSection.Rules
        casesTarget = true
        tab = Tab.Explore
    }

    fun consumeCasesTarget() {
        casesTarget = false
    }

    fun openExplore(section: ExploreSection) {
        casesTarget = false
        ruleTarget = null
        exploreSection = section
        tab = Tab.Explore
    }

    fun changeExploreSection(section: ExploreSection) {
        ruleTarget = null
        exploreSection = section
    }

    fun openRule(target: RuleTarget) {
        ruleTarget = target
        exploreSection = ExploreSection.Rules
        tab = Tab.Explore
    }

    fun openStats() {
        tab = Tab.Stats
    }

    fun openAdmin() {
        showAdmin = true
    }

    fun closeAdmin() {
        showAdmin = false
    }

    /** Tapping a locked category sends free users to the Premium screen. */
    fun selectDeck(id: String) {
        if (access.value.lockedIds.contains(id)) {
            tab = Tab.Premium
            return
        }
        selectedDeck = id
        tab = Tab.Practice
    }

    /** If the selected custom deck was deleted, fall back to "All words". */
    fun effectiveDeck(): String {
        val id = selectedDeck
        return if (isCustomDeckId(id) && customDecks.value.none { it.id == id }) "all" else id
    }

    // ----- limits ---------------------------------------------------------------------------

    private fun remaining(counter: DailyCounter): Int {
        if (access.value.isPremium) return Int.MAX_VALUE
        val used = getTodayUsage(progressStore.progress.value).valueOf(counter)
        return maxOf(0, freeLimit(counter) - used)
    }

    /** Returns true if the action is allowed; otherwise shows the limit dialog. */
    fun tryUse(counter: DailyCounter): Boolean {
        if (remaining(counter) > 0) return true
        val copy = limitCopy(counter)
        limitNotice = LimitNotice(copy.title, copy.body, resets = true)
        return false
    }

    /** Premium dialog with custom copy (for Premium-only features that are not daily limits). */
    fun askPremium(title: String, body: String) {
        // The Google Play version never says "Premium".
        val t = if (BuildConfig.PLAY_BUILD) title.replace("Premium", "membership") else title
        val b = if (BuildConfig.PLAY_BUILD) body.replace("Premium", "membership") else body
        limitNotice = LimitNotice(t, b, resets = false)
    }

    fun dismissNotice() {
        limitNotice = null
    }

    // ----- progress -------------------------------------------------------------------------

    fun recordAnswer(isCorrect: Boolean, elapsedMs: Long) = progressStore.recordAnswer(isCorrect, elapsedMs)

    fun toggleFavorite(id: String) = progressStore.toggleFavorite(id)

    fun recordPronunciation(perfect: Boolean) = progressStore.recordPronunciation(perfect)

    fun stopSpeaking() = speaker.stop()

    /** Favorites count toward the same free daily limit everywhere. */
    fun toggleFavoriteLimited(id: String) {
        val adding = !progressStore.progress.value.favorites.contains(id)
        if (adding && !tryUse(DailyCounter.FAVORITES_ADDED)) return
        progressStore.toggleFavorite(id)
    }

    fun countRuleOpen() = progressStore.countRuleOpen()

    fun resetStats() = progressStore.resetStats()

    /** Levels can be combined; at least one always stays selected. */
    fun toggleLevel(id: String) = progressStore.updateSettings { s: Settings ->
        val current = s.levels
        val next = if (current.contains(id)) {
            if (current.size > 1) current - id else current
        } else {
            current + id
        }
        s.copy(levels = next)
    }

    fun setReadAloud(on: Boolean, currentWord: String?) {
        progressStore.updateSettings { it.copy(readAloud = on) }
        if (on && currentWord != null) speaker.speak(currentWord)
    }

    fun setReminders(on: Boolean) {
        progressStore.updateSettings { s: Settings -> s.copy(reminders = on) }
        if (on) Reminders.schedule(ctx) else Reminders.cancel(ctx)
    }

    fun setTheme(value: ThemePref) = themeStore.set(value)

    // ----- custom decks ---------------------------------------------------------------------

    fun createDeck(): CustomDeck = deckStore.create()

    fun renameDeck(id: String, name: String) = deckStore.rename(id, name)

    fun setDeckCategories(id: String, ids: List<String>) = deckStore.setCategories(id, ids)

    fun removeDeck(id: String) = deckStore.remove(id)

    // ----- sound ----------------------------------------------------------------------------

    fun speak(text: String) = speaker.speak(text)

    // ----- account --------------------------------------------------------------------------

    fun signIn(activity: Activity) {
        if (signInPending) return
        if (!auth.state.value.configured) {
            signInError = "Sign-in is not available right now. Please try again later."
            return
        }
        signInError = null
        deleteMessage = null
        signInPending = true
        viewModelScope.launch {
            try {
                auth.signIn(activity)
            } catch (e: GetCredentialCancellationException) {
                // user closed the Google dialog
            } catch (e: NoCredentialException) {
                signInError = "No Google account found on this phone. Add one in Settings and try again."
            } catch (e: Exception) {
                signInError = "Sign-in failed (${e.javaClass.simpleName}: ${e.message?.take(80) ?: "no details"}). Check your internet and try again."
            } finally {
                signInPending = false
            }
        }
    }

    fun deleteAccount(activity: Activity) {
        if (deletePending) return
        deletePending = true
        deleteMessage = null
        viewModelScope.launch {
            try {
                auth.deleteAccount(activity)
                deleteMessage = "Your account was deleted."
            } catch (e: GetCredentialCancellationException) {
                deleteMessage = "Deletion cancelled."
            } catch (e: Exception) {
                deleteMessage = "Could not delete the account (${e.javaClass.simpleName}: ${e.message?.take(80) ?: "no details"}). Please try again or contact support."
            } finally {
                deletePending = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    override fun onCleared() {
        speaker.shutdown()
        super.onCleared()
    }
}
