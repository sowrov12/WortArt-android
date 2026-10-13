package com.sowrov.germanarticlebangla.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

/** Remote image (Coil). Shows [fallback] when there is no URL or the download fails. */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onSuccess: (() -> Unit)? = null,
    backupUrl: String? = null,
    fallback: @Composable () -> Unit = {},
) {
    // stage 0 = main url, 1 = backup url, 2 = give up and show the fallback
    var stage by remember(url, backupUrl) { mutableIntStateOf(0) }
    val target: String? = when (stage) {
        0 -> if (url.isNullOrEmpty()) backupUrl else url
        1 -> backupUrl
        else -> null
    }
    if (target.isNullOrEmpty()) {
        fallback()
        return
    }
    AsyncImage(
        model = target,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        onSuccess = { onSuccess?.invoke() },
        onError = { stage = if (stage == 0 && !backupUrl.isNullOrEmpty() && target != backupUrl) 1 else 2 },
    )
}

/** A number that slides up when it increases (like the website's rolling counter). */
@Composable
fun RollingNumber(
    value: Int,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
) {
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "rolling-number",
    ) { v ->
        Text(
            text = v.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = fontFamily,
        )
    }
}

/** Round tinted tile holding an icon (the golden icon tiles of the website). */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 40,
    round: Boolean = false,
    background: Color = tint.copy(alpha = 0.15f),
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(if (round) CircleShape else RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** Small round icon button used on cards. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

/** A settings row with a title, a description and a switch. */
@Composable
fun SwitchRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = WortTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, c.primary, size = 40)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = c.foreground, fontSize = 15.sp)
            Text(description, color = c.mutedForeground, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.background,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.mutedForeground,
                uncheckedTrackColor = c.secondary,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}

/** A tappable row: icon tile, label, optional trailing text and a chevron. */
@Composable
fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: String? = null,
    enabled: Boolean = true,
) {
    val c = WortTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, if (enabled) c.primary else c.mutedForeground, size = 40)
        Text(
            label,
            color = if (enabled) c.foreground else c.mutedForeground,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, color = c.mutedForeground, fontSize = 13.sp)
        }
        Icon(AppIcons.ChevronRight, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(18.dp))
    }
}
