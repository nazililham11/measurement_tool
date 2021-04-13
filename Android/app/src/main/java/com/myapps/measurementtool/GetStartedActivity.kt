package com.myapps.measurementtool

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

// Class Activity/tampilan get started
class GetStartedActivity : AppCompatActivity() {

    
    private lateinit var btnStart: Button
    private lateinit var sharePrefs: SharePrefs


    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        // Inisialisasi shared preferences
        sharePrefs = SharePrefs(this)

        // Apabila aplikasi sudah pernah dibuka sebelumnya dan setting showGetStarted dinonaktifkan 
        if (!sharePrefs.appFirstOpened && !sharePrefs.showGetStarted){
            // Buka Main activity
            gotoMainActivity()
        }

        // Bind tombol start dengan variable 
        btnStart = findViewById(R.id.btn_start)

        // Berikan event saat tombol start ditekan 
        btnStart.setOnClickListener { gotoMainActivity() }

    }


    // Fungsi untuk berpindah ke main activity
    private fun gotoMainActivity(){
        // Apakah aplikasi baru pertama kali dibuka 
        if (sharePrefs.appFirstOpened){
            // Simpan pada shared preferences  
            sharePrefs.appFirstOpened = false
        }
        // Buka main activity 
        startActivity(Intent(this@GetStartedActivity, MainActivity::class.java))
    }

}