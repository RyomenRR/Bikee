package com.ryomen.bikee

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import java.io.OutputStream
import java.util.*

class BluetoothService : Service() {

    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = intent?.getStringExtra("cmd") ?: return START_NOT_STICKY
        val prefs = getSharedPreferences("bikee_prefs", MODE_PRIVATE)
        val mac = prefs.getString("last_mac", null)

        if (mac != null) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device: BluetoothDevice = adapter.getRemoteDevice(mac)

            Thread {
                try {
                    val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
                    socket.connect()
                    val out: OutputStream = socket.outputStream
                    out.write((command + "\n").toByteArray())
                    out.flush()
                    socket.close()
                } catch (_: Exception) {}
                stopSelf()
            }.start()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}