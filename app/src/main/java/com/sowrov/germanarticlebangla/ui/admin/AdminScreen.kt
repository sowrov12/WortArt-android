package com.sowrov.germanarticlebangla.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sowrov.germanarticlebangla.auth.AdminRepository
import com.sowrov.germanarticlebangla.auth.FoundUser
import com.sowrov.germanarticlebangla.data.PLANS
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.RoundIconButton
import com.sowrov.germanarticlebangla.ui.premium.formatDate
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

private const val DAY_MS = 24L * 60L * 60L * 1000L

@Composable
fun AdminScreen(onBack: () -> Unit) {
    val c = WortTheme.colors
    val scope = rememberCoroutineScope()
    var term by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<FoundUser?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf("") }

    fun search() {
        val query = term.trim()
        if (query.isEmpty() || busy) return
        busy = true
        message = null
        scope.launch {
            try {
                val user = AdminRepository.findUser(query)
                found = user
                if (user == null) message = "No user found."
            } catch (e: Exception) {
                found = null
                message = "Search failed: ${e.message ?: "unknown error"}"
            } finally {
                busy = false
            }
        }
    }

    fun write(user: FoundUser, untilMs: Long?, done: String) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try {
                AdminRepository.writePremium(user.uid, untilMs)
                found = user.copy(premiumUntilMs = untilMs)
                message = done
            } catch (e: Exception) {
                message = "Could not save: ${e.message ?: "unknown error"}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundIconButton(AppIcons.Back, "Back", c.foreground, c.card, onBack)
            Text("Admin", color = c.foreground, fontFamily = SerifFont, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("Find a user by User ID or email, then set their Premium.", color = c.mutedForeground, fontSize = 14.sp)

        TextField(
            value = term,
            onValueChange = { term = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("User ID or email", color = c.mutedForeground) },
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
        Button(
            onClick = { search() },
            enabled = !busy && term.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
        ) { Text(if (busy) "Working…" else "Find user") }

        val msg = message
        if (msg != null) Text(msg, color = c.mutedForeground, fontSize = 13.sp)

        val user = found
        if (user != null) {
            val now = System.currentTimeMillis()
            val until = user.premiumUntilMs
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(user.name.ifEmpty { "(no name)" }, color = c.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(user.email, color = c.mutedForeground, fontSize = 13.sp)
                Text("ID: ${user.uid}", color = c.mutedForeground, fontSize = 11.sp)
                if (until != null && until > now) {
                    Text("Premium until ${formatDate(until)}", color = c.success, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                } else if (until != null) {
                    Text("Premium expired on ${formatDate(until)}", color = c.destructive, fontSize = 14.sp)
                } else {
                    Text("Free plan", color = c.mutedForeground, fontSize = 14.sp)
                }

                Text("Add time", color = c.foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
                for (plan in PLANS) {
                    Button(
                        onClick = {
                            // Extends from the current end date when premium is still active.
                            val base = if (until != null && until > now) until else now
                            write(user, base + plan.days * DAY_MS, "Added ${plan.label}.")
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
                    ) { Text("+ ${plan.label}") }
                }

                TextField(
                    value = customDate,
                    onValueChange = { customDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Custom end date (YYYY-MM-DD)", color = c.mutedForeground) },
                    shape = RoundedCornerShape(14.dp),
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
                OutlinedButton(
                    onClick = {
                        val parsed = try {
                            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                            format.isLenient = false
                            format.parse(customDate.trim() + " 23:59:59")?.time
                        } catch (e: Exception) {
                            null
                        }
                        if (parsed == null) {
                            message = "Use the format YYYY-MM-DD, for example 2026-12-31."
                        } else {
                            write(user, parsed, "Premium set until ${formatDate(parsed)}.")
                        }
                    },
                    enabled = !busy && customDate.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.border),
                ) { Text("Set end date", color = c.foreground) }

                OutlinedButton(
                    onClick = { write(user, null, "Premium removed.") },
                    enabled = !busy && until != null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.destructive.copy(alpha = 0.5f)),
                ) { Text("Remove Premium", color = c.destructive) }
            }
        }
    }
}
