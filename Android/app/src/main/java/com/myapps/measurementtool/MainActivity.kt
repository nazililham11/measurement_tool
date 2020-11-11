package com.myapps.measurementtool

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var btnNew:        Button
    private lateinit var btnList:       Button
    private lateinit var btnAbout:      Button
    private lateinit var btnExit:       Button
    private lateinit var btnCheck:      Button
    private lateinit var btnSettings:   Button
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnNew          = findViewById(R.id.btn_new)
        btnList         = findViewById(R.id.btn_list)
        btnAbout        = findViewById(R.id.btn_about)
        btnExit         = findViewById(R.id.btn_exit)
        btnCheck        = findViewById(R.id.btn_check)
        btnSettings     = findViewById(R.id.btn_settings)

        btnNew.setOnClickListener {
            startActivity(Intent(this@MainActivity, CreateActivity::class.java))
        }
        btnList.setOnClickListener {
            startActivity(Intent(this@MainActivity, ListActivity::class.java))
        }
        btnAbout.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        btnExit.setOnClickListener {
            moveTaskToBack(true)
            exitProcess(-1)
        }

    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            moveTaskToBack(true)
            exitProcess(-1)
        }
        this.doubleBackToExitPressedOnce = true
        val msg = getString(R.string.toast_msg_back_twice_to_exit)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

}