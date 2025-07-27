package com.ryomen.bikee

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class SettingsActivity : AppCompatActivity() {

    private val commands = listOf(
        "START_ENGINE",
        "IGNITION_ON",
        "IGNITION_OFF",
        "INDICATOR_MODE",
        "CUSTOM_FX"
    )

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var commandMap: MutableMap<String, MutableList<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        listView = findViewById(R.id.listCommands)
        commandMap = loadCommandMap()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, commands)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val command = commands[position]
            showEditDialog(command)
        }
    }

    private fun showEditDialog(command: String) {
        val phrases = commandMap[command] ?: mutableListOf()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Phrases for $command")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val listView = ListView(this)
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, phrases)
        listView.adapter = listAdapter

        listView.setOnItemLongClickListener { _, _, position, _ ->
            phrases.removeAt(position)
            listAdapter.notifyDataSetChanged()
            true
        }

        val input = EditText(this)
        input.hint = "Add new phrase"
        input.inputType = InputType.TYPE_CLASS_TEXT

        layout.addView(listView)
        layout.addView(input)

        builder.setView(layout)

        builder.setPositiveButton("Add") { _, _ ->
            val newPhrase = input.text.toString().trim()
            if (newPhrase.isNotEmpty()) {
                phrases.add(newPhrase)
                commandMap[command] = phrases
                saveCommandMap(commandMap)
            }
        }

        builder.setNegativeButton("Done", null)
        builder.show()
    }

    private fun getPrefs() = getSharedPreferences("voice_commands", Context.MODE_PRIVATE)

    private fun loadCommandMap(): MutableMap<String, MutableList<String>> {
        val prefs = getPrefs()
        val map = mutableMapOf<String, MutableList<String>>()
        for (cmd in commands) {
            val json = prefs.getString(cmd, "[]")
            val arr = JSONArray(json)
            val phrases = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                phrases.add(arr.getString(i))
            }
            map[cmd] = phrases
        }
        return map
    }

    private fun saveCommandMap(map: Map<String, List<String>>) {
        val editor = getPrefs().edit()
        for ((cmd, list) in map) {
            editor.putString(cmd, JSONArray(list).toString())
        }
        editor.apply()
    }
}