package com.ryomen.bikee

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private val REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        requestBluetoothPermissions()

        findViewById<Button>(R.id.btnConnect).setOnClickListener {
            showDevicePicker()
        }

        findViewById<Button>(R.id.btnIgnition).setOnClickListener {
            sendCommand("IGN")
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            sendCommand("START")
        }

        findViewById<Button>(R.id.btnLeft).setOnClickListener {
            sendCommand("LEFT")
        }

        findViewById<Button>(R.id.btnRight).setOnClickListener {
            sendCommand("RIGHT")
        }

        findViewById<Button>(R.id.btnFxMenu).setOnClickListener {
            showFxMenu()
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (notGranted.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
            Toast.makeText(this, "Bluetooth is required to control your bike", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDevicePicker() {
        val pairedDevices = bluetoothAdapter.bondedDevices

        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired devices found. Pair ESP32 first.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceNames = pairedDevices.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Device")
            .setItems(deviceNames) { _, which ->
                val selectedDevice = pairedDevices.elementAt(which)
                connectToDevice(selectedDevice)
            }
            .setCancelable(true)
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter.cancelDiscovery()
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream

                runOnUiThread {
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun sendCommand(command: String) {
        thread {
            try {
                outputStream?.write((command + "\n").toByteArray())
                runOnUiThread {
                    Toast.makeText(this, "Sent: $command", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun showFxMenu() {
        val fxOptions = arrayOf(
            "🔄 Sync Blink (Left+Right)",
            "↔ Alternate Blink",
            "⚡ Fast Blink",
            "2x Left ↔ 2x Right"
        )

        val fxCommands = arrayOf(
            "FX_SYNC", "FX_ALT", "FX_FAST", "FX_2X2"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose FX Mode")
            .setItems(fxOptions) { _, which ->
                sendCommand(fxCommands[which])
            }
            .show()
    }
}