package com.error404.neunest

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberSystemStats(): State<SystemStats> {
    val context = LocalContext.current
    return produceState(
        initialValue = SystemStats(),
        key1 = context
    ) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        while (true) {
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)

            val memory = Pair(
                mi.availMem / (1024 * 1024),
                mi.totalMem / (1024 * 1024)
            )

            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            val temperature = temp / 10f

            val voltage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

            value = SystemStats(
                memory = memory,
                temperature = temperature,
                voltage = voltage
            )

            kotlinx.coroutines.delay(1500)
        }
    }
}

data class SystemStats(
    val memory: Pair<Long, Long> = 0L to 0L,
    val temperature: Float = 0f,
    val voltage: Int = 0
)

class Stats(
    val context: Context
) {
    fun memory(): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        return Pair(
            mi.availMem / (1024 * 1024),
            mi.totalMem / (1024 * 1024)
        )
    }

    fun batteryTemperature(): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return temp / 10f
    }

    fun batteryVoltage(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    }
}