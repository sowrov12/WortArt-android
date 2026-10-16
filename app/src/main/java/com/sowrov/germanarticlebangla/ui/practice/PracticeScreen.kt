package com.sowrov.germanarticlebangla.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.sowrov.germanarticlebangla.BuildConfig
import com.sowrov.germanarticlebangla.R
import com.sowrov.germanarticlebangla.data.ARTICLES
import com.sowrov.germanarticlebangla.data.Article
import com.sowrov.germanarticlebangla.data.Category
import com.sowrov.germanarticlebangla.data.DAILY_GOAL
import com.sowrov.germanarticlebangla.data.DailyCounter
import com.sowrov.germanarticlebangla.data.LEVEL_IDS
import com.sowrov.germanarticlebangla.data.levelName
import com.sowrov.germanarticlebangla.data.levelPillText
import com.sowrov.germanarticlebangla.data.levelSummary
import com.sowrov.germanarticlebangla.data.ThemePref
import com.sowrov.germanarticlebangla.data.Word
import com.sowrov.germanarticlebangla.data.categoryImageBackupUrl
import com.sowrov.germanarticlebangla.data.categoryImageUrl
import com.sowrov.germanarticlebangla.data.describeMatch
import com.sowrov.germanarticlebangla.data.getActiveStreak
import com.sowrov.germanarticlebangla.data.getRuleMatch
import com.sowrov.germanarticlebangla.data.getRuleTarget
import com.sowrov.germanarticlebangla.data.getTodayUsage
import com.sowrov.germanarticlebangla.data.isCorrectArticle
import com.sowrov.germanarticlebangla.data.isCustomDeckId
import com.sowrov.germanarticlebangla.data.wordsForDeck
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.AppViewModel
import com.sowrov.germanarticlebangla.ui.ExploreSection
import com.sowrov.germanarticlebangla.ui.IconTile
import com.sowrov.germanarticlebangla.ui.MenuRow
import com.sowrov.germanarticlebangla.ui.PluralDialog
import com.sowrov.germanarticlebangla.ui.RemoteImage
import com.sowrov.germanarticlebangla.ui.RollingNumber
import com.sowrov.germanarticlebangla.ui.RoundIconButton
import com.sowrov.germanarticlebangla.ui.SwitchRow
import com.sowrov.germanarticlebangla.ui.theme.DarkColors
import com.sowrov.germanarticlebangla.ui.theme.LocalWortColors
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme
import com.sowrov.germanarticlebangla.util.SoundFx
import kotlinx.coroutines.delay

data class AnswerState(val picked: Article, val isCorrect: Boolean)

/** Practice progress kept in the ViewModel, so leaving the Practice tab and coming back keeps the same card. */
class PracticeSession(val key: List<Any>, initial: List<Word>) {
    var cards by mutableStateOf(initial)
    var index by mutableIntStateOf(0)
    var answer by mutableStateOf<AnswerState?>(null)
}

private const val CORRECT_DELAY_MS = 450L

