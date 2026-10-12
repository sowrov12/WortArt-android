package com.sowrov.germanarticlebangla.ui.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sowrov.germanarticlebangla.data.FREE_CATEGORIES_PER_DECK
import com.sowrov.germanarticlebangla.data.FREE_CUSTOM_DECKS
import com.sowrov.germanarticlebangla.data.DailyCounter
import com.sowrov.germanarticlebangla.data.PLANS
import com.sowrov.germanarticlebangla.data.freeLimit
import com.sowrov.germanarticlebangla.data.whatsappLink
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.AppViewModel
import com.sowrov.germanarticlebangla.ui.IconTile
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun formatDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))

@Composable
fun PremiumScreen(vm: AppViewModel) {
    val c = WortTheme.colors
    val auth by vm.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val user = auth.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text("Premium", color = c.foreground, fontFamily = SerifFont, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Unlimited learning, every category.", color = c.mutedForeground, fontSize = 14.sp)
        }

        // Benefits
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(AppIcons.Crown, c.primary, size = 48, round = true)
                Text("What you get", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Benefit("Unlimited practice (free: ${freeLimit(DailyCounter.CARDS_PRACTICED)} cards a day)")
            Benefit("Unlimited favorites (free: ${freeLimit(DailyCounter.FAVORITES_ADDED)} a day)")
            Benefit("Unlimited grammar rule look-ups (free: ${freeLimit(DailyCounter.RULES_OPENED)} a day)")
            Benefit("All Premium categories")
            Benefit("Unlimited multi-category decks (free: $FREE_CUSTOM_DECKS deck, $FREE_CATEGORIES_PER_DECK categories)")
        }

        when {
            !auth.configured -> {
                Text(
                    "Login is not set up in this build yet. Add google-services.json to the app folder (see README) to enable Google sign-in and Premium.",
                    color = c.mutedForeground, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.card).padding(16.dp),
                )
            }
            auth.loading -> {
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = c.primary)
                }
            }
            user == null -> {
                Button(
                    onClick = { context.findActivity()?.let { vm.signIn(it) } },
                    enabled = !vm.signInPending,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
                ) { Text(if (vm.signInPending) "Signing in…" else "Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                val error = vm.signInError
                if (error != null) Text(error, color = c.destructive, fontSize = 13.sp)
                Text("Sign in to buy Premium and keep it on your account.", color = c.mutedForeground, fontSize = 12.sp)
            }
            else -> {
                // Account
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(user.name, color = c.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(user.email, color = c.mutedForeground, fontSize = 13.sp)
                    val until = auth.premiumUntilMs
                    if (auth.isPremium && until != null) {
                        Text("Premium until ${formatDate(until)}", color = c.success, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    } else if (until != null) {
                        Text("Premium expired on ${formatDate(until)}", color = c.destructive, fontSize = 14.sp)
                    } else {
                        Text("Free plan", color = c.mutedForeground, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.secondary)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("ID: ${user.uid}", color = c.mutedForeground, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Icon(
                            AppIcons.Copy, "Copy user ID", tint = c.foreground,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { clipboard.setText(AnnotatedString(user.uid)) },
                        )
                    }
                }

                Text("Choose a plan", color = c.foreground, fontFamily = SerifFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                for (plan in PLANS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(c.card)
                            .border(1.dp, if (plan.badge != null) c.primary.copy(alpha = 0.5f) else c.border, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(plan.label, color = c.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                if (plan.badge != null) {
                                    Text(
                                        plan.badge, color = c.primaryForeground, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(CircleShape).background(c.primary).padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                            Text("${plan.price}৳", color = c.primary, fontFamily = SerifFont, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { uriHandler.openUri(whatsappLink(plan, user.uid, user.email)) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
                        ) { Text("Buy on WhatsApp") }
                    }
                }
                Text(
                    "After you pay, send your User ID on WhatsApp. Premium is switched on for your account within a short time.",
                    color = c.mutedForeground, fontSize = 12.sp,
                )

                if (auth.isAdmin) {
                    OutlinedButton(
                        onClick = { vm.openAdmin() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, c.border),
                    ) {
                        Icon(AppIcons.Admin, null, tint = c.primary, modifier = Modifier.size(18.dp))
                        Text("Admin panel", color = c.foreground, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedButton(
                    onClick = { vm.signOut() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, c.border),
                ) {
                    Icon(AppIcons.Logout, null, tint = c.mutedForeground, modifier = Modifier.size(18.dp))
                    Text("Sign out", color = c.foreground, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    val c = WortTheme.colors
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(AppIcons.Check, null, tint = c.success, modifier = Modifier.padding(top = 2.dp).size(18.dp))
        Text(text, color = c.foreground, fontSize = 14.sp)
    }
}
