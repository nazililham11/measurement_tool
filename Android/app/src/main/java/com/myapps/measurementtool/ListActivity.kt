package com.myapps.measurementtool

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListActivity : AppCompatActivity() {

    private lateinit var rvMeasurement:     RecyclerView
    private lateinit var listUserAdapter:   MeasurementListAdapter
    private lateinit var tvEmpty:           TextView
    private lateinit var progressBar:       ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        rvMeasurement = findViewById(R.id.rv_measurement)
        tvEmpty       = findViewById(R.id.tv_empty)
        progressBar   = findViewById(R.id.progress_list)

        listUserAdapter = MeasurementListAdapter(arrayListOf())
        rvMeasurement.layoutManager = LinearLayoutManager(this)
        rvMeasurement.adapter = listUserAdapter

        listUserAdapter.onItemClick = { measurement ->
            val intent = Intent(this@ListActivity, ViewActivity::class.java)
            intent.putExtra("id", measurement.id)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        val measurementsProvider = MeasurementsProvider(this)
        val list = measurementsProvider.read()
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        listUserAdapter.update(list)
    }

}