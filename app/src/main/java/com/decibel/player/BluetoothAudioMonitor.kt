package com.decibel.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Watches output devices and reports when media is likely going over Bluetooth
 * (A2DP buds/speakers or BLE audio), so the wave visualizer can add codec delay.
 */
class BluetoothAudioMonitor(
    context: Context,
    private val onBluetoothOutputChanged: (Boolean) -> Unit,
) {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var registered = false
    private var lastBluetooth = false

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refresh()
        }
    }

    fun start() {
        if (registered) return
        audioManager.registerAudioDeviceCallback(callback, mainHandler)
        registered = true
        refresh()
    }

    fun stop() {
        if (!registered) return
        runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
        registered = false
    }

    private fun refresh() {
        val bluetooth = hasBluetoothOutput()
        if (bluetooth == lastBluetooth) return
        lastBluetooth = bluetooth
        onBluetoothOutputChanged(bluetooth)
    }

    private fun hasBluetoothOutput(): Boolean {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { device -> isBluetoothOutput(device) }
    }

    private fun isBluetoothOutput(device: AudioDeviceInfo): Boolean {
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            -> true
            else -> {
                if (Build.VERSION.SDK_INT >= 31) {
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                } else {
                    false
                }
            }
        }
    }
}
