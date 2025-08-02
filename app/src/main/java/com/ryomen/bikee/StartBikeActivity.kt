package com.ryomen.bikee

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class StartBikeActivity : Activity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("bikee_prefs", MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        if (lastMac != null && bluetoothAdapter != null) {
            val device = bluetoothAdapter.getRemoteDevice(lastMac)
            connectToDeviceAndStart(device)
        } else {
            Toast.makeText(this, "No paired device or Bluetooth not supported 💔", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun connectToDeviceAndStart(device: BluetoothDevice) {
        val serviceIntent = Intent(this, BluetoothService::class.java)
        serviceIntent.putExtra("cmd", "START_ENGINE")
        startService(serviceIntent)

        Toast.makeText(this, "Sending start command 🛵", Toast.LENGTH_SHORT).show()
        finish()
    }
}