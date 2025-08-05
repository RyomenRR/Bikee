package com.ryomen.bikee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.util.Log

class StartBikeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Toast.makeText(context, "🚀 Voice Start Triggered", Toast.LENGTH_SHORT).show()

        // 1. Start the BluetoothService with MAC
        val serviceIntent = Intent(context, BluetoothService::class.java)
        serviceIntent.putExtra("DEVICE_MAC", "00:48:12:33:A7:0A") // replace with Reyona's MAC
        context.startService(serviceIntent)

        // 2. Wait until it's connected, then send START
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if (BluetoothService.isConnected) {
                    BluetoothService.instance?.sendCommand("START")
                    Log.d("StartBikeReceiver", "✅ START command sent")
                } else {
                    Log.d("StartBikeReceiver", "⏳ Not connected yet, retrying...")
                    Handler(Looper.getMainLooper()).postDelayed(this, 500) // try again after 0.5s
                }
            }
        }, 1000) // initial delay
    }
}