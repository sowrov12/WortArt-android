package com.sowrov.germanarticlebangla.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sowrov.germanarticlebangla.data.Article
import com.sowrov.germanarticlebangla.data.Category
import com.sowrov.germanarticlebangla.data.CustomDeck
import com.sowrov.germanarticlebangla.data.FREE_CATEGORIES_PER_DECK
import com.sowrov.germanarticlebangla.data.FREE_CUSTOM_DECKS
import com.sowrov.germanarticlebangla.data.LevelFilter
import com.sowrov.germanarticlebangla.data.RULE_GROUPS
import com.sowrov.germanarticlebangla.data.Rule
import com.sowrov.germanarticlebangla.data.RuleGroup
import com.sowrov.germanarticlebangla.data.Word
import com.sowrov.germanarticlebangla.data.categoryImageBackupUrl
import com.sowrov.germanarticlebangla.data.categoryImageUrl
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.AppViewModel
import com.sowrov.germanarticlebangla.ui.ExploreSection
import com.sowrov.germanarticlebangla.ui.IconTile
import com.sowrov.germanarticlebangla.ui.RemoteImage
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortColors
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

private sealed interface RuleRow {
    class Header(val group: RuleGroup, val open: Boolean) : RuleRow
    class Item(val rule: Rule) : RuleRow
    class CasesHeader(val open: Boolean) : RuleRow
    object CasesTable : RuleRow
}

@Composable
fun ExploreScreen(vm: AppViewModel) {
    val c = WortTheme.colors
    val section = vm.exploreSection
    val target = vm.ruleTarget
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val customDecks by vm.customDecks.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var openGroups by remember { mutableStateOf(setOf(target?.article ?: Article.der)) }
    var casesOpen by remember { mutableStateOf(vm.casesTarget) }
    var query by remember { mutableStateOf("") }
    var searchLevel by remember { mutableStateOf(LevelFilter.ALL) }
    var editingDeckId by remember { mutableStateOf<String?>(null) }

    val ruleRows = remember(openGroups, casesOpen) {
        val groupRows = RULE_GROUPS.flatMap { group ->
            val open = openGroups.contains(group.article)
            listOf<RuleRow>(RuleRow.Header(group, open)) +
                (if (open) group.rules.map { RuleRow.Item(it) } else emptyList())
        }
        groupRows + listOf<RuleRow>(RuleRow.CasesHeader(casesOpen)) +
            (if (casesOpen) listOf<RuleRow>(RuleRow.CasesTable) else emptyList())
    }
    val latestRows by rememberUpdatedState(ruleRows)

    // Arriving from a wrong answer: open that rule's group and scroll to the rule.
    LaunchedEffect(target) {
        val t = target ?: return@LaunchedEffect
        if (!openGroups.contains(t.article)) openGroups = openGroups + t.article
        kotlinx.coroutines.delay(250)
        val index = latestRows.indexOfFirst { row ->
            when (row) {
                is RuleRow.Item -> t.ruleId != null && row.rule.id == t.ruleId
                is RuleRow.Header -> t.ruleId == null && row.group.article == t.article
                else -> false
            }
        }
        if (index >= 0) listState.animateScrollToItem(2 + index)
    }

    // Sent here from the menu: open the "German cases" table and scroll to it.
    LaunchedEffect(vm.casesTarget) {
        if (!vm.casesTarget) return@LaunchedEffect
        casesOpen = true
        kotlinx.coroutines.delay(250)
        val index = latestRows.indexOfFirst { it is RuleRow.CasesHeader }
        if (index >= 0) listState.animateScrollToItem(2 + index)
        vm.consumeCasesTarget()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column {
                Text("Explore", color = c.foreground, fontFamily = SerifFont, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("Pick a category, learn the rules, or search any word.", color = c.mutedForeground, fontSize = 14.sp)
            }
        }
        item(key = "tabs") {
            Row(
                modifier = Modifier.fillMaxWidth().clip(CircleShape).background(c.card).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionTab("Categories", AppIcons.Grid, section == ExploreSection.Categories, Modifier.weight(1f)) {
                    vm.changeExploreSection(ExploreSection.Categories)
                }
                SectionTab("Rules", AppIcons.Book, section == ExploreSection.Rules, Modifier.weight(1f)) {
                    vm.changeExploreSection(ExploreSection.Rules)
                }
                SectionTab("Search", AppIcons.Search, section == ExploreSection.Search, Modifier.weight(1f)) {
                    vm.changeExploreSection(ExploreSection.Search)
                }
            }
        }

        when (section) {
            ExploreSection.Categories -> categoriesSection(
                c = c,
                topics = catalog.topics,
                words = catalog.words,
                lockedIds = access.lockedIds,
                favoritesCount = progress.favorites.size,
                customDecks = customDecks,
                selectedDeck = vm.effectiveDeck(),
                onSelect = { vm.selectDeck(it) },
                onNewDeck = {
                    if (!access.isPremium && customDecks.size >= FREE_CUSTOM_DECKS) {
                        vm.askPremium(
                            "Multi-category decks are limited",
                            "Free accounts can make $FREE_CUSTOM_DECKS multi-category deck. Premium unlocks unlimited decks.",
                        )
                    } else {
                        editingDeckId = vm.createDeck().id
                    }
                },
                onEditDeck = { editingDeckId = it },
            )
            ExploreSection.Rules -> rulesSection(
                c = c,
                rows = ruleRows,
                targetRuleId = target?.ruleId,
                targetPattern = target?.pattern,
                casesOpen = casesOpen,
                onToggleCases = { casesOpen = !casesOpen },
                onToggleGroup = { article ->
                    openGroups = if (openGroups.contains(article)) openGroups - article else openGroups + article
                },
            )
            ExploreSection.Search -> searchSection(
                c = c,
                words = catalog.words,
                lockedIds = access.lockedIds,
                favorites = progress.favorites,
                query = query,
                onQueryChange = { query = it },
                level = searchLevel,
                onLevelChange = { searchLevel = it },
                getCategory = { catalog.getCategory(it) },
                onToggleFavorite = { vm.toggleFavoriteLimited(it) },
            )
        }
    }

    val editing = customDecks.firstOrNull { it.id == editingDeckId }
    if (editing != null) {
        DeckEditorSheet(
            deck = editing,
            topics = catalog.topics,
            lockedIds = access.lockedIds,
            isPremium = access.isPremium,
            onRename = { vm.renameDeck(editing.id, it) },
            onSetCategories = { vm.setDeckCategories(editing.id, it) },
            onLimit = {
                vm.askPremium(
                    "More categories need Premium",
                    "Free accounts can combine up to $FREE_CATEGORIES_PER_DECK categories in one deck.",
                )
            },
            onDelete = { vm.removeDeck(editing.id); editingDeckId = null },
            onPractice = { editingDeckId = null; vm.selectDeck(editing.id) },
            onDismiss = { editingDeckId = null },
        )
    }
}

