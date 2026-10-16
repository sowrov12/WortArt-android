package com.sowrov.germanarticlebangla.ui.practice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sowrov.germanarticlebangla.data.Word
import com.sowrov.germanarticlebangla.data.matchesSpoken
import com.sowrov.germanarticlebangla.ui.AppIcons
import com.sowrov.germanarticlebangla.ui.RoundIconButton
import com.sowrov.germanarticlebangla.ui.theme.SerifFont
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

private enum class VoiceStatus { Starting, Listening, Wrong, Problem }

private fun describeError(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Try again."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Check your internet connection and try again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The microphone is busy. Try again."
    else -> "Something went wrong. Try again."
}

/**
 * Pronunciation popup: say the word together with its article.
 * [onResult] is called after every recognized attempt (true = perfect). The popup stays open after a wrong
 * attempt (Try again); the X button is always there.
 */
@Composable
fun PronunciationDialog(
    word: Word,
    onResult: (Boolean) -> Unit,
    canListen: () -> Boolean,
    onDismiss: () -> Unit,
) {
    val c = WortTheme.colors
    val context = LocalContext.current
    var status by remember { mutableStateOf(VoiceStatus.Starting) }
    var heard by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    // One recognized result per attempt: a repeated callback must never trigger a second chime.
    var delivered by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current.density
    val latestOnResult by rememberUpdatedState(onResult)

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                status = VoiceStatus.Listening
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (error == SpeechRecognizer.ERROR_CLIENT) return
                problem = describeError(error)
                status = VoiceStatus.Problem
            }

            override fun onResults(results: Bundle?) {
                if (delivered) return
                delivered = true
                val list: List<String> = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                heard = list.firstOrNull().orEmpty()
                val perfect = list.any { matchesSpoken(it, word) }
                if (!perfect) status = VoiceStatus.Wrong
                latestOnResult(perfect)
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    fun startListening() {
        // Free daily limit: when it is used up the limit message is shown and the popup closes.
        if (!canListen()) {
            onDismiss()
            return
        }
        val r = recognizer
        if (r == null) {
            problem = "Speech recognition isn't available on this phone."
            status = VoiceStatus.Problem
            return
        }
        heard = ""
        delivered = false
        status = VoiceStatus.Starting
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "de-DE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            r.cancel()
            r.startListening(intent)
        } catch (e: Exception) {
            problem = "Could not start the microphone."
            status = VoiceStatus.Problem
        }
    }

    LaunchedEffect(Unit) { startListening() }

    LaunchedEffect(status) {
        if (status == VoiceStatus.Wrong) {
            for (d in listOf(-14f, 14f, -9f, 9f, 0f)) shake.animateTo(d, tween(60))
        }
    }

    val pulse = rememberInfiniteTransition(label = "voice-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "voice-pulse-scale",
    )
    val micScale = if (status == VoiceStatus.Listening) pulseScale else 1f

    val statusText = when (status) {
        VoiceStatus.Starting -> "Getting ready…"
        VoiceStatus.Listening -> "Listening… say it now"
        VoiceStatus.Wrong -> "Not quite. Try again!"
        VoiceStatus.Problem -> problem
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer { translationX = shake.value * density }
                .clip(RoundedCornerShape(28.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            RoundIconButton(
                AppIcons.Close, "Cancel", c.foreground, c.secondary, onDismiss,
                Modifier.align(Alignment.TopEnd), 36,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Say the word with its article",
                    color = c.mutedForeground, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
                Text(
                    "___  ${word.word}",
                    color = c.foreground, fontFamily = SerifFont, fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(micScale)
                        .clip(CircleShape)
                        .background(if (status == VoiceStatus.Listening) c.primary else c.secondary)
                        .clickable(enabled = status == VoiceStatus.Wrong || status == VoiceStatus.Problem) { startListening() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.Mic, contentDescription = "Microphone",
                        tint = if (status == VoiceStatus.Listening) c.primaryForeground else c.primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Text(
                    statusText,
                    color = if (status == VoiceStatus.Wrong) c.destructive else c.mutedForeground,
                    fontSize = 14.sp, textAlign = TextAlign.Center,
                )
                if (status == VoiceStatus.Wrong && heard.isNotEmpty()) {
                    Text("I heard: “$heard”", color = c.mutedForeground, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                if (status == VoiceStatus.Wrong || status == VoiceStatus.Problem) {
                    Button(
                        onClick = { startListening() },
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.primaryForeground),
                    ) { Text("Try again") }
                }
            }
        }
    }
}
