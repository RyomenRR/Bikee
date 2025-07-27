package com.ryomen.bikee

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray

class CommandEditActivity : AppCompatActivity() {

    private val commandKeys = listOf(
        "START_ENGINE",
        "IGNITION_ON",
        "IGNITION_OFF",
        "INDICATOR_MODE",
        "CUSTOM_FX"
    )

    private lateinit var commandsContainer: LinearLayout
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_command_edit)

        commandsContainer = findViewById(R.id.commandsContainer)
        prefs = getSharedPreferences("voice_commands", Context.MODE_PRIVATE)

        for (cmd in commandKeys) {
            addCommandBlock(cmd)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveAllCommands()
        }
    }

    private fun addCommandBlock(commandKey: String) {
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 48)
            }
        }

        val title = TextView(this).apply {
            text = commandKey.replace("_", " ")
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 8, 16, 8)
        }

        val phraseList = mutableListOf<EditText>()
        val saved = prefs.getString(commandKey, "[]") ?: "[]"
        val phrases = JSONArray(saved)

        group.addView(title)

        for (i in 0 until phrases.length()) {
            val et = createEditText(phrases.getString(i))
            group.addView(et)
            phraseList.add(et)
        }

        val addButton = Button(this).apply {
            text = "+ ADD PHRASE"
            setBackgroundColor(android.graphics.Color.DKGRAY)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 16, 16, 16)
        }

        addButton.setOnClickListener {
            val input = EditText(this).apply {
    hint = "Enter phrase"
    setTextColor(android.graphics.Color.BLACK)
    
    setHintTextColor(android.graphics.Color.DKGRAY)
    
    setBackgroundColor(android.graphics.Color.WHITE)
    
    setPadding(32, 24, 32, 24)
}

            AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle("Add Phrase")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val phrase = input.text.toString().trim()
                    if (phrase.isNotEmpty()) {
                        val et = createEditText(phrase)
                        group.addView(et, group.childCount - 1) // Insert above button
                        phraseList.add(et)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        group.addView(addButton)
        group.tag = Pair(commandKey, phraseList)
        commandsContainer.addView(group)
    }

    private fun createEditText(text: String): EditText {
    return EditText(this).apply {
        setText(text)
        hint = "Voice phrase"
        inputType = InputType.TYPE_CLASS_TEXT
        setTextColor(android.graphics.Color.BLACK)        // ← Black text
        setHintTextColor(android.graphics.Color.DKGRAY)   // ← Dark gray hint
        setBackgroundColor(android.graphics.Color.WHITE)  // ← White background
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16, 8, 16, 8)
        }
        setPadding(24, 16, 24, 16)  // Nice inner spacing
    }
}

    private fun saveAllCommands() {
        val editor = prefs.edit()
        for (i in 0 until commandsContainer.childCount) {
            val block = commandsContainer.getChildAt(i)
            if (block.tag is Pair<*, *>) {
                val (commandKey, fields) = block.tag as Pair<String, List<EditText>>
                val json = JSONArray()
                fields.forEach {
                    val value = it.text.toString().trim()
                    if (value.isNotEmpty()) json.put(value)
                }
                editor.putString(commandKey, json.toString())
            }
        }
        editor.apply()
        Toast.makeText(this, "Commands saved 😘", Toast.LENGTH_SHORT).show()
    }
}