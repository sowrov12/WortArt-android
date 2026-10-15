package com.sowrov.germanarticlebangla.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sowrov.germanarticlebangla.data.Article
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

private class CaseHeader(val name: String, val hint: String)

private class GenderRow(val label: String, val article: Article?, val forms: List<String>)

private val CASE_HEADERS = listOf(
    CaseHeader("Nominativ", "(subject)"),
    CaseHeader("Akkusativ", "(direct object)"),
    CaseHeader("Dativ", "(indirect object)"),
)

/** forms = Nominativ def, indef, Akkusativ def, indef, Dativ def, indef */
private val GENDER_ROWS = listOf(
    GenderRow("Masculine", Article.der, listOf("der", "ein", "den", "einen", "dem", "einem")),
    GenderRow("Feminine", Article.die, listOf("die", "eine", "die", "eine", "der", "einer")),
    GenderRow("Neuter", Article.das, listOf("das", "ein", "das", "ein", "dem", "einem")),
    GenderRow("Plural", null, listOf("die", "—", "die", "—", "den", "—")),
)

@Composable
fun CasesTable() {
    val c = WortTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("The article table", color = c.foreground, fontFamily = SerifFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cell = (maxWidth / 7.5f).coerceAtLeast(46.dp)
            val genderWidth = cell * 1.5f
            val tableWidth = genderWidth + cell * 6

            Column(Modifier.horizontalScroll(rememberScrollState())) {
                // 1. Case header: German name with the English explanation underneath, in one container.
                Row {
                    Spacer(Modifier.width(genderWidth))
                    for (case in CASE_HEADERS) {
                        Box(Modifier.width(cell * 2).padding(horizontal = 2.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c.secondary)
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(case.name, color = c.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(case.hint, color = c.mutedForeground, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Column labels: Gender | Def. | Indef. x3
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(genderWidth).padding(vertical = 8.dp)) {
                        Text("Gender", color = c.mutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    for (index in 0 until 6) {
                        Box(Modifier.width(cell).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (index % 2 == 0) "Def." else "Indef.",
                                color = c.mutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // 3. Article data
                for (row in GENDER_ROWS) {
                    val tint: Color = if (row.article != null) c.article(row.article) else c.foreground
                    Box(Modifier.width(tableWidth).height(1.dp).background(c.border))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(genderWidth).padding(vertical = 10.dp)) {
                            Text(row.label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        for (form in row.forms) {
                            Box(Modifier.width(cell).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    form, color = tint, fontFamily = SerifFont, fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Def. = definite article (der, die, das) · Indef. = indefinite article (ein, eine)",
            color = c.mutedForeground, fontSize = 11.sp,
        )
    }
}
