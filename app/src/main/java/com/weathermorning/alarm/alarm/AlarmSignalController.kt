package com.weathermorning.alarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.weathermorning.alarm.domain.model.AlarmSettings

class AlarmSignalController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun start(settings: AlarmSettings) {
        if (settings.alarmSoundEnabled) {
            startSound(settings)
        }
        if (settings.vibrationEnabled) {
            startVibration()
        }
    }

    fun stop() {
        stopSound()
        stopVibration()
    }

    private fun startSound(settings: AlarmSettings) {
        if (mediaPlayer != null) return
        val alarmUri = settings.alarmSoundUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        mediaPlayer = createPlayer(alarmUri)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let {
                createPlayer(it)
            }
    }

    private fun createPlayer(uri: Uri): MediaPlayer? {
        return runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun stopSound() {
        mediaPlayer?.runCatchingStop()
        mediaPlayer = null
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 700, 500, 700, 1_000)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, 0),
        )
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun MediaPlayer.runCatchingStop() {
        runCatching {
            if (isPlaying) stop()
        }
        runCatching { release() }
    }
}
