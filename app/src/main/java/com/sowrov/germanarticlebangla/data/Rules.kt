package com.sowrov.germanarticlebangla.data

class RulePattern(
    val label: String,
    val example: String,
    val test: (lowerWord: String, word: String) -> Boolean,
)

class Rule(
    val id: String,
    val article: Article,
    val title: String,
    /** "always" or "usually" */
    val strength: String,
    val description: String,
    val examples: List<String>? = null,
    val members: List<String>? = null,
    val patterns: List<RulePattern>? = null,
    val exception: Pair<Article, String>? = null,
)

class RuleGroup(
    val article: Article,
    val label: String,
    val summary: String,
    val rules: List<Rule>,
)

data class RuleTarget(
    val article: Article,
    val ruleId: String? = null,
    val pattern: String? = null,
)

private fun ending(suffix: String, example: String): RulePattern =
    RulePattern("-$suffix", example) { lower, _ ->
        lower.endsWith(suffix) && lower.length > suffix.length + 1
    }

private val MONTHS = listOf(
    "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember",
)
private val SEASONS = listOf("Frühling", "Sommer", "Herbst", "Winter")
private val DRINKS = listOf("Wein", "Rum", "Champagner", "Schnaps", "Likör", "Cognac", "Whisky", "Wodka", "Sekt")
private val PRECIPITATIONS = listOf("Regen", "Schnee", "Hagel", "Sturm", "Nebel")
private val WEEKDAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
private val COMPASS = listOf("Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten", "Westen", "Nordwesten")
private val CAR_BRANDS = listOf("Mercedes", "Volkswagen", "Ferrari", "Opel", "BMW", "Audi", "Porsche")
private val VERB_NOUNS = listOf("Essen", "Lesen", "Schwimmen", "Leben", "Schreiben", "Trinken")
private val COLORS = listOf("Rot", "Blau", "Grün", "Gelb", "Schwarz", "Weiß")

val RULE_GROUPS: List<RuleGroup> = listOf(
    RuleGroup(
        article = Article.der,
        label = "der — masculine",
        summary = "Time words, weather, drinks, directions, car brands and key endings.",
        rules = listOf(
            Rule(
                id = "der-months", article = Article.der, title = "Months", strength = "always",
                description = "Months are always masculine.",
                examples = listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli"),
                members = MONTHS,
            ),
            Rule(
                id = "der-seasons", article = Article.der, title = "Seasons", strength = "always",
                description = "Seasons are always masculine.",
                examples = SEASONS, members = SEASONS,
            ),
            Rule(
                id = "der-drinks", article = Article.der, title = "Drinks", strength = "usually",
                description = "Except for beer, alcoholic drinks are always masculine.",
                examples = listOf("Wein", "Rum", "Champagner", "Schnaps", "Liquor", "Cognac"),
                members = DRINKS,
                exception = Pair(Article.das, "Bier"),
            ),
            Rule(
                id = "der-precipitations", article = Article.der, title = "Precipitations", strength = "always",
                description = "Precipitations are always masculine.",
                examples = listOf("Regen", "Schnee", "Hagel", "Sturm"),
                members = PRECIPITATIONS,
            ),
            Rule(
                id = "der-weekdays", article = Article.der, title = "Weekdays", strength = "always",
                description = "Weekdays are always masculine.",
                examples = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"),
                members = WEEKDAYS,
            ),
            Rule(
                id = "der-compass", article = Article.der, title = "Compass", strength = "always",
                description = "Points of the compass are always masculine.",
                examples = listOf("Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten"),
                members = COMPASS,
            ),
            Rule(
                id = "der-car-brands", article = Article.der, title = "Car brands", strength = "always",
                description = "Car brands are always masculine.",
                examples = listOf("Mercedes", "Volkswagen", "Ferrari", "Opel", "BMW", "Audi"),
                members = CAR_BRANDS,
            ),
            Rule(
                id = "der-endings-always", article = Article.der, title = "Endings", strength = "always",
                description = "Nouns with these endings are always masculine.",
                patterns = listOf(
                    ending("ismus", "Journalismus"),
                    ending("ner", "Rechner"),
                    ending("or", "Mentor"),
                ),
            ),
            Rule(
                id = "der-endings-usually", article = Article.der, title = "Endings", strength = "usually",
                description = "Nouns with these endings are usually masculine.",
                patterns = listOf(
                    ending("ig", "Honig"),
                    ending("ich", "Pfirsich"),
                    ending("ling", "Schmetterling"),
                    ending("ist", "Polizist"),
                    ending("ant", "Elefant"),
                    ending("ent", "Agent"),
                    ending("är", "Bär"),
                    ending("eur", "Ingenieur"),
                ),
            ),
        ),
    ),
    RuleGroup(
        article = Article.die,
        label = "die — feminine",
        summary = "Abstract endings like -heit, -ung, -tät and most nouns ending in -e.",
        rules = listOf(
            Rule(
                id = "die-endings-always", article = Article.die, title = "Endings", strength = "always",
                description = "Nouns with these endings are always feminine.",
                patterns = listOf(
                    ending("heit", "Gesundheit"),
                    ending("keit", "Süßigkeit"),
                    ending("schaft", "Wissenschaft"),
                    ending("ung", "Bildung"),
                    ending("tät", "Qualität"),
                    ending("ion", "Information"),
                    ending("ie", "Galaxie"),
                    ending("ik", "Mathematik"),
                    ending("enz", "Konferenz"),
                    ending("anz", "Distanz"),
                ),
            ),
            Rule(
                id = "die-plural", article = Article.die, title = "Plural nouns", strength = "always",
                description = "Every noun in the plural takes die — no matter its singular gender.",
                examples = listOf("Leute", "Eltern", "Ferien", "Kinder"),
            ),
            Rule(
                id = "die-endings-usually", article = Article.die, title = "Endings", strength = "usually",
                description = "Nouns with these endings are usually feminine.",
                patterns = listOf(
                    ending("in", "Lehrerin"),
                    ending("e", "Stunde"),
                    ending("ei", "Polizei"),
                    ending("ur", "Tastatur"),
                ),
            ),
        ),
    ),
    RuleGroup(
        article = Article.das,
        label = "das — neuter",
        summary = "Verbs as nouns, colors, diminutives and many Latin-style endings.",
        rules = listOf(
            Rule(
                id = "das-categories", article = Article.das, title = "Categories", strength = "always",
                description = "Verbs used as nouns and colors are always neuter.",
                patterns = listOf(
                    RulePattern("Verbs as nouns", "Essen, Lesen") { _, word -> VERB_NOUNS.contains(word) },
                    RulePattern("Colors", "Rot, Blau") { _, word -> COLORS.contains(word) },
                ),
            ),
            Rule(
                id = "das-endings-always", article = Article.das, title = "Endings", strength = "always",
                description = "Diminutives are always neuter — even for people.",
                patterns = listOf(ending("chen", "Mädchen"), ending("lein", "Büchlein")),
            ),
            Rule(
                id = "das-endings-usually", article = Article.das, title = "Endings", strength = "usually",
                description = "Nouns with these endings or prefix are usually neuter.",
                patterns = listOf(
                    ending("ial", "Material"),
                    ending("nis", "Erlebnis"),
                    ending("um", "Album"),
                    ending("ment", "Dokument"),
                    ending("o", "Auto"),
                    RulePattern("Ge- prefix", "Geschenk") { lower, _ ->
                        lower.startsWith("ge") && lower.length > 4
                    },
                ),
            ),
        ),
    ),
)

