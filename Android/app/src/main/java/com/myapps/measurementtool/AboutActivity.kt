package com.myapps.measurementtool

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

// Class Activity/tampilan about
class AboutActivity : AppCompatActivity() {

    private lateinit var btnClose: Button

    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    
        // Hubungkan vaeriable dengan elemen UI
        btnClose = findViewById(R.id.btn_close)


        // Event Saat tombol close ditekan
        btnClose.setOnClickListener {
            finish()    // Keluar activity
        }

    }
}