@Composable
private fun SectionTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = WortTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) c.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) c.primaryForeground else c.mutedForeground
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Categories
// ---------------------------------------------------------------------------------------------

private fun LazyListScope.categoriesSection(
    c: WortColors,
    topics: List<Category>,
    words: List<Word>,
    lockedIds: Set<String>,
    favoritesCount: Int,
    customDecks: List<CustomDeck>,
    selectedDeck: String,
    onSelect: (String) -> Unit,
    onNewDeck: () -> Unit,
    onEditDeck: (String) -> Unit,
) {
    val counts = words.groupingBy { it.category }.eachCount()

    item(key = "quick") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard("All words", "Every category", AppIcons.Layers, selectedDeck == "all", Modifier.weight(1f)) { onSelect("all") }
            QuickCard("Favorites", "$favoritesCount saved", AppIcons.Favorite, selectedDeck == "favorites", Modifier.weight(1f)) { onSelect("favorites") }
        }
    }

    items(topics.chunked(2), key = { row -> row.first().id }) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (topic in row) {
                CategoryCard(
                    category = topic,
                    count = counts[topic.id] ?: 0,
                    locked = lockedIds.contains(topic.id),
                    selected = selectedDeck == topic.id,
                    modifier = Modifier.weight(1f),
                ) { onSelect(topic.id) }
            }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }

    item(key = "decks-title") {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text("Multi-category decks", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Combine several categories into one practice deck.", color = c.mutedForeground, fontSize = 13.sp)
        }
    }
    items(customDecks, key = { it.id }) { deck ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.card)
                .border(1.dp, if (selectedDeck == deck.id) c.primary else c.border, RoundedCornerShape(20.dp))
                .clickable { onSelect(deck.id) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(AppIcons.Layers, c.primary, size = 44)
            Column(Modifier.weight(1f)) {
                Text(deck.name, color = c.foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${deck.categoryIds.size} categories", color = c.mutedForeground, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.secondary).clickable { onEditDeck(deck.id) },
                contentAlignment = Alignment.Center,
            ) { Icon(AppIcons.Edit, "Edit deck", tint = c.foreground, modifier = Modifier.size(18.dp)) }
        }
    }
    item(key = "new-deck") {
        OutlinedButton(
            onClick = onNewDeck,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, c.border),
        ) {
            Icon(AppIcons.Add, null, tint = c.primary, modifier = Modifier.size(18.dp))
            Text("New multi-category deck", color = c.foreground, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun QuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = WortTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .border(1.dp, if (selected) c.primary else c.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon, c.primary, size = 40)
        Column {
            Text(title, color = c.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = c.mutedForeground, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    count: Int,
    locked: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = WortTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(132.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, if (selected) c.primary else c.border, shape)
            .clickable(onClick = onClick),
    ) {
        var hasImage by remember(category.id) { mutableStateOf(false) }
        RemoteImage(
            url = categoryImageUrl(category, true),
            backupUrl = categoryImageBackupUrl(category, true),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { hasImage = true },
        )
        if (hasImage) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.75f)))
                )
            )
        }
        val textColor = if (hasImage) Color.White else c.foreground
        val subColor = if (hasImage) Color.White.copy(alpha = 0.75f) else c.mutedForeground
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                RemoteImage(
                    url = categoryImageUrl(category, false),
                    backupUrl = categoryImageBackupUrl(category, false),
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    fallback = { IconTile(AppIcons.forCategory(category.id), c.primary, size = 40, round = true) },
                )
                if (locked) {
                    Icon(AppIcons.Lock, "Premium", tint = c.primary, modifier = Modifier.size(18.dp))
                } else if (category.premium) {
                    Icon(AppIcons.Crown, "Premium", tint = c.primary, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(category.label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val bn = category.bn
                if (bn != null) Text(bn, color = subColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckEditorSheet(
    deck: CustomDeck,
    topics: List<Category>,
    lockedIds: Set<String>,
    isPremium: Boolean,
    onRename: (String) -> Unit,
    onSetCategories: (List<String>) -> Unit,
    onLimit: () -> Unit,
    onDelete: () -> Unit,
    onPractice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = WortTheme.colors
    var name by remember(deck.id) { mutableStateOf(deck.name) }
    ModalBottomSheet(
        onDismissRequest = { onRename(name); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.card,
        contentColor = c.foreground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Edit deck", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            AppTextField(name, { name = it.take(30) }, "Deck name")
            Text("Categories", color = c.mutedForeground, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            for (topic in topics) {
                val checked = deck.categoryIds.contains(topic.id)
                val locked = lockedIds.contains(topic.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (locked) {
                                onLimit()
                            } else if (checked) {
                                onSetCategories(deck.categoryIds.filter { it != topic.id })
                            } else if (!isPremium && deck.categoryIds.size >= FREE_CATEGORIES_PER_DECK) {
                                onLimit()
                            } else {
                                onSetCategories(deck.categoryIds + topic.id)
                            }
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = c.primary, checkmarkColor = c.primaryForeground, uncheckedColor = c.mutedForeground),
                    )
                    Text(topic.label, color = if (locked) c.mutedForeground else c.foreground, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    if (locked) Icon(AppIcons.Lock, "Premium", tint = c.primary, modifier = Modifier.size(16.dp))
                }
            }
            Button(
                onClick = { onRename(name); onPractice() },
                enabled = deck.categoryIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 0.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
            ) { Text("Practice this deck") }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, c.destructive.copy(alpha = 0.5f)),
            ) {
                Icon(AppIcons.Delete, null, tint = c.destructive, modifier = Modifier.size(18.dp))
                Text("Delete deck", color = c.destructive, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun AppTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = WortTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = c.mutedForeground) },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.secondary,
            unfocusedContainerColor = c.secondary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = c.foreground,
            unfocusedTextColor = c.foreground,
            cursorColor = c.primary,
        ),
    )
}

// ---------------------------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.rulesSection(
    c: WortColors,
    rows: List<RuleRow>,
    targetRuleId: String?,
    targetPattern: String?,
    casesOpen: Boolean,
    onToggleCases: () -> Unit,
    onToggleGroup: (Article) -> Unit,
) {
    items(rows, key = { row ->
        when (row) {
            is RuleRow.Header -> "group-${row.group.article.name}"
            is RuleRow.Item -> "rule-${row.rule.id}"
            is RuleRow.CasesHeader -> "cases-header"
            is RuleRow.CasesTable -> "cases-table"
        }
    }) { row ->
        when (row) {
            is RuleRow.CasesHeader -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.card)
                        .clickable { onToggleCases() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(c.primary))
                    Column(Modifier.weight(1f)) {
                        Text("German cases", color = c.primary, fontFamily = SerifFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Nominativ, Akkusativ and Dativ: the article table.", color = c.mutedForeground, fontSize = 12.sp)
                    }
                    Icon(
                        if (row.open) AppIcons.ChevronDown else AppIcons.ChevronRight,
                        null, tint = c.mutedForeground, modifier = Modifier.size(20.dp),
                    )
                }
            }
            is RuleRow.CasesTable -> CasesTable()
            is RuleRow.Header -> {
                val color = c.article(row.group.article)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.card)
                        .clickable { onToggleGroup(row.group.article) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Column(Modifier.weight(1f)) {
                        Text(row.group.label, color = color, fontFamily = SerifFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(row.group.summary, color = c.mutedForeground, fontSize = 12.sp)
                    }
                    Icon(
                        if (row.open) AppIcons.ChevronDown else AppIcons.ChevronRight,
                        null, tint = c.mutedForeground, modifier = Modifier.size(20.dp),
                    )
                }
            }
            is RuleRow.Item -> {
                val rule = row.rule
                val color = c.article(rule.article)
                val isTarget = targetRuleId == rule.id
                val shape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                        .clip(shape)
                        .background(c.card)
                        .border(if (isTarget) 2.dp else 1.dp, if (isTarget) c.primary else c.border, shape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(rule.title, color = c.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            if (rule.strength == "always") "Always" else "Usually",
                            color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text(rule.description, color = c.mutedForeground, fontSize = 13.sp)
                    val examples = rule.examples
                    if (examples != null) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (example in examples) {
                                Text(
                                    "${rule.article.name} $example", color = c.foreground, fontSize = 12.sp,
                                    modifier = Modifier.clip(CircleShape).background(c.secondary).padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                    val patterns = rule.patterns
                    if (patterns != null) {
                        for (pattern in patterns) {
                            val hit = isTarget && targetPattern == pattern.label
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hit) c.primary.copy(alpha = 0.18f) else c.secondary)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(pattern.label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(pattern.example, color = c.mutedForeground, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------------------------

private const val MAX_RESULTS = 50

private fun LazyListScope.searchSection(
    c: WortColors,
    words: List<Word>,
    lockedIds: Set<String>,
    favorites: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    level: LevelFilter,
    onLevelChange: (LevelFilter) -> Unit,
    getCategory: (String) -> Category,
    onToggleFavorite: (String) -> Unit,
) {
    item(key = "search-field") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search German, English or Bengali", color = c.mutedForeground) },
                leadingIcon = { Icon(AppIcons.Search, null, tint = c.mutedForeground) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.card,
                    unfocusedContainerColor = c.card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = c.foreground,
                    unfocusedTextColor = c.foreground,
                    cursorColor = c.primary,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in LevelFilter.entries) {
                    val selected = option == level
                    Text(
                        option.short,
                        color = if (selected) c.primaryForeground else c.mutedForeground,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) c.primary else c.card)
                            .clickable { onLevelChange(option) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    val needle = query.trim().lowercase()
    if (needle.isEmpty()) {
        item(key = "search-hint") {
            Text("Type a word to find its article.", color = c.mutedForeground, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }

    val matches = words.filter { w ->
        !lockedIds.contains(w.category) &&
            (level == LevelFilter.ALL || w.level == level.id) &&
            (w.word.lowercase().contains(needle) || w.en.lowercase().contains(needle) || w.bn.lowercase().contains(needle))
    }
    if (matches.isEmpty()) {
        item(key = "search-empty") {
            Text("No words found.", color = c.mutedForeground, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        return
    }

    items(matches.take(MAX_RESULTS), key = { it.id }) { w ->
        val isFav = favorites.contains(w.id)
        val color = c.article(w.articles.first())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.articleLabel, color = color, fontFamily = SerifFont, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(" ${w.word}", color = c.foreground, fontFamily = SerifFont, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("${w.en}  ·  ${w.bn}", color = c.mutedForeground, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${w.level} · ${getCategory(w.category).label}", color = c.mutedForeground.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(c.secondary).clickable { onToggleFavorite(w.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isFav) AppIcons.Favorite else AppIcons.FavoriteOutline,
                    if (isFav) "Remove from favorites" else "Add to favorites",
                    tint = if (isFav) c.die else c.mutedForeground,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    if (matches.size > MAX_RESULTS) {
        item(key = "search-more") {
            Text("Showing the first $MAX_RESULTS results — refine your search.", color = c.mutedForeground, fontSize = 12.sp)
        }
    }
}