private val ALL_RULES: List<Rule> = RULE_GROUPS.flatMap { it.rules }

private fun ruleById(id: String): Rule? = ALL_RULES.firstOrNull { it.id == id }

private val MATCH_ORDER = listOf(
    "das-categories",
    "das-endings-always",
    "die-endings-always",
    "der-endings-always",
    "das-endings-usually",
    "der-endings-usually",
    "die-endings-usually",
)

private fun genderName(article: Article): String = when (article) {
    Article.der -> "masculine"
    Article.die -> "feminine"
    Article.das -> "neuter"
}

enum class MatchKind { MEMBER, PATTERN, EXCEPTION, COUNTER }

class RuleMatch(
    val rule: Rule,
    val pattern: String?,
    /** EXCEPTION = listed exception; COUNTER = word breaks a suffix rule of another article. */
    val kind: MatchKind,
)

fun getRuleMatch(word: Word): RuleMatch? {
    if (word.plural) {
        val rule = ruleById("die-plural")
        if (rule != null) return RuleMatch(rule, null, MatchKind.MEMBER)
    }

    for (rule in ALL_RULES) {
        val ex = rule.exception
        if (ex != null && ex.second == word.word && word.articles.contains(ex.first)) {
            return RuleMatch(rule, null, MatchKind.EXCEPTION)
        }
        val members = rule.members
        if (members != null && word.articles.contains(rule.article) && members.contains(word.word)) {
            return RuleMatch(rule, null, MatchKind.MEMBER)
        }
    }

    val lower = word.word.lowercase()
    for (article in word.articles) {
        for (id in MATCH_ORDER) {
            val rule = ruleById(id) ?: continue
            if (rule.article != article) continue
            val pattern = rule.patterns?.firstOrNull { it.test(lower, word.word) }
            if (pattern != null) return RuleMatch(rule, pattern.label, MatchKind.PATTERN)
        }
    }

    // No rule supports the real article: point at the suffix rule this word breaks (e.g. der Käse vs. -e).
    for (id in MATCH_ORDER) {
        val rule = ruleById(id) ?: continue
        if (word.articles.contains(rule.article)) continue
        val pattern = rule.patterns?.firstOrNull { it.test(lower, word.word) }
        if (pattern != null) return RuleMatch(rule, pattern.label, MatchKind.COUNTER)
    }
    return null
}

fun getRuleTarget(word: Word): RuleTarget {
    val match = getRuleMatch(word)
    return if (match != null) {
        RuleTarget(match.rule.article, match.rule.id, match.pattern)
    } else {
        RuleTarget(word.articles.first())
    }
}

fun describeMatch(word: Word, match: RuleMatch?): String {
    val full = "${word.articleLabel} ${word.word}"
    if (match == null) {
        return "No fixed rule for ${word.word} — learn it as \"$full\"."
    }
    if (match.kind == MatchKind.EXCEPTION) {
        return "Exception to the ${match.rule.title.lowercase()} rule: it is $full."
    }
    val strength = match.rule.strength
    val pattern = match.pattern
    if (match.kind == MatchKind.COUNTER && pattern != null) {
        return "Watch out: $pattern nouns are $strength ${genderName(match.rule.article)} (${match.rule.article.name}), but this one is an exception."
    }
    if (pattern != null) {
        return "$pattern nouns are $strength ${genderName(match.rule.article)}."
    }
    return match.rule.description
}
