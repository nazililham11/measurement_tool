package com.example.measurementtool

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GetStartedActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var sharePrefs: SharePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        sharePrefs = SharePrefs(this)

        if (!sharePrefs.appFirstOpened && !sharePrefs.showGetStarted){
            gotoMainActivity()
        }


        btnStart = findViewById(R.id.btn_start)

        btnStart.setOnClickListener {
            gotoMainActivity()
        }

    }

    private fun gotoMainActivity(){
        if (sharePrefs.appFirstOpened){
            sharePrefs.appFirstOpened = false
        }
        startActivity(Intent(this@GetStartedActivity, MainActivity::class.java))
    }

}