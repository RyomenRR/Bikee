package com.ryomen.bikee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StartBikeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        if (lastMac != null) {
            Toast.makeText(context, "Sending start bike command 🛵", Toast.LENGTH_SHORT).show()
            val serviceIntent = Intent(context, BluetoothService::class.java)
            serviceIntent.putExtra("cmd", "START_ENGINE")
            context.startService(serviceIntent)
        } else {
            Toast.makeText(context, "No device paired 💔", Toast.LENGTH_SHORT).show()
        }
    }
}