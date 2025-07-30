package com.ryomen.bikee

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class StartBikeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        if (lastMac != null) {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val device: BluetoothDevice? = btAdapter?.getRemoteDevice(lastMac)

            if (device != null) {
                if (!BluetoothService.isConnected()) {
                    // Connect to ESP32 first
                    BluetoothService.connectToDevice(device, this) {
                        if (it) {
                            sendCommands()
                        } else {
                            Toast.makeText(this, "Failed to connect 💔", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    // Already connected
                    sendCommands()
                }
            } else {
                Toast.makeText(this, "Device not found 🥺", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "No paired device 💔", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun sendCommands() {
        val serviceIntent = Intent(this, BluetoothService::class.java)
        serviceIntent.putExtra("cmd", "IGNITION_ON")
        startService(serviceIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            val startIntent = Intent(this, BluetoothService::class.java)
            startIntent.putExtra("cmd", "START_ENGINE")
            startService(startIntent)
            finish()
        }, 1500)
    }
}