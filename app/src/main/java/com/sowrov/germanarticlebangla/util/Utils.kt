package com.sowrov.germanarticlebangla.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sowrov.germanarticlebangla.MainActivity
import com.sowrov.germanarticlebangla.R
import com.sowrov.germanarticlebangla.data.PREFS_NAME
import com.sowrov.germanarticlebangla.data.ProgressStore
import com.sowrov.germanarticlebangla.data.dayKey
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------------------------
// Text to speech (German)
// ---------------------------------------------------------------------------------------------

class Speaker(context: Context) {
    @Volatile
    private var ready = false
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val tts = engine
            if (status == TextToSpeech.SUCCESS && tts != null) {
                val result = tts.setLanguage(Locale.GERMANY)
                tts.setSpeechRate(0.9f)
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wortart-utterance")
    }

    fun stop() {
        try {
            engine?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        engine = null
    }
}

// ---------------------------------------------------------------------------------------------
// Sounds: the same two chimes as the website, synthesized (no audio files needed)
// ---------------------------------------------------------------------------------------------

object SoundFx {
    private const val RATE = 22050

    private class Tone(
        val freq: Double,
        val endFreq: Double?,
        val start: Double,
        val dur: Double,
        val triangle: Boolean,
        val vol: Double,
    )

    private val correctPcm: ShortArray by lazy {
        render(
            listOf(
                Tone(659.25, null, 0.0, 0.35, false, 0.18),
                Tone(987.77, null, 0.09, 0.55, false, 0.18),
                Tone(1975.5, null, 0.09, 0.40, false, 0.04),
            )
        )
    }

    private val wrongPcm: ShortArray by lazy {
        render(
            listOf(
                Tone(260.0, 160.0, 0.0, 0.32, true, 0.22),
                Tone(196.0, 130.0, 0.12, 0.30, true, 0.14),
            )
        )
    }

    private fun render(tones: List<Tone>): ShortArray {
        var total = 0.0
        for (t in tones) total = maxOf(total, t.start + t.dur)
        val n = ((total + 0.05) * RATE).toInt()
        val mix = DoubleArray(n)
        for (t in tones) {
            val offset = (t.start * RATE).toInt()
            val len = (t.dur * RATE).toInt()
            var phase = 0.0
            for (i in 0 until len) {
                val idx = offset + i
                if (idx >= n) break
                val progress = i.toDouble() / len
                val freq = if (t.endFreq != null) t.freq * Math.pow(t.endFreq / t.freq, progress) else t.freq
                phase += 2.0 * Math.PI * freq / RATE
                val wave = if (t.triangle) (2.0 / Math.PI) * Math.asin(Math.sin(phase)) else Math.sin(phase)
                val time = i.toDouble() / RATE
                val envelope = if (time < 0.015) {
                    time / 0.015
                } else {
                    Math.exp(-5.0 * (time - 0.015) / (t.dur - 0.015))
                }
                mix[idx] += wave * envelope * t.vol
            }
        }
        val out = ShortArray(n)
        for (i in 0 until n) {
            val v = mix[i].coerceIn(-1.0, 1.0)
            out[i] = (v * 32767.0).toInt().toShort()
        }
        return out
    }

    fun play(correct: Boolean) {
        val pcm = if (correct) correctPcm else wrongPcm
        Thread {
            var track: AudioTrack? = null
            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep((pcm.size * 1000L / RATE) + 150L)
            } catch (e: Exception) {
                // sounds are optional
            } finally {
                try {
                    track?.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }.start()
    }
}

// ---------------------------------------------------------------------------------------------
// Daily reminder: after 18:00, if you have not practiced today
// ---------------------------------------------------------------------------------------------

object Reminders {
    const val CHANNEL_ID = "daily_reminder"
    private const val WORK_NAME = "daily_reminder_check"
    private const val REMINDER_HOUR = 18
    private const val KEY_LAST_REMINDER_DAY = "last_reminder_day"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }

    internal fun checkAndNotify(context: Context) {
        val progress = ProgressStore.readSnapshot(context)
        if (!progress.settings.reminders) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) < REMINDER_HOUR) return
        val today = dayKey(now)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_REMINDER_DAY, null) == today) return
        if (progress.stats.lastPlayedDay == today) return

        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle("Zeit zum Üben!")
            .setContentText("Keep your der/die/das streak alive — a few cards take one minute.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
            prefs.edit().putString(KEY_LAST_REMINDER_DAY, today).apply()
        } catch (e: SecurityException) {
            // permission revoked in the meantime
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Reminders.checkAndNotify(applicationContext)
        return Result.success()
    }
}
