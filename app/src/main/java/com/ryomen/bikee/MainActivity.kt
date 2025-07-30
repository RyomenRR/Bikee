package com.ryomen.bikee

// Importing necessary Android, Bluetooth, JSON, UI, and utility classes
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
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
import android.app.PendingIntent
import android.net.Uri
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {

    // UUID for serial Bluetooth (SPP)
    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Bluetooth connection variables
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private lateinit var pairedDevicesList: Set<BluetoothDevice>

    // State variables
    private var ignitionOn = false
    private var isFxExpanded = false

    // UI components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up top toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Ignition button from layout
        val toggleIgnition = findViewById<Button>(R.id.toggleIgnition)

        // 🔧 Setup shortcut for "Start Bike" using Android's pinned shortcut
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
        val shortcut = ShortcutInfoCompat.Builder(this, "start_bike")
    .setShortLabel("Start Bike")
    .setLongLabel("Start your bike silently")
    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bike))
    .setIntent(Intent(this, StartBikeReceiver::class.java).apply {
        action = "com.ryomen.bikee.START_BIKE"
    })
    .build()

ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
            // Setup callback for success after adding shortcut
            val pinnedShortcutCallbackIntent = ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
            val successCallback = PendingIntent.getBroadcast(
                this, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            )

            ShortcutManagerCompat.requestPinShortcut(this, shortcut, successCallback.intentSender)
        } else {
            Toast.makeText(this, "Pinned shortcuts not supported", Toast.LENGTH_SHORT).show()
        }

        // Buttons from layout
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnFx = findViewById<Button>(R.id.btnFx)
        // new thing 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
    val shortcutManager = getSystemService(ShortcutManager::class.java)

    val intent = Intent("com.bikee.START_BIKE").apply {
        setClass(this@MainActivity, StartBikeReceiver::class.java)
    }

    val shortcut = ShortcutInfo.Builder(this, "start_bike")
        .setShortLabel("Start Bike")
        .setLongLabel("Start your bike silently")
        .setIcon(Icon.createWithResource(this, R.drawable.ic_bike))
        .setIntent(intent) // ← this will now trigger BroadcastReceiver
        .build()

    shortcutManager.dynamicShortcuts = listOf(shortcut)
}
        // ✅ Handle if shortcut URI triggers this activity
        intent?.data?.let { uri ->
            if (uri.toString() == "bikee://start_bike") {
                handleVoiceCommand("start bike") // Automatically act on shortcut trigger
            }
        }

        // FX section buttons
        val fxOptions = findViewById<LinearLayout>(R.id.fxOptions)
        val btnFx1 = findViewById<Button>(R.id.btnFx1)
        val btnFx2 = findViewById<Button>(R.id.btnFx2)
        val btnFx3 = findViewById<Button>(R.id.btnFx3)
        val btnFxOff = findViewById<Button>(R.id.btnFxOff)

        // Navigation drawer setup
        drawerLayout = findViewById(R.id.drawerLayout)
        val navView = findViewById<NavigationView>(R.id.navView)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Handle menu items in navigation drawer
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_language -> showLanguageDialog()
                R.id.nav_voice_commands -> {
                    // Open activity to edit custom voice commands
                    startActivity(Intent(this, CommandEditActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

// 🌟 Create a dynamic shortcut so user can trigger "start bike" via Assistant or Home Screen
        val startBikeShortcut = ShortcutInfoCompat.Builder(this, "start_bike")
            .setShortLabel("Start Bike") // Shown on the shortcut icon
            .setLongLabel("Start your bike with Google Assistant") // Shown as full label
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bike)) // Shortcut icon
            .setIntent(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW // Required action for shortcut
                putExtra("voice_command", "start bike") // Send voice command as intent extra
            }).build()

        // Add shortcut dynamically to the system (does not need user to pin)
        ShortcutManagerCompat.pushDynamicShortcut(this, startBikeShortcut)

        // ✅ Check necessary permissions (e.g. Bluetooth permissions)
        checkPermissions()

        // 🔌 Get Bluetooth adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // ❌ If adapter is null, the device doesn't support Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device 😢", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔄 If Bluetooth is off, ask user to enable it
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 123)
        } else {
        
            // ⏳ If already enabled, try to reconnect after delay
            Handler(Looper.getMainLooper()).postDelayed({
                attemptAutoReconnect()
            }, 1000)
        }
        // 🎙️ Handle incoming intent from Google Assistant or shortcut
        handleVoiceIntent()
        Log.d("BikeeLog", "✅ handleVoiceIntent was triggered")
        // 🔘 Toggle ignition button logic
        toggleIgnition.setOnClickListener {
            ignitionOn = !ignitionOn // Flip ignition state
            sendCommand(if (ignitionOn) "IGNITION_ON" else "IGNITION_OFF") // Send command
            toggleIgnition.text = if (ignitionOn) "IGNITION ON" else "IGNITION OFF" // Change label
            val colorRes = if (ignitionOn) android.R.color.holo_green_light else android.R.color.holo_red_light
            toggleIgnition.backgroundTintList = getColorStateList(colorRes) // Change button color
        }

        // 🚀 Start button logic
        btnStart.setOnClickListener {
            if (!ignitionOn) {
                sendCommand("IGNITION_ON")
                ignitionOn = true
                toggleIgnition.text = "IGNITION ON"
                toggleIgnition.backgroundTintList = getColorStateList(android.R.color.holo_green_light)
            }
            sendCommand("START_ENGINE")
        }

        // ✨ FX button toggles visibility of FX controls
        btnFx.setOnClickListener {
            if (!isFxExpanded) {
                fxOptions.visibility = View.VISIBLE
                isFxExpanded = true
            } else {
                fxOptions.visibility = View.GONE
                sendCommand("CUSTOM_FX_OFF") // Turn off FX if collapsing
                isFxExpanded = false
            }
        }

        // 🎆 Individual FX buttons
        btnFx1.setOnClickListener { sendCommand("CUSTOM_FX_1") }
        btnFx2.setOnClickListener { sendCommand("CUSTOM_FX_2") }
        btnFx3.setOnClickListener { sendCommand("CUSTOM_FX_3") }
        btnFxOff.setOnClickListener { sendCommand("CUSTOM_FX_OFF") }

        // ➕ Add manual shortcut from button in UI
        findViewById<Button>(R.id.addShortcutButton).setOnClickListener {
            createStartBikeShortcut()
        }
}
       // 🧠 End of onCreate()

    // 🔁 Function to reconnect automatically with previously saved MAC address
    private fun autoReconnect() {
        val prefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null) // Retrieve saved MAC

        if (lastMac != null) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            // For Android 12+, check permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 100)
                return
            }

            // If Bluetooth is on, try to connect to saved device
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
// 💖 Creates a shortcut on the home screen to start the bike directly
    private fun createStartBikeShortcut() {
    val shortcutIntent = Intent(this, StartBikeReceiver::class.java).apply {
        action = "com.ryomen.bikee.START_BIKE"
    }

    val shortcut = ShortcutInfoCompat.Builder(this, "start_bike")
        .setShortLabel("Start Bike")
        .setLongLabel("Start your bike silently")
        .setIcon(IconCompat.createWithResource(this, R.drawable.ic_bike))
        .setIntent(shortcutIntent) // 🔥 Not launching activity, but sending broadcast
        .build()

    ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

    Toast.makeText(this, "Silent shortcut added! 🛵", Toast.LENGTH_SHORT).show()


        // 📌 Wrap it into a launcher shortcut intent
        val addIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, "Start Bike") // Shortcut title
            putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this@MainActivity, R.drawable.ic_bike) // icon
            )
            action = "com.android.launcher.action.INSTALL_SHORTCUT"
        }

        // 📣 Broadcast to install the shortcut
        sendBroadcast(addIntent)
        Toast.makeText(this, "Home shortcut added! 🛵", Toast.LENGTH_SHORT).show()
    
}
    // 🔁 Tries to auto-reconnect to the last known paired device
    private fun attemptAutoReconnect() {
        val prefs = getSharedPreferences("bikee_prefs", Context.MODE_PRIVATE)
        val lastMac = prefs.getString("last_mac", null) // Saved device MAC

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // ❌ No Bluetooth support
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported 💔", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔌 Ask user to enable Bluetooth if it's off
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 123)
            return
        }

        // ✅ Try to connect using last MAC
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
                        // ⚠️ Failed, show prompt to reconnect manually
                        runOnUiThread { showReconnectDialog() }
                    }
                }.start()
            } catch (e: Exception) {
                showReconnectDialog()
            }
        } else {
            // ❗ No saved MAC, ask to connect
            showReconnectDialog()
        }
    }

    // 🔐 Request location/Bluetooth permissions if needed
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 100)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6 to 11
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            }
        }
    }

    // 🧠 Detects and handles incoming voice command or shortcut intent
    private fun handleVoiceIntent() {
        val assistantCmd = intent.getStringExtra("voice_command") // From Assistant or shortcut
        val data = intent.data?.toString() // From deep link (e.g., bikee://start_bike)
        val voiceInput = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) // From Google voice

        // Combine inputs in order of preference
        val input = assistantCmd ?: voiceInput?.firstOrNull() ?: data

        // If command is present, try to execute it
        input?.let { command ->
            if (outputStream == null) {
                Toast.makeText(this, "Connect to ESP32 first 💔", Toast.LENGTH_LONG).show()
                return
            }
            handleVoiceCommand(command)
        }
    }

    // 🗣 Maps the received command to an actual device command and sends it
    private fun handleVoiceCommand(command: String) {
        val lowerCmd = command.lowercase(Locale.getDefault())
        val prefs = getSharedPreferences("voice_commands", Context.MODE_PRIVATE)

        // List of known internal commands we support
        val allCmds = listOf("START_ENGINE", "IGNITION_ON", "IGNITION_OFF", "CUSTOM_FX")
        var matched = false

        for (cmd in allCmds) {
            val phrasesJson = prefs.getString(cmd, null)

            // If not customized yet, use default phrases
            val array = if (phrasesJson == null || phrasesJson == "[]") {
                when (cmd) {
                    "START_ENGINE" -> JSONArray(listOf("start bike", "engine start"))
                    "IGNITION_ON" -> JSONArray(listOf("ignition on", "turn on ignition"))
                    "IGNITION_OFF" -> JSONArray(listOf("ignition off", "turn off ignition"))
                    "CUSTOM_FX" -> JSONArray(listOf("fx", "flash", "light show"))
                    else -> JSONArray()
                }
            } else JSONArray(phrasesJson)

            // Loop through each phrase and see if any matches
            for (i in 0 until array.length()) {
                val phrase = array.getString(i).lowercase(Locale.getDefault())
                if (lowerCmd.contains(phrase)) {
                    Log.d("BikeeLog", "🗣 Matching voice command: $command")
                    sendCommand(cmd) // Send command to ESP32
                    matched = true
                    break
                }
            }

            if (matched) break
        }

        if (!matched) {
            // 😢 User said something we didn't understand
            Toast.makeText(this, "Unknown command: $command 🤷", Toast.LENGTH_SHORT).show()
        }
    }