@Composable
fun PracticeScreen(vm: AppViewModel, onToggleReminders: (Boolean) -> Unit) {
    val c = WortTheme.colors
    if (!vm.catalogReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = c.primary)
        }
        return
    }
    val progress by vm.progress.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val customDecks by vm.customDecks.collectAsStateWithLifecycle()
    val settings = progress.settings
    val deck = vm.effectiveDeck()
    val deckLocked = access.lockedIds.contains(deck)

    val resetKey = listOf<Any>(deck, settings.levels)
    val session = remember(resetKey) {
        vm.practiceSession(resetKey) {
            wordsForDeck(
                catalog.words, deck, progress.favorites, settings.levels, access.lockedIds, customDecks,
            ).shuffled()
        }
    }
    var cards by session::cards
    var index by session::index
    var answer by session::answer

    // When the live word list or Premium status arrives after launch, add the new words
    // but keep showing the card the user is already looking at.
    LaunchedEffect(access.lockedIds, catalog.version) {
        val fresh = wordsForDeck(
            catalog.words, deck, progress.favorites, settings.levels, access.lockedIds, customDecks,
        )
        val existingIds = session.cards.map { it.id }.toHashSet()
        if (fresh.size == existingIds.size && fresh.all { existingIds.contains(it.id) }) return@LaunchedEffect
        val shuffled = fresh.shuffled()
        val currentWord = session.cards.getOrNull(session.index)
        // Keep showing the card on screen, even if the new list no longer contains it.
        session.cards = if (currentWord != null && shuffled.isNotEmpty()) {
            listOf(currentWord) + shuffled.filter { it.id != currentWord.id }
        } else {
            shuffled
        }
        session.index = 0
    }
    var shownAt by remember(resetKey) { mutableLongStateOf(System.currentTimeMillis()) }

    val current: Word? = cards.getOrNull(index)
    val context = LocalContext.current

    fun advance() {
        val list = cards
        if (list.isEmpty()) return
        answer = null
        shownAt = System.currentTimeMillis()
        if (index + 1 >= list.size) {
            var reshuffled = list.shuffled()
            if (reshuffled.size > 1 && reshuffled[0].id == list[index].id) {
                reshuffled = reshuffled.drop(1) + reshuffled[0]
            }
            cards = reshuffled
            index = 0
        } else {
            index += 1
        }
    }

    // Correct answers move on by themselves; wrong ones wait for "Next word".
    LaunchedEffect(answer) {
        if (answer?.isCorrect == true) {
            delay(CORRECT_DELAY_MS)
            advance()
        }
    }

    // Read each new word aloud.
    LaunchedEffect(current?.id, index, cards) {
        val w = current
        if (w != null && settings.readAloud) vm.speak(w.word)
    }

    // Warm the image cache for the next few cards.
    LaunchedEffect(cards, index) {
        if (cards.isEmpty()) return@LaunchedEffect
        for (step in 1..3) {
            val upcoming = cards[(index + step) % cards.size]
            val cat = catalog.getCategory(upcoming.category)
            for (bg in listOf(true, false)) {
                val url = categoryImageUrl(cat, bg) ?: continue
                context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }

    var showVoice by remember { mutableStateOf(false) }
    var showPlural by remember { mutableStateOf(false) }
    LaunchedEffect(current?.id) { showPlural = false }
    var micDenied by remember { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.stopSpeaking()
            showVoice = true
        } else {
            micDenied = true
        }
    }

    fun openVoice() {
        if (current == null || answer != null) return
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) {
            vm.stopSpeaking()
            showVoice = true
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleAnswer(picked: Article) {
        val w = current ?: return
        if (answer != null) return
        if (!vm.tryUse(DailyCounter.CARDS_PRACTICED)) return
        val correct = isCorrectArticle(w, picked)
        SoundFx.play(correct)
        vm.recordAnswer(correct, System.currentTimeMillis() - shownAt)
        answer = AnswerState(picked, correct)
    }

    // A recognized attempt counts in the pronunciation stats. A perfect one is a correct answer for the card.
    fun handleVoiceResult(w: Word, perfect: Boolean) {
        vm.recordPronunciation(perfect)
        if (!perfect) {
            SoundFx.play(false)
            return
        }
        if (answer != null) return
        showVoice = false
        if (!vm.tryUse(DailyCounter.CARDS_PRACTICED)) return
        SoundFx.play(true)
        vm.recordAnswer(true, System.currentTimeMillis() - shownAt)
        answer = AnswerState(w.articles.first(), true)
    }

    val stats = progress.stats
    val successRate = if (stats.cardsPlayed > 0) Math.round(stats.correct * 100f / stats.cardsPlayed) else 0
    val isWrong = answer?.isCorrect == false
    val currentCategory = if (current != null) catalog.getCategory(current.category) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PracticeHeader(
            vm = vm,
            deck = deck,
            streak = getActiveStreak(stats),
            dailyCount = getTodayUsage(progress).cardsPracticed,
            canSkip = current != null,
            currentWord = current?.word,
            onSkip = { advance() },
            onToggleReminders = onToggleReminders,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("RUN", Modifier.weight(1f)) {
                RollingNumber(stats.currentRun, c.foreground, 18.sp, fontFamily = SerifFont)
            }
            StatBox("PLAYED", Modifier.weight(1f)) {
                Text(stats.cardsPlayed.toString(), color = c.foreground, fontSize = 18.sp, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold)
            }
            StatBox("SUCCESS", Modifier.weight(1f)) {
                Text("$successRate%", color = c.foreground, fontSize = 18.sp, fontFamily = SerifFont, fontWeight = FontWeight.SemiBold)
            }
        }

        if (deckLocked) {
            InfoPane(
                icon = AppIcons.Crown,
                title = if (BuildConfig.PLAY_BUILD) "${catalog.getCategory(deck).label} is for members" else "${catalog.getCategory(deck).label} is Premium",
                body = if (BuildConfig.PLAY_BUILD) {
                    "This category is for members."
                } else {
                    "Unlock every category, unlimited practice and favorites with Premium."
                },
                button = if (BuildConfig.PLAY_BUILD) "Member access" else "Get Premium",
                onClick = { vm.openPremiumTab() },
                modifier = Modifier.weight(1f),
            )
        } else if (cards.isEmpty()) {
            val isFav = deck == "favorites"
            val levelText = if (settings.levels.size >= LEVEL_IDS.size) "" else LEVEL_IDS.filter { settings.levels.contains(it) }.joinToString("/") + " "
            InfoPane(
                icon = if (isFav) AppIcons.Favorite else AppIcons.Layers,
                title = if (isFav) "No favorites yet" else "No ${levelText}words here",
                body = if (isFav) {
                    "Tap the heart on any word card or in search to build your own practice deck."
                } else {
                    "Try another category, or switch level from the menu."
                },
                button = if (isFav) "Find words" else "Browse categories",
                onClick = { vm.openExplore(if (isFav) ExploreSection.Search else ExploreSection.Categories) },
                modifier = Modifier.weight(1f),
            )
        } else {
            androidx.compose.runtime.key(current?.id) {
                WordCard(
                    word = current,
                    answer = answer,
                    isFavorite = current != null && progress.favorites.contains(current.id),
                    category = currentCategory,
                    onToggleFavorite = { current?.let { vm.toggleFavoriteLimited(it.id) } },
                    onSpeak = {
                        val w = current
                        if (w != null) {
                            vm.speak(if (answer != null) "${w.articles.joinToString(" oder ") { it.name }} ${w.word}" else w.word)
                        }
                    },
                    onMic = { openVoice() },
                    onInfo = { showPlural = true },
                    onOpenRule = {
                        val w = current
                        if (w != null && vm.tryUse(DailyCounter.RULES_OPENED)) {
                            vm.countRuleOpen()
                            vm.openRule(getRuleTarget(w))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (isWrong) {
                Button(
                    onClick = { advance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
                ) {
                    Text("Next word", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Icon(AppIcons.Forward, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                }
            } else {
                ArticleButtons(
                    answer = answer,
                    correctArticles = current?.articles ?: emptyList(),
                    enabled = current != null,
                    onPick = { handleAnswer(it) },
                )
            }
        }

        val voiceWord = current
        if (showVoice && voiceWord != null && answer == null) {
            PronunciationDialog(
                word = voiceWord,
                onResult = { perfect -> handleVoiceResult(voiceWord, perfect) },
                canListen = { vm.tryUse(DailyCounter.VOICE_PRACTICED) },
                onDismiss = { showVoice = false },
            )
        }
        if (showPlural && voiceWord != null) {
            PluralDialog(voiceWord) { showPlural = false }
        }
        if (micDenied) {
            AlertDialog(
                onDismissRequest = { micDenied = false },
                containerColor = c.card,
                title = { Text("Microphone needed", color = c.foreground) },
                text = {
                    Text(
                        "Allow microphone access for this app in your phone settings to practice pronunciation.",
                        color = c.mutedForeground,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { micDenied = false }) { Text("OK", color = c.primary) }
                },
            )
        }
    }
}

@Composable
private fun StatBox(label: String, modifier: Modifier, value: @Composable () -> Unit) {
    val c = WortTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = c.mutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
        value()
    }
}

@Composable
private fun InfoPane(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WortTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.card)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(icon, c.primary, size = 64, round = true)
        Text(
            title, color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            body, color = c.mutedForeground, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
        ) { Text(button) }
    }
}

// ---------------------------------------------------------------------------------------------
// Article buttons
// ---------------------------------------------------------------------------------------------

@Composable
fun ArticleButtons(
    answer: AnswerState?,
    correctArticles: List<Article>,
    enabled: Boolean,
    onPick: (Article) -> Unit,
) {
    val c = WortTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (article in ARTICLES) {
            val isPicked = answer?.picked == article
            val showCorrect = answer != null && correctArticles.contains(article)
            val showWrong = isPicked && answer?.isCorrect == false
            val dimmed = answer != null && !isPicked && !showCorrect
            val bg = when {
                showCorrect -> c.success
                showWrong -> c.destructive
                else -> c.card
            }
            val fg = if (showCorrect || showWrong) c.background else c.foreground
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .graphicsLayer { alpha = if (dimmed) 0.4f else 1f }
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(1.dp, if (showCorrect || showWrong) bg else c.border, RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled && answer == null) { onPick(article) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showCorrect) Icon(AppIcons.Check, null, tint = fg, modifier = Modifier.size(20.dp))
                    if (showWrong) Icon(AppIcons.Close, null, tint = fg, modifier = Modifier.size(20.dp))
                    Text(article.name, color = fg, fontFamily = SerifFont, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(24.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (showCorrect || showWrong) fg.copy(alpha = 0.4f) else c.article(article)),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Word card (always dark, like the website)
// ---------------------------------------------------------------------------------------------

@Composable
fun WordCard(
    word: Word?,
    answer: AnswerState?,
    isFavorite: Boolean,
    category: Category?,
    onToggleFavorite: () -> Unit,
    onSpeak: () -> Unit,
    onOpenRule: () -> Unit,
    onMic: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalWortColors provides DarkColors) {
        val c = DarkColors
        val shape = RoundedCornerShape(28.dp)
        if (word == null || category == null) {
            Box(modifier.fillMaxWidth().clip(shape).background(c.card))
            return@CompositionLocalProvider
        }

        val density = LocalDensity.current.density
        val shake = remember { Animatable(0f) }
        val pop = remember { Animatable(1f) }
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, tween(260)) }
        LaunchedEffect(answer) {
            val a = answer
            if (a == null) return@LaunchedEffect
            if (a.isCorrect) {
                pop.animateTo(1.05f, tween(140))
                pop.animateTo(1f, tween(200))
            } else {
                for (d in listOf(-14f, 14f, -9f, 9f, 0f)) shake.animateTo(d, tween(60))
            }
        }

        val ring by animateFloatAsState(if (answer != null) 1f else 0f, label = "ring")
        val ringColor = when {
            answer?.isCorrect == true -> c.success.copy(alpha = 0.6f * ring)
            answer != null -> c.destructive.copy(alpha = 0.6f * ring)
            else -> Color.Transparent
        }
        var bgLoaded by remember { mutableStateOf(false) }
        val articleColor = c.article(word.articles.first())
        val fullWord = "${word.articleLabel} ${word.word}"
        val hint = remember(word.id) { describeMatch(word, getRuleMatch(word)) }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = shake.value * density + (1f - enter.value) * 40f * density
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = enter.value
                }
                .clip(shape)
                .background(c.card)
                .border(2.dp, ringColor, shape),
        ) {
            // Column H: full-card cover image with a dark gradient so text stays readable.
            RemoteImage(
                url = categoryImageUrl(category, true),
                backupUrl = categoryImageBackupUrl(category, true),
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { bgLoaded = true },
            )
            if (bgLoaded) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.8f)))),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp, bottom = 104.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                Box {
                    RemoteImage(
                        url = categoryImageUrl(category, false),
                        backupUrl = categoryImageBackupUrl(category, false),
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .border(3.dp, c.primary, CircleShape),
                        fallback = {
                            IconTile(
                                AppIcons.forCategory(category.id), c.primary, size = 96, round = true,
                                background = c.primary.copy(alpha = 0.12f),
                            )
                        },
                    )
                    if (answer != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (answer.isCorrect) c.success else c.destructive),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (answer.isCorrect) AppIcons.Check else AppIcons.Close,
                                contentDescription = null, tint = c.background, modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
                        if (answer != null) {
                            Text(word.articleLabel, color = articleColor, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("___", color = c.mutedForeground.copy(alpha = 0.6f), fontFamily = SerifFont, fontSize = 20.sp)
                        }
                    }
                    Text(
                        word.word, color = c.foreground, fontFamily = SerifFont, fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(word.en, color = c.mutedForeground, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Text(word.bn, color = c.foreground.copy(alpha = 0.9f), fontSize = 18.sp, textAlign = TextAlign.Center)
                }

                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    if (answer != null && answer.isCorrect) {
                        Text("Richtig! Correct", color = c.success, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (answer != null && !answer.isCorrect) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(c.background.copy(alpha = 0.8f))
                                .border(BorderStroke(1.dp, c.destructive.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(c.destructive),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(AppIcons.Close, null, tint = c.background, modifier = Modifier.size(16.dp)) }
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = c.destructive, textDecoration = TextDecoration.LineThrough)) {
                                            append(answer.picked.name)
                                        }
                                        append(" ")
                                        append(word.word)
                                    },
                                    color = c.foreground, fontFamily = SerifFont, fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                RoundIconButton(AppIcons.Forward, "Open the grammar rule for $fullWord", c.background, c.destructive, onOpenRule)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(AppIcons.Bulb, null, tint = c.primary, modifier = Modifier.padding(top = 2.dp).size(14.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append("It's ")
                                        withStyle(SpanStyle(color = articleColor, fontWeight = FontWeight.SemiBold)) { append(fullWord) }
                                        append(". ")
                                        append(hint)
                                    },
                                    color = c.mutedForeground, fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            if (answer == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(c.primary)
                        .clickable(onClick = onMic),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Mic, contentDescription = "Say the word", tint = c.primaryForeground, modifier = Modifier.size(30.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(AppIcons.Volume, "Hear pronunciation of ${word.word}", c.foreground, c.secondary, onSpeak)
                Row(
                    modifier = Modifier.clip(CircleShape).background(c.secondary).padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        word.level, color = c.mutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onInfo),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.Info, contentDescription = "Plural form", tint = c.mutedForeground, modifier = Modifier.size(17.dp))
                    }
                }
                RoundIconButton(
                    if (isFavorite) AppIcons.Favorite else AppIcons.FavoriteOutline,
                    if (isFavorite) "Remove from favorites" else "Add to favorites",
                    if (isFavorite) c.die else c.mutedForeground, c.secondary, onToggleFavorite,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header + menu
// ---------------------------------------------------------------------------------------------

/** Before the daily goal: a progress bar. After 10 cards: a full "DAY n" streak pill. */
@Composable
private fun StreakPill(goalDone: Boolean, streak: Int, fill: Float, modifier: Modifier, onClick: () -> Unit) {
    val c = WortTheme.colors
    val animatedFill by animateFloatAsState(fill, label = "goal-fill")
    if (goalDone) {
        Box(
            modifier = modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4A34B))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons.Flame, contentDescription = null, tint = Color(0xFF1D140D), modifier = Modifier.size(20.dp))
                Text("DAY $streak", color = Color(0xFF1D140D), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Box(
            modifier = modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(c.card)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(c.secondary))
            Box(Modifier.fillMaxWidth(animatedFill).height(14.dp).clip(CircleShape).background(c.primary))
        }
    }
}

private enum class MenuPage { Main, Level, Decks, Theme }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeHeader(
    vm: AppViewModel,
    deck: String,
    streak: Int,
    dailyCount: Int,
    canSkip: Boolean,
    currentWord: String?,
    onSkip: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
) {
    val c = WortTheme.colors
    val progress by vm.progress.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val customDecks by vm.customDecks.collectAsStateWithLifecycle()
    val themePref by vm.themePref.collectAsStateWithLifecycle()
    val settings = progress.settings

    var menuOpen by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(MenuPage.Main) }

    val customDeck = customDecks.firstOrNull { it.id == deck }
    val category = catalog.getCategory(deck)
    val title = customDeck?.name ?: category.label
    val titleIcon = if (customDeck != null) AppIcons.Layers else AppIcons.forCategory(category.id)
    val goalDone = dailyCount >= DAILY_GOAL
    val fill = (dailyCount.toFloat() / DAILY_GOAL).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 140.dp)
                .clip(CircleShape)
                .background(c.card)
                .clickable { vm.openExplore(ExploreSection.Categories) }
                .padding(start = 6.dp, top = 6.dp, bottom = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconTile(titleIcon, c.primary, size = 28, round = true)
            Text(title, color = c.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Icon(AppIcons.ChevronDown, null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
        }
        // Tapping the level opens the level choices.
        Text(
            levelPillText(settings.levels), color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .background(c.card)
                .clickable { page = MenuPage.Level; menuOpen = true }
                .padding(horizontal = 10.dp, vertical = 7.dp),
        )
        StreakPill(
            goalDone = goalDone,
            streak = streak,
            fill = fill,
            modifier = Modifier.weight(1f),
            onClick = { vm.openStats() },
        )
        Image(
            painter = painterResource(R.drawable.ic_settings_tile),
            contentDescription = "Menu",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { page = MenuPage.Main; menuOpen = true },
        )
    }

    if (menuOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = sheetState,
            containerColor = c.card,
            contentColor = c.foreground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 24.dp),
            ) {
                when (page) {
                    MenuPage.Main -> {
                        MenuRow(AppIcons.School, "Level", { page = MenuPage.Level }, trailing = levelSummary(settings.levels))
                        SwitchRow(AppIcons.Bell, "Reminders", "Daily practice nudge", settings.reminders, onToggleReminders)
                        SwitchRow(AppIcons.Audio, "Read flashcards", "Speak each new word", settings.readAloud) {
                            vm.setReadAloud(it, currentWord)
                        }
                        MenuRow(AppIcons.Skip, "Skip word", { onSkip(); menuOpen = false }, enabled = canSkip)
                        MenuRow(AppIcons.Grid, "German cases", { menuOpen = false; vm.openCases() })
                        MenuRow(AppIcons.Book, "Grammar rules", { menuOpen = false; vm.openExplore(ExploreSection.Rules) })
                        MenuRow(AppIcons.Search, "Search words", { menuOpen = false; vm.openExplore(ExploreSection.Search) })
                        MenuRow(AppIcons.Chart, "Statistics", { menuOpen = false; vm.openStats() })
                        MenuRow(AppIcons.ThemeMix, "Theme", { page = MenuPage.Theme })
                    }
                    MenuPage.Level -> {
                        SubHeader("Level") { page = MenuPage.Main }
                        Text(
                            "Choose one or more levels.", color = c.mutedForeground, fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        )
                        for (id in LEVEL_IDS) {
                            ChoiceRow(
                                label = "$id  ·  ${levelName(id)}",
                                selected = settings.levels.contains(id),
                            ) { vm.toggleLevel(id) }
                        }
                    }
                    MenuPage.Decks -> {
                        SubHeader("Categories") { page = MenuPage.Main }
                        for (item in catalog.decks) {
                            val locked = access.lockedIds.contains(item.id)
                            ChoiceRow(
                                label = item.label,
                                selected = deck == item.id,
                                icon = AppIcons.forCategory(item.id),
                                locked = locked,
                            ) { menuOpen = false; vm.selectDeck(item.id) }
                        }
                        for (item in customDecks) {
                            ChoiceRow(
                                label = item.name,
                                selected = deck == item.id,
                                icon = AppIcons.Layers,
                            ) { menuOpen = false; vm.selectDeck(item.id) }
                        }
                    }
                    MenuPage.Theme -> {
                        SubHeader("Theme") { page = MenuPage.Main }
                        ChoiceRow("Phone setting", themePref == ThemePref.SYSTEM, AppIcons.Phone) { vm.setTheme(ThemePref.SYSTEM) }
                        ChoiceRow("Light", themePref == ThemePref.LIGHT, AppIcons.Sun) { vm.setTheme(ThemePref.LIGHT) }
                        ChoiceRow("Dark", themePref == ThemePref.DARK, AppIcons.Moon) { vm.setTheme(ThemePref.DARK) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    val c = WortTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoundIconButton(AppIcons.Back, "Back", c.foreground, c.secondary, onBack, size = 36)
        Text(title, color = c.foreground, fontFamily = SerifFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    locked: Boolean = false,
    onClick: () -> Unit,
) {
    val c = WortTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = c.primary, modifier = Modifier.size(20.dp))
        Text(label, color = c.foreground, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (locked) Icon(AppIcons.Lock, "Premium", tint = c.primary, modifier = Modifier.size(16.dp))
        if (selected) Icon(AppIcons.Check, null, tint = c.primary, modifier = Modifier.size(18.dp))
    }
}
