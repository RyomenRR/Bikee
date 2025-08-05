package com.ryomen.bikee

import android.app.*
import android.bluetooth.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import java.io.OutputStream
import java.util.*

class BluetoothService : Service() {

    private val CHANNEL_ID = "bikee_channel"
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        var isConnected = false
        var instance: BluetoothService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(1, getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mac = intent?.getStringExtra("DEVICE_MAC")
        if (mac != null) {
            connectToDevice(mac)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        outputStream?.close()
        bluetoothSocket?.close()
        isConnected = false
        instance = null
        super.onDestroy()
    }

    fun sendCommand(command: String) {
        outputStream?.write(command.toByteArray())
    }

    fun connectAndSend(command: String, mac: String) {
    val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac)
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    Thread {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            isConnected = true

            // ✅ Only send after connection
            outputStream?.write(command.toByteArray())

        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
        }
    }.start()
}

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bikee")
            .setContentText("Connected to Reyona 💖")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Bikee Bluetooth Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}