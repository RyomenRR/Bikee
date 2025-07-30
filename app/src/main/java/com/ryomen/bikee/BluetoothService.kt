package com.ryomen.bikee

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.OutputStream
import java.util.UUID

class BluetoothService : Service() {

    private var outputStream: OutputStream? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("cmd")

        val deviceAddress = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
            .getString("last_mac", null)

        if (deviceAddress != null) {
            val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)

            if (!isConnected()) {
                connectToDevice(device, this) { success ->
                    if (success && cmd != null) {
                        sendCommand(cmd)
                    } else {
                        Log.e("BluetoothService", "Connection failed or command is null")
                    }
                    stopSelf()
                }
            } else {
                if (cmd != null) sendCommand(cmd)
                stopSelf()
            }
        } else {
            Log.e("BluetoothService", "No saved device address")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun sendCommand(command: String) {
        try {
            outputStream = socket?.outputStream
            outputStream?.write((command + "\n").toByteArray())
            Log.d("BluetoothService", "Command sent: $command")
        } catch (e: Exception) {
            Log.e("BluetoothService", "Error sending command", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private var socket: BluetoothSocket? = null
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        fun isConnected(): Boolean {
            return socket?.isConnected == true
        }

        fun connectToDevice(
            device: BluetoothDevice,
            context: Context,
            callback: (Boolean) -> Unit
        ) {
            Thread {
                try {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                    BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                    socket?.connect()
                    Log.d("BluetoothService", "Connected to device")
                    callback(true)
                } catch (e: Exception) {
                    Log.e("BluetoothService", "Failed to connect", e)
                    socket = null
                    callback(false)
                }
            }.start()
        }
    }
}