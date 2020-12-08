package com.myapps.measurementtool

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat

class ViewActivity : AppCompatActivity() {

    private lateinit var tvDate:            TextView
    private lateinit var tvStatus:          TextView
    private lateinit var tvStatusSummary:   TextView
    private lateinit var icStatus:          ImageView
    private lateinit var lytStatus:         LinearLayout
    private lateinit var pbStatus:          ProgressBar

    private lateinit var etTitle:           EditText
    private lateinit var etLength:          EditText
    private lateinit var etWidth:           EditText
    private lateinit var etHeight:          EditText
    private lateinit var etWheelBase:       EditText
    private lateinit var etFOH:             EditText
    private lateinit var etROH:             EditText
    private lateinit var etApproachAngle:   EditText
    private lateinit var etDepartureAngle:  EditText
    private lateinit var btnSave:           Button
    private lateinit var btnDelete:         Button
    private lateinit var btnScan:           Button

    private lateinit var measurement:       Measurement
    private lateinit var hardwareProvider:  HardwareProvider
    private lateinit var sharePrefs:        SharePrefs
    private lateinit var mainHandler:       Handler

    private var enableScan: Boolean = false
        set(value) {
             if (value){
                 btnScan.isEnabled = true
                 btnScan.alpha = 1F
             } else {
                 btnScan.isEnabled = false
                 btnScan.alpha = 0.5F
             }
            field = value
        }
    private var isScanning: Boolean = false

    private val scanTimeoutTask = Runnable {
        if (isScanning){
            Toast.makeText(this@ViewActivity, "Connection Timeout", Toast.LENGTH_LONG).show()
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            enableScan = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        sharePrefs = SharePrefs(this)
        mainHandler = Handler(Looper.getMainLooper())

        bindToUI()

        initHardwareProvider()

        val id = intent.getIntExtra("id", 0)
        if (id < 1) finish()

        val measurementProvider = MeasurementsProvider(this)
        this.measurement = measurementProvider.readById(id)

        setFieldValues(this.measurement)

    }


    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(scanTimeoutTask)
    }

    override fun onDestroy() {
        super.onDestroy()
//        hardwareProvider.onDestroy()
    }

