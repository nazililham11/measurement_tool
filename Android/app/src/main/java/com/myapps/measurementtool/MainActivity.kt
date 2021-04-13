package com.myapps.measurementtool

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

// Class Activity/tampilan menu utama
class MainActivity : AppCompatActivity() {

    private lateinit var btnNew:        Button
    private lateinit var btnList:       Button
    private lateinit var btnAbout:      Button
    private lateinit var btnExit:       Button
    private lateinit var btnCheck:      Button
    private lateinit var btnSettings:   Button
    
    private var doubleBackToExitPressedOnce = false


    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hubungkan vaeriable dengan elemen UI
        btnNew          = findViewById(R.id.btn_new)
        btnList         = findViewById(R.id.btn_list)
        btnAbout        = findViewById(R.id.btn_about)
        btnExit         = findViewById(R.id.btn_exit)
        btnCheck        = findViewById(R.id.btn_check)
        btnSettings     = findViewById(R.id.btn_settings)

        // Memberikan event saat tombol ditekan 
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
            // Keluar aplikasi
            moveTaskToBack(true)
            exitProcess(-1)
        }

    }

    // Event saat tombol back ditekan 
    override fun onBackPressed() {
        // Apabila tombol back telah ditekan 2 kali 
        if (doubleBackToExitPressedOnce) {
            // Keluar aplikasi
            moveTaskToBack(true)
            exitProcess(-1)
        }
        
        // Tampilkan pesan press_back_twice_to_exit
        this.doubleBackToExitPressedOnce = true
        val msg = getString(R.string.toast_msg_back_twice_to_exit)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        // Berikan timeout 2 detik (memberikan batas waktu untuk menekan tombol back ke 2 selama 2detik)
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

}