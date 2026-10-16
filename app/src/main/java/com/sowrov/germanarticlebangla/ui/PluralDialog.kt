package com.sowrov.germanarticlebangla.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sowrov.germanarticlebangla.data.Word
import com.sowrov.germanarticlebangla.data.hasNoPlural
import com.sowrov.germanarticlebangla.data.pluralMissing
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

/** Info popup for a word: "die" + plural form, with its English and Bengali meaning. */
@Composable
fun PluralDialog(word: Word, onDismiss: () -> Unit) {
    val c = WortTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            RoundIconButton(
                AppIcons.Close, "Close", c.foreground, c.secondary, onDismiss,
                Modifier.align(Alignment.TopEnd), 36,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Plural of ${word.word}",
                    color = c.mutedForeground, fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
                when {
                    word.hasNoPlural() -> Text(
                        "There is no plural form",
                        color = c.foreground, fontFamily = SerifFont, fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    word.pluralMissing() -> Text(
                        "Plural form not added yet",
                        color = c.mutedForeground, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    else -> {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = c.die)) { append("die ") }
                                append(word.pluralForm)
                            },
                            color = c.foreground, fontFamily = SerifFont, fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        )
                        if (word.pluralEn.isNotEmpty()) {
                            Text(word.pluralEn, color = c.mutedForeground, fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                        if (word.pluralBn.isNotEmpty()) {
                            Text(word.pluralBn, color = c.foreground, fontSize = 18.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