    private fun getFieldValues(): Measurement {
        return Measurement(
            id              = this.measurement.id,
            photo           = "",
            title           = etTitle.text.toString(),
            length          = getFieldDoubleValue(etLength),
            width           = getFieldDoubleValue(etWidth),
            height          = getFieldDoubleValue(etHeight),
            wheel_base      = getFieldDoubleValue(etWheelBase),
            foh             = getFieldDoubleValue(etFOH),
            roh             = getFieldDoubleValue(etROH),
            approach_angle  = getFieldDoubleValue(etApproachAngle),
            departure_angle = getFieldDoubleValue(etDepartureAngle),
            date            = this.measurement.date
        )
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun setFieldValues(measurement: Measurement) {
        tvDate.text           = SimpleDateFormat("dd/MM/yyy hh:mm:ss").format(measurement.date)
        etTitle.text          = measurement.title.toEditable()
        etLength.text         = measurement.length.toString().toEditable()
        etWidth.text          = measurement.width.toString().toEditable()
        etHeight.text         = measurement.height.toString().toEditable()
        etWheelBase.text      = measurement.wheel_base.toString().toEditable()
        etFOH.text            = measurement.foh.toString().toEditable()
        etROH.text            = measurement.roh.toString().toEditable()
        etApproachAngle.text  = measurement.approach_angle.toString().toEditable()
        etDepartureAngle.text = measurement.departure_angle.toString().toEditable()
    }

    private fun bindToUI() {
        lytStatus       = findViewById(R.id.ly_status)
        tvStatus        = findViewById(R.id.tv_status)
        tvStatusSummary = findViewById(R.id.tv_summary)
        icStatus        = findViewById(R.id.ic_status)
        pbStatus        = findViewById(R.id.pb_status)

        tvDate           = findViewById(R.id.tv_date)
        etTitle          = findViewById(R.id.et_title)
        etLength         = findViewById(R.id.et_length)
        etWidth          = findViewById(R.id.et_width)
        etHeight         = findViewById(R.id.et_height)
        etWheelBase      = findViewById(R.id.et_wheel_base)
        etFOH            = findViewById(R.id.et_foh)
        etROH            = findViewById(R.id.et_roh)
        etApproachAngle  = findViewById(R.id.et_approach_angle)
        etDepartureAngle = findViewById(R.id.et_departure_angle)
        btnSave          = findViewById(R.id.btn_save)
        btnDelete        = findViewById(R.id.btn_delete)
        btnScan          = findViewById(R.id.btn_scan)

        tvStatusSummary.text = sharePrefs.hardwareAddress

        btnDelete.setOnClickListener { showDeleteDialog() }
        btnSave.setOnClickListener { saveChanges() }
        btnScan.setOnClickListener { scanDevice() }
    }

    private fun saveChanges(){
        val measurement = getFieldValues()
        val validation = measurement.validate(this)

        if (validation.isError){
            Toast.makeText(this@ViewActivity, validation.errorMessage, Toast.LENGTH_LONG).show()
        } else {
            val measurementsProvider = MeasurementsProvider(this)
            val result = measurementsProvider.update(measurement)
            if (result) {
                val message = getString(R.string.toast_msg_update_completed).replace("$", measurement.title)
                Toast.makeText(this@ViewActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDeleteDialog(){
        val dialogBuilder = AlertDialog.Builder(this)
        val yesLabel = getString(R.string.btn_yes)
        val cancelLabel = getString(R.string.btn_cancel)
        val question = getString(R.string.question_delete)
        val title = getString(R.string.question_delete_title)

        dialogBuilder.setMessage(question)
            .setCancelable(false)
            .setPositiveButton(yesLabel) { _, _ -> deleteData() }
            .setNegativeButton(cancelLabel) { dialog, _ -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle(title)
        alert.show()
    }

    private fun deleteData(){
        val measurementsProvider = MeasurementsProvider(this)
        val result = measurementsProvider.delete(measurement.id)
        if (result) {
            val message = getString(R.string.toast_msg_delete_completed).replace("$", measurement.title)
            Toast.makeText(this@ViewActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }



    private fun initHardwareProvider(){
        setConnectionStatus(ConnectionStatus.CONNECTING)
        hardwareProvider = HardwareProvider(URI(sharePrefs.hardwareAddress))
        hardwareProvider.connectionChangedCallback = { status -> setConnectionStatus(status) }
        hardwareProvider.reciveMessageCallback = { message -> onMessageRecived(message) }
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()
    }

    private fun scanDevice(){
        enableScan = false
        isScanning = true
        mainHandler.postDelayed(scanTimeoutTask, 5000)
        hardwareProvider.scanDistance()
    }

    private fun onMessageRecived(msg: String?){
        val stringValue = msg?.replace("Distance: ", "")?.trim()
        val value: Double = stringValue?.fullTrim()?.toDouble() ?: 0.0

        runOnUiThread {
            if (isScanning){
                mainHandler.removeCallbacks(scanTimeoutTask)
                isScanning = false
                enableScan = true
            }

            for (field in numbersField()) {
                if(field.isFocused){
                    field.text = value.toString().fullTrim().toEditable()
                    break
                }
            }
        }
    }

    private fun setConnectionStatus(status: ConnectionStatus){
        runOnUiThread {
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                    tvStatus.text = resources.getString(R.string.connected)
                    icStatus.setImageResource(R.drawable.ic_baseline_check_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    enableScan = true
                }
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    tvStatus.text = resources.getString(R.string.disconnected)
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    enableScan = false
                }
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                    tvStatus.text = resources.getString(R.string.connecting)
                    icStatus.visibility = View.GONE
                    pbStatus.visibility = View.VISIBLE
                    enableScan = false
                }
            }
        }
    }


    // Helpers
    private fun String.fullTrim() = trim().replace("\uFEFF", "")
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private fun numbersField(): Array<EditText> {
        return arrayOf(etLength, etWidth, etHeight, etWheelBase, etFOH, etROH, etApproachAngle, etDepartureAngle)
    }
    private fun getFieldDoubleValue(field: EditText): Double {
        val text = field.text.toString()
        if (text.isNullOrBlank())
            return 0.0
        return text.fullTrim().toDouble()
    }

}