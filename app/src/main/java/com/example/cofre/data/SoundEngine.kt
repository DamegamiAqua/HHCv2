package com.example.cofre.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Motor de efectos corto; los recursos raw son intercambiables sin tocar la lógica de la app. */
enum class SoundEvent(val resourceName: String) {
    BUTTON("sfx_button"), NAVIGATION("sfx_navigation"), CONFIRM("sfx_confirm"), COIN("sfx_coin"), CONTRIBUTION("sfx_contribution"), TRANSFER("sfx_transfer"), WITHDRAWAL("sfx_withdrawal"), REWARD("sfx_reward")
}

class SoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    @Volatile var enabled: Boolean = true
    @Volatile var volume: Float = 0.75f
    private val loaded = Collections.synchronizedSet(mutableSetOf<Int>())
    private val released = AtomicBoolean(false)
    private val ids: Map<SoundEvent, Int?>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
        ids = SoundEvent.entries.associateWith { event ->
            appContext.resources.getIdentifier(event.resourceName, "raw", appContext.packageName)
                .takeIf { it != 0 }
                ?.let { resId -> pool.load(appContext, resId, 1) }
        }
    }

    fun play(event: SoundEvent) {
        if (!enabled || released.get()) return
        ids[event]?.takeIf { it in loaded }?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun release() { if (released.compareAndSet(false, true)) pool.release() }
}