// 💞 Shows a dialog prompting the user to reconnect to their bike
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

    // 🔍 Displays list of paired Bluetooth devices to let the user pick one
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

        // ✅ Get list of already paired devices
        pairedDevicesList = bluetoothAdapter.bondedDevices

        if (pairedDevicesList.isEmpty()) {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
            return
        }

        val devices = pairedDevicesList.map { it.name + "\n" + it.address }.toTypedArray()

        // 🧠 Show dialog to choose a device
        AlertDialog.Builder(this)
            .setTitle("Select your ESP32")
            .setItems(devices) { _, which ->
                val device = pairedDevicesList.elementAt(which)
                connectToDevice(device)
            }.show()
    }

    // 🔌 Connects to a selected Bluetooth device
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

    // 🚀 Sends a command string to the connected ESP32 device
    private fun sendCommand(cmd: String) {
        try {
            outputStream?.write((cmd + "\n").toByteArray())
        } catch (e: Exception) {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
        }
    }

    // 🌐 Shows language selection dialog and applies locale change
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.english),
            getString(R.string.hindi),
            getString(R.string.gujarati)
        )
        val codes = arrayOf("en", "hi", "gu")

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codes[which]))
                recreate() // Restart to apply language
            }
            .show()
    }

    override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent) // Make sure intent updates
    handleVoiceIntent() // 🧠 This will process the voice command again
}
    // ☰ Handles navigation drawer toggle
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) true
        else super.onOptionsItemSelected(item)
    }

    // 🔙 Result from Bluetooth enable intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            attemptAutoReconnect()
        }
    }

    // 😢 Back button with graceful exit + safety off
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

    // 💨 Called when app is closed, safely turn off ignition
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing || !isChangingConfigurations) {
            sendCommand("IGNITION_OFF")
        }
    }

    // 🛡️ Permission result handler
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == 100 || requestCode == 101) && grantResults.isNotEmpty()) {
            val msg = if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                "Bluetooth permission granted" else "Permission denied. Bluetooth may not work."
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
} 