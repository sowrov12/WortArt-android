package com.sowrov.germanarticlebangla.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.sowrov.germanarticlebangla.util.Reminders
import com.sowrov.germanarticlebangla.util.Speaker
import kotlinx.coroutines.Dispatchers
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

    init {
        // Refresh the words from the live Google Sheet in the background.
        viewModelScope.launch {
            val remote = withContext(Dispatchers.IO) { repository.fetchRemote() }
            if (remote != null) _catalog.value = remote
        }
        if (progressStore.progress.value.settings.reminders) Reminders.schedule(ctx)
    }

    // ----- navigation -----------------------------------------------------------------------

    fun setTab(value: Tab) {
        tab = value
    }

    fun openPremiumTab() {
        tab = Tab.Premium
    }

    fun openExplore(section: ExploreSection) {
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
        limitNotice = LimitNotice(title, body, resets = false)
    }

    fun dismissNotice() {
        limitNotice = null
    }

    // ----- progress -------------------------------------------------------------------------

    fun recordAnswer(isCorrect: Boolean, elapsedMs: Long) = progressStore.recordAnswer(isCorrect, elapsedMs)

    fun toggleFavorite(id: String) = progressStore.toggleFavorite(id)

    /** Favorites count toward the same free daily limit everywhere. */
    fun toggleFavoriteLimited(id: String) {
        val adding = !progressStore.progress.value.favorites.contains(id)
        if (adding && !tryUse(DailyCounter.FAVORITES_ADDED)) return
        progressStore.toggleFavorite(id)
    }

    fun countRuleOpen() = progressStore.countRuleOpen()

    fun resetStats() = progressStore.resetStats()

    fun setLevel(level: LevelFilter) = progressStore.updateSettings { it.copy(level = level) }

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
        signInError = null
        signInPending = true
        viewModelScope.launch {
            try {
                auth.signIn(activity)
            } catch (e: GetCredentialCancellationException) {
                // user closed the Google dialog
            } catch (e: Exception) {
                signInError = "Sign-in failed. Please try again."
            } finally {
                signInPending = false
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
