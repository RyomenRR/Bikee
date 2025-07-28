package com.ryomen.bikee

import android.app.AlertDialog
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.OutputStream
import java.util.*
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private lateinit var pairedDevicesList: Set<BluetoothDevice>
    private var ignitionOn = false
    private var isFxExpanded = false

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val toggleIgnition = findViewById<Button>(R.id.toggleIgnition)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnFx = findViewById<Button>(R.id.btnFx)

        val fxOptions = findViewById<LinearLayout>(R.id.fxOptions)
        val btnFx1 = findViewById<Button>(R.id.btnFx1)
        val btnFx2 = findViewById<Button>(R.id.btnFx2)
        val btnFx3 = findViewById<Button>(R.id.btnFx3)
        val btnFxOff = findViewById<Button>(R.id.btnFxOff)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_language -> showLanguageDialog()
                R.id.nav_voice_commands -> startActivity(Intent(this, CommandEditActivity::class.java))
            }
            drawerLayout.closeDrawers()
            true
        }

        val startBikeShortcut = ShortcutInfoCompat.Builder(this, "start_bike")
            .setShortLabel("Start Bike")
            .setLongLabel("Start your bike with Google Assistant")
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bike))
            .setIntent(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("voice_command", "start bike")
            }).build()
        ShortcutManagerCompat.pushDynamicShortcut(this, startBikeShortcut)

        checkPermissions()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device 😢", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 123)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                attemptAutoReconnect()
            }, 1000)
        }

        toggleIgnition.setOnClickListener {
            ignitionOn = !ignitionOn
            sendCommand(if (ignitionOn) "IGNITION_ON" else "IGNITION_OFF")
            toggleIgnition.text = if (ignitionOn) "IGNITION ON" else "IGNITION OFF"
            val colorRes = if (ignitionOn) android.R.color.holo_green_light else android.R.color.holo_red_light
            toggleIgnition.backgroundTintList = getColorStateList(colorRes)
        }

        btnStart.setOnClickListener {
            if (!ignitionOn) {
                sendCommand("IGNITION_ON")
                ignitionOn = true
                toggleIgnition.text = "IGNITION ON"
                toggleIgnition.backgroundTintList = getColorStateList(android.R.color.holo_green_light)
            }
            sendCommand("START_ENGINE")
        }

        btnFx.setOnClickListener {
            if (!isFxExpanded) {
                fxOptions.visibility = View.VISIBLE
                isFxExpanded = true
            } else {
                fxOptions.visibility = View.GONE
                sendCommand("CUSTOM_FX_OFF")
                isFxExpanded = false
            }
        }

        btnFx1.setOnClickListener { sendCommand("CUSTOM_FX_1") }
        btnFx2.setOnClickListener { sendCommand("CUSTOM_FX_2") }
        btnFx3.setOnClickListener { sendCommand("CUSTOM_FX_3") }
        btnFxOff.setOnClickListener { sendCommand("CUSTOM_FX_OFF") }

        handleVoiceIntent()
        Log.d("BikeeLog", "✅ handleVoiceIntent was triggered")

        findViewById<Button>(R.id.addShortcutButton).setOnClickListener {
    createStartBikeShortcut()
        }
    }

    private fun autoReconnect() {
        val prefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        if (lastMac != null) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 100)
                return
            }

            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                try {
                    val device = bluetoothAdapter.getRemoteDevice(lastMac)
                    connectToDevice(device)
                } catch (e: Exception) {
                    Log.e("Bikee", "Reconnect failed: ${e.message}")
                }
            } else {
                Log.d("Bikee", "Bluetooth not enabled yet")
            }
        } else {
            Log.d("Bikee", "No previous MAC saved")
        }
    }
    private fun createStartBikeShortcut() {
    val shortcutIntent = Intent(this, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra("voice_command", "start bike")
    }

    val addIntent = Intent().apply {
        putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start Bike")
        putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(this@MainActivity, R.drawable.ic_bike)) // use your icon here
        action = "com.android.launcher.action.INSTALL_SHORTCUT"
    }

    sendBroadcast(addIntent)
    Toast.makeText(this, "Home shortcut added! 🛵", Toast.LENGTH_SHORT).show()
    }
    
    private fun attemptAutoReconnect() {
        val prefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported 💔", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 123)
            return
        }

        if (lastMac != null) {
            try {
                val device = bluetoothAdapter.getRemoteDevice(lastMac)
                Thread {
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
                        bluetoothSocket?.connect()
                        outputStream = bluetoothSocket?.outputStream

                        runOnUiThread {
                            Toast.makeText(this, "Reconnected to ${device.name}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showReconnectDialog() }
                    }
                }.start()
            } catch (e: Exception) {
                showReconnectDialog()
            }
        } else {
            showReconnectDialog()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 100)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            }
        }
    }

    private fun handleVoiceIntent() {
    val assistantCmd = intent.getStringExtra("voice_command")
    val data = intent.data?.toString()
    val voiceInput = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

    val input = assistantCmd ?: voiceInput?.firstOrNull() ?: data

    input?.let { command ->
        if (outputStream == null) {
            Toast.makeText(this, "Connect to ESP32 first 💔", Toast.LENGTH_LONG).show()
            return
        }
        handleVoiceCommand(command)
    }
}

    private fun handleVoiceCommand(command: String) {
        val lowerCmd = command.lowercase(Locale.getDefault())
        val prefs = getSharedPreferences("voice_commands", Context.MODE_PRIVATE)
        val allCmds = listOf("START_ENGINE", "IGNITION_ON", "IGNITION_OFF", "CUSTOM_FX")
        var matched = false
        for (cmd in allCmds) {
            val phrasesJson = prefs.getString(cmd, null)
            val array = if (phrasesJson == null || phrasesJson == "[]") {
                when (cmd) {
                    "START_ENGINE" -> JSONArray(listOf("start bike", "engine start"))
                    "IGNITION_ON" -> JSONArray(listOf("ignition on", "turn on ignition"))
                    "IGNITION_OFF" -> JSONArray(listOf("ignition off", "turn off ignition"))
                    "CUSTOM_FX" -> JSONArray(listOf("fx", "flash", "light show"))
                    else -> JSONArray()
                }
            } else JSONArray(phrasesJson)
            for (i in 0 until array.length()) {
                val phrase = array.getString(i).lowercase(Locale.getDefault())
                if (lowerCmd.contains(phrase)) {
                    Log.d("BikeeLog", "🗣 Matching voice command: $command")
                    sendCommand(cmd)
                    matched = true
                    break
                }
            }
            if (matched) break
        }
        if (!matched) {
            Toast.makeText(this, "Unknown command: $command 🤷", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReconnectDialog() {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Bike not connected")
                .setMessage("Do you want to connect now?")
                .setPositiveButton("Yes") { _, _ -> showDeviceList() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showDeviceList() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 123)
            return
        }
        pairedDevicesList = bluetoothAdapter.bondedDevices
        if (pairedDevicesList.isEmpty()) {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
            return
        }
        val devices = pairedDevicesList.map { it.name + "\n" + it.address }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select your ESP32")
            .setItems(devices) { _, which ->
                val device = pairedDevicesList.elementAt(which)
                connectToDevice(device)
            }.show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                runOnUiThread {
                    Toast.makeText(this, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
                    val sharedPrefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("last_mac", device.address).apply()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun sendCommand(cmd: String) {
        try {
            outputStream?.write((cmd + "\n").toByteArray())
        } catch (e: Exception) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.english), getString(R.string.hindi), getString(R.string.gujarati))
        val codes = arrayOf("en", "hi", "gu")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[which]))
                recreate()
            }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            attemptAutoReconnect()
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit Bikee")
            .setMessage("Do you want to turn off ignition and exit?")
            .setPositiveButton("Yes") { _, _ ->
                sendCommand("IGNITION_OFF")
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing || !isChangingConfigurations) {
            sendCommand("IGNITION_OFF")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == 100 || requestCode == 101) && grantResults.isNotEmpty()) {
            val msg = if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                "Bluetooth permission granted" else "Permission denied. Bluetooth may not work."
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
