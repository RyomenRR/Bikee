package com.ryomen.bikee

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

class VoiceShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BluetoothService.isConnected && BluetoothService.instance != null) {
            BluetoothService.instance?.sendCommand("s")
            Toast.makeText(this, "Started Reyona from shortcut 💖", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Reyona not connected 😢", Toast.LENGTH_LONG).show()
        }

        finish()
    }
}