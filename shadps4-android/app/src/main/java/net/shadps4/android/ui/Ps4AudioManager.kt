package net.shadps4.android.ui

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.delay

/**
 * Plays authentic PS4 UI sound effects (navigate, startup, shutdown, trophy reward)
 * copied directly from d4v1-sudo/Ps4-UI assets/ps4ui/*.mp3.
 */
object Ps4AudioManager {

    fun playSound(context: Context, assetFileName: String) {
        try {
            val afd = context.assets.openFd("ps4ui/$assetFileName")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { player ->
                try {
                    player.release()
                } catch (_: Throwable) {}
            }
            mp.prepare()
            mp.start()
        } catch (_: Throwable) {}
    }

    fun playNavigate(context: Context) = playSound(context, "navigate.mp3")
    fun playStartup(context: Context) = playSound(context, "startup.mp3")
    fun playShutdown(context: Context) = playSound(context, "shutdown.mp3")
    fun playReward(context: Context) = playSound(context, "reward.mp3")
}
