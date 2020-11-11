package com.myapps.measurementtool

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.text.SimpleDateFormat

class ViewActivity : AppCompatActivity() {

//    private lateinit var tvId:              TextView
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
    private lateinit var numbersField:      Array<EditText>
    private lateinit var sharePrefs:        SharePrefs
    private lateinit var mainHandler:       Handler

    private var isScanning = false
    private val scanTimeoutTask = Runnable {
        if (isScanning){
            Toast.makeText(this@ViewActivity, "Connection Timeout", Toast.LENGTH_LONG).show()
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            btnScan.isEnabled = false
            btnScan.alpha = 0.5F
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        bindToUI()

        mainHandler = Handler(Looper.getMainLooper())
        sharePrefs = SharePrefs(this)
        setConnectionStatus(ConnectionStatus.CONNECTING)
        hardwareProvider = HardwareProvider(URI(sharePrefs.hardwareAddress))
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()

        val id = intent.getIntExtra("id", 0)
        if (id < 1) finish()

        val measurementProvider = MeasurementsProvider(this)
        this.measurement = measurementProvider.readById(id)

        fillToUI(this.measurement)

    }

    private fun getFromUI(): Measurement {
        return Measurement(
            id              = this.measurement.id,
            title           = etTitle.text.toString(),
            length          = etLength.text.toString().fullTrim().toDouble(),
            width           = etWidth.text.toString().fullTrim().toDouble(),
            height          = etHeight.text.toString().fullTrim().toDouble(),
            wheel_base      = etWheelBase.text.toString().fullTrim().toDouble(),
            foh             = etFOH.text.toString().fullTrim().toDouble(),
            roh             = etROH.text.toString().fullTrim().toDouble(),
            approach_angle  = etApproachAngle.text.toString().fullTrim().toDouble(),
            departure_angle = etDepartureAngle.text.toString().fullTrim().toDouble(),
            date            = this.measurement.date
        )
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun fillToUI(measurement: Measurement) {
        //        tvId.text             = "ID ${measurement.id}"
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

        numbersField        = arrayOf(etLength, etWidth, etHeight, etWheelBase, etFOH, etROH, etApproachAngle, etDepartureAngle)

        btnDelete.setOnClickListener {
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

        btnSave.setOnClickListener {
            val measurementsProvider = MeasurementsProvider(this)
            val measurement = getFromUI()
            val result = measurementsProvider.update(measurement)
            if (result) {
                val message = getString(R.string.toast_msg_update_completed).replace("$", measurement.title)
                Toast.makeText(this@ViewActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        btnScan.setOnClickListener {
            btnScan.isEnabled = false
            btnScan.alpha = 0.5F
            isScanning = true
            mainHandler.postDelayed(scanTimeoutTask, 5000)

            if (hardwareProvider.isOpen) hardwareProvider.send("ping")
        }

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

    private fun setConnectionStatus(status: ConnectionStatus){
        runOnUiThread {
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                    tvStatus.text = resources.getString(R.string.connected)
                    icStatus.setImageResource(R.drawable.ic_baseline_check_24)
                    icStatus.visibility = View.VISIBLE
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    pbStatus.visibility = View.GONE
                    btnScan.isEnabled = true
                    btnScan.alpha = 1F
                }
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    tvStatus.text = resources.getString(R.string.disconnected)
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)
                    icStatus.visibility = View.VISIBLE
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    pbStatus.visibility = View.GONE
                    btnScan.isEnabled = false
                    btnScan.alpha = 0.5F
                }
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                    tvStatus.text = resources.getString(R.string.connecting)
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    icStatus.visibility = View.GONE
                    pbStatus.visibility = View.VISIBLE
                    btnScan.isEnabled = false
                    btnScan.alpha = 0.5F
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

}