package com.ryomen.bikee

import android.content.Context
import android.content.Intent
import android.util.Log

object MyBluetoothService {

    fun sendCommand(context: Context, command: String) {
        Log.d("MyBluetoothService", "Sending command: $command")

        val btService = BluetoothService.instance

        if (btService != null && BluetoothService.isConnected) {
            btService.sendCommand(command)
        } else {
            Log.d("MyBluetoothService", "Bluetooth not connected. Attempting to reconnect...")

            // Try to reconnect using stored MAC (replace with your device MAC or load from prefs)
            val intent = Intent(context, BluetoothService::class.java)
            intent.putExtra("DEVICE_MAC", "00:48:12:33:A7:0A") // ← Replace with your Reyona MAC
            context.startService(intent)

            // Delay sending the command until connection completes
            Thread {
                Thread.sleep(2000) // Wait 2 seconds (Bluetooth usually connects fast)
                BluetoothService.instance?.sendCommand(command)
            }.start()
        }
    }
}