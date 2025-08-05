package com.ryomen.bikee

import com.ryomen.bikee.StartBikeReceiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class StartBikeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == "com.ryomen.bikee.START_BIKE") {
            Toast.makeText(context, "Starting bike...", Toast.LENGTH_SHORT).show()
            MyBluetoothService.sendCommand("START")
            Log.d("StartBikeReceiver", "Bike start command sent")
        }
    }
}