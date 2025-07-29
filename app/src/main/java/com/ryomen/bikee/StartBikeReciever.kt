package com.ryomen.bikee

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.io.OutputStream
import java.util.*

class StartBikeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        if (lastMac == null) {
            Toast.makeText(context, "No saved bike to start 💔", Toast.LENGTH_SHORT).show()
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth is off 💢", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(lastMac)
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            Thread {
                try {
                    socket.connect()
                    val outputStream: OutputStream = socket.outputStream
                    outputStream.write("IGNITION_ON\nSTART_ENGINE\n".toByteArray())
                    outputStream.flush()
                    outputStream.close()
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start bike", Toast.LENGTH_SHORT).show()
        }
    }
}