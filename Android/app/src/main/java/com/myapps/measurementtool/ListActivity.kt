package com.myapps.measurementtool

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Class Activity/tampilan list pengukuran
class ListActivity : AppCompatActivity() {

    private lateinit var rvMeasurement:     RecyclerView
    private lateinit var listUserAdapter:   MeasurementListAdapter
    private lateinit var tvEmpty:           TextView
    private lateinit var progressBar:       ProgressBar


    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        // Hubungkan vaeriable dengan elemen UI
        rvMeasurement = findViewById(R.id.rv_measurement)
        tvEmpty       = findViewById(R.id.tv_empty)
        progressBar   = findViewById(R.id.progress_list)

        // Inisialisasi List Adapter
        listUserAdapter = MeasurementListAdapter(arrayListOf())
        rvMeasurement.layoutManager = LinearLayoutManager(this)
        rvMeasurement.adapter = listUserAdapter

        // Memberikan event saat item pada list ditekan 
        listUserAdapter.onItemClick = { measurement ->

            // Ambil id dari item dan buka View Activity dan sertakan id tadi pada View Activity
            val intent = Intent(this@ListActivity, ViewActivity::class.java)
            intent.putExtra("id", measurement.id)
            startActivity(intent)
        }

    }

    // Event saat activity dilanjutkan atau saat activity telah dibuat 
    override fun onResume() {
        super.onResume()

        // Inisialisasi komunikasi dengan database
        val measurementsProvider = MeasurementsProvider(this)

        // Baca data dari database
        val list = measurementsProvider.read()

        // Apabila tidak ada data maka tampilkan teks Empty
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        // Update tampilan list
        listUserAdapter.update(list)
    }

}