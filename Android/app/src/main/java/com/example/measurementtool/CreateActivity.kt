package com.example.measurementtool

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

class CreateActivity : AppCompatActivity() {

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

    private lateinit var btnScan:           Button
    private lateinit var tvStatus:          TextView
    private lateinit var tvStatusSummary:   TextView
    private lateinit var icStatus:          ImageView
    private lateinit var lytStatus:         LinearLayout
    private lateinit var pbStatus:          ProgressBar

    private lateinit var hardwareProvider:  HardwareProvider
    private lateinit var numbersField:      Array<EditText>
    private lateinit var sharePrefs:        SharePrefs
    private lateinit var mainHandler:       Handler

    private var isScanning = false
    private val scanTimeoutTask = Runnable {
        if (isScanning){
            Toast.makeText(this@CreateActivity, "Connection Timeout", Toast.LENGTH_LONG).show()
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            btnScan.isEnabled = false
            btnScan.alpha = 0.5F
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        bindToUI()

        mainHandler = Handler(Looper.getMainLooper())
        sharePrefs = SharePrefs(this)

        setConnectionStatus(ConnectionStatus.CONNECTING)

        hardwareProvider = HardwareProvider(URI(sharePrefs.hardwareAddress))
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()

    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(scanTimeoutTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hardwareProvider.isOpen)
            hardwareProvider.close()
    }

    private fun bindToUI(){
        etTitle             = findViewById(R.id.et_title)
        etLength            = findViewById(R.id.et_length)
        etWidth             = findViewById(R.id.et_width)
        etHeight            = findViewById(R.id.et_height)
        etWheelBase         = findViewById(R.id.et_wheel_base)
        etFOH               = findViewById(R.id.et_foh)
        etROH               = findViewById(R.id.et_roh)
        etApproachAngle     = findViewById(R.id.et_approach_angle)
        etDepartureAngle    = findViewById(R.id.et_departure_angle)
        btnSave             = findViewById(R.id.btn_save)
        btnScan             = findViewById(R.id.btn_scan)
        lytStatus           = findViewById(R.id.ly_status)
        icStatus            = findViewById(R.id.ic_status)
        tvStatus            = findViewById(R.id.tv_status)
        tvStatusSummary     = findViewById(R.id.tv_summary)
        pbStatus            = findViewById(R.id.pb_status)
        numbersField        = arrayOf(etLength, etWidth, etHeight, etWheelBase, etFOH, etROH, etApproachAngle, etDepartureAngle)

        btnSave.setOnClickListener {
            val validation = validate()
            if (!validation.isError) saveChanges()
            else Toast.makeText(this@CreateActivity, validation.errorMessage, Toast.LENGTH_LONG).show()
        }
        btnScan.setOnClickListener {
            btnScan.isEnabled = false
            btnScan.alpha = 0.5F
            isScanning = true
            mainHandler.postDelayed(scanTimeoutTask, 5000)

            if (hardwareProvider.isOpen) hardwareProvider.send("ping")
        }
    }

    private fun validate(): ValidationResult {
        if (etTitle.text.toString().isBlank()) {
            val msg = getString(R.string.error_msg_title_empty)
            return ValidationResult(true, msg)
        }
        for (i in numbersField){
            if (i.text.toString().isBlank() || i.text.toString().fullTrim().toDouble() <= 0.0){
                val msg = getString(R.string.error_msg_field_empty_or_less_zero).replace("$", i.hint.toString())
                return ValidationResult(true, msg)
            }
        }
        return ValidationResult(false)
    }

    private fun saveChanges(){
        val measurement = Measurement(
            title           = etTitle.text.toString(),
            length          = etLength.text.toString().fullTrim().toDouble(),
            width           = etWidth.text.toString().fullTrim().toDouble(),
            height          = etHeight.text.toString().fullTrim().toDouble(),
            wheel_base      = etWheelBase.text.toString().fullTrim().toDouble(),
            foh             = etFOH.text.toString().fullTrim().toDouble(),
            roh             = etROH.text.toString().fullTrim().toDouble(),
            approach_angle  = etApproachAngle.text.toString().fullTrim().toDouble(),
            departure_angle = etDepartureAngle.text.toString().fullTrim().toDouble(),
            date            = Calendar.getInstance().time
        )
        val measurementsProvider = MeasurementsProvider(this)
        val result = measurementsProvider.insert(measurement)
        if (result) {

            val message = getString(R.string.toast_msg_create_completed).replace("$", measurement.title)
            Toast.makeText(this@CreateActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setConnectionStatus(status: ConnectionStatus){
        runOnUiThread {
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    tvStatus.text = resources.getString(R.string.connected)
                    icStatus.setImageResource(R.drawable.ic_baseline_check_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    btnScan.isEnabled = true
                    btnScan.alpha = 1F
                }
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    tvStatus.text = resources.getString(R.string.disconnected)
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    btnScan.isEnabled = false
                    btnScan.alpha = 0.5F
                }
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    tvStatus.text = resources.getString(R.string.connecting)
                    icStatus.visibility = View.GONE
                    pbStatus.visibility = View.VISIBLE
                    btnScan.isEnabled = false
                    btnScan.alpha = 0.5F
                }
            }
        }
    }

    private fun messageCallback(value: Double){
        val editableValue = value.toString().fullTrim().toEditable()

        runOnUiThread {
            if (isScanning){
                mainHandler.removeCallbacks(scanTimeoutTask)
                isScanning = false
                btnScan.isEnabled = true
                btnScan.alpha = 1F
            }

            // Fill to Focused Field
            for (field in numbersField) {
                if(field.isFocused){
                    field.text = editableValue
                    break
                }
            }
        }
    }

    private fun String.fullTrim() = trim().replace("\uFEFF", "")

    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

    inner class HardwareProvider(serverUri: URI?) : WebSocketClient(serverUri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            setConnectionStatus(ConnectionStatus.CONNECTED)
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            setConnectionStatus(ConnectionStatus.DISCONNECT)
        }

        override fun onMessage(message: String?) {
            val stringValue = message?.replace("Distance: ", "")?.trim()
            val value: Double = stringValue?.fullTrim()?.toDouble() ?: 0.0
            messageCallback(value)
        }

        override fun onError(ex: Exception?) {
            Log.d("HardwareProvider", ex?.message.toString())
        }


    }

    enum class ConnectionStatus{
        DISCONNECT,
        CONNECTED,
        CONNECTING
    }

    data class ValidationResult(
        var isError: Boolean = false,
        var errorMessage: String = ""
    )

}

