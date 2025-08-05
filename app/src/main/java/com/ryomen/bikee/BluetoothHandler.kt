package com.ryomen.bikee

import android.bluetooth.BluetoothSocket
import java.io.OutputStream

object BluetoothHandler {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    fun setSocket(bluetoothSocket: BluetoothSocket) {
        socket = bluetoothSocket
        outputStream = bluetoothSocket.outputStream
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    fun send(message: String) {
        try {
            outputStream?.write(message.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}