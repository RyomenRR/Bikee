package com.ryomen.bikee

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.IOException
import java.util.*
import kotlin.concurrent.thread

class BluetoothService : Service() {

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("bikee_prefs", MODE_PRIVATE)
        val mac = prefs.getString("last_mac", null)
        val cmd = intent?.getStringExtra("cmd") ?: "START_ENGINE"

        if (mac != null) {
            val device = adapter?.getRemoteDevice(mac)
            connectAndSend(device, cmd)
        } else {
            Log.e("BluetoothService", "No MAC found in prefs")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun connectAndSend(device: BluetoothDevice?, command: String) {
        if (device == null) {
            Log.e("BluetoothService", "Device is null")
            stopSelf()
            return
        }

        thread {
            try {
                val uuid = device.uuids?.firstOrNull()?.uuid ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                socket = device.createRfcommSocketToServiceRecord(uuid)
                adapter?.cancelDiscovery()
                socket?.connect()

                Log.i("BluetoothService", "Connected to device")

                socket?.outputStream?.write((command + "\n").toByteArray())

                Log.i("BluetoothService", "Command sent: $command")
            } catch (e: IOException) {
                Log.e("BluetoothService", "Connection failed", e)
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) { }
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}