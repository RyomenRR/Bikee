package com.ryomen.bikee

import android.util.Log

object MyBluetoothService {
    fun sendCommand(command: String) {
        // Replace with your actual Bluetooth transmission code
        Log.d("MyBluetoothService", "Sending command: $command")
        // Example: BluetoothConnection.send(command)
    }
}