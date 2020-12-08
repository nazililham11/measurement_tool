package com.myapps.measurementtool

import android.Manifest
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
import android.provider.MediaStore
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
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

    private lateinit var btnRemovePhoto:    Button
    private lateinit var btnTakePhoto:      Button
    private lateinit var btnBrowsePhoto:    Button
    private lateinit var btnScan:           Button
    private lateinit var tvStatus:          TextView
    private lateinit var tvStatusSummary:   TextView
    private lateinit var icStatus:          ImageView
    private lateinit var ivPhoto:           ImageView
    private lateinit var lytStatus:         LinearLayout
    private lateinit var pbStatus:          ProgressBar

    private lateinit var hardwareProvider:  HardwareProvider
    private lateinit var numbersField:      Array<EditText>
    private lateinit var sharePrefs:        SharePrefs
    private lateinit var mainHandler:       Handler


    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001

    private var imagePath: String = ""
    private var imageUri: Uri? = null
    private var isScanning = false
    private var photoCaptured = false
    private var saved = false

    private val scanTimeoutTask = Runnable {
        if (isScanning){
            Toast.makeText(this@CreateActivity, "Connection Timeout", Toast.LENGTH_LONG).show()
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            setScanButtonEnable(false)
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
        hardwareProvider.reciveMessageCallback = { msg -> onMessageRecived(msg) }
        hardwareProvider.connectionChangedCallback = { status -> setConnectionStatus(status) }
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(scanTimeoutTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (photoCaptured && !saved)
            deleteSavedPhoto(imagePath)

//        hardwareProvider.onDestroy()
    }



    private fun onMessageRecived(msg: String?){
        val stringValue = msg?.replace("Distance: ", "")?.trim()
        val value: Double = stringValue?.fullTrim()?.toDouble() ?: 0.0

        runOnUiThread {
            if (isScanning){
                mainHandler.removeCallbacks(scanTimeoutTask)
                isScanning = false
                setScanButtonEnable(true)
            }
            for (field in numbersField) {
                if (field.isFocused){
                    field.text = value.toString().fullTrim().toEditable()
                    break
                }
            }
        }
    }

    private fun initHardwareProvider(){

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
                    setScanButtonEnable(true)
                }
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    tvStatus.text = resources.getString(R.string.disconnected)
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    setScanButtonEnable(false)
                }
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                    tvStatusSummary.text = sharePrefs.hardwareAddress
                    tvStatus.text = resources.getString(R.string.connecting)
                    icStatus.visibility = View.GONE
                    pbStatus.visibility = View.VISIBLE
                    setScanButtonEnable(false)
                }
            }
        }
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
        ivPhoto             = findViewById(R.id.iv_photo)
        btnRemovePhoto      = findViewById(R.id.btn_remove)
        btnTakePhoto        = findViewById(R.id.btn_take_photo)
        btnBrowsePhoto      = findViewById(R.id.btn_browse)
        lytStatus           = findViewById(R.id.ly_status)
        icStatus            = findViewById(R.id.ic_status)
        tvStatus            = findViewById(R.id.tv_status)
        tvStatusSummary     = findViewById(R.id.tv_summary)
        pbStatus            = findViewById(R.id.pb_status)
        numbersField        = arrayOf(etLength, etWidth, etHeight, etWheelBase, etFOH, etROH, etApproachAngle, etDepartureAngle)

        btnSave.setOnClickListener { saveChanges() }
        btnScan.setOnClickListener { scanDevice() }
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnRemovePhoto.setOnClickListener { showDeletePhotoDialog() }
    }

    private fun saveChanges(){
        val measurement= getFieldValues()
        val validation = measurement.validate(this)

        if (validation.isError){
            Toast.makeText(this@CreateActivity, validation.errorMessage, Toast.LENGTH_LONG).show()
        } else {
            val measurementsProvider = MeasurementsProvider(this)
            val result = measurementsProvider.insert(measurement)
            if (result) {
                val message = getString(R.string.toast_msg_create_completed).replace("$", measurement.title)
                Toast.makeText(this@CreateActivity, message, Toast.LENGTH_SHORT).show()
                saved = true
                finish()
            }
        }
    }

    private fun scanDevice(){
        setScanButtonEnable(false)
        isScanning = true
        mainHandler.postDelayed(scanTimeoutTask, 5000)
        hardwareProvider.scanDistance()
    }

    private fun takePhoto(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permission, PERMISSION_CODE)
                return
            }
        }
        openCamera()
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (imageUri != null){
                imageUri?.let { imagePath = getFilePath(it).toString() }
                val image = getBitmapFromPath(imagePath)
                photoCaptured = image != null
                image?.let { ivPhoto.setImageBitmap(it) }
                photoCaptured = true
                setPhotoVisibility(true)
                Toast.makeText(this@CreateActivity, "Image Captured", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setPhotoVisibility(state: Boolean){
        if (state){
            ivPhoto.visibility = View.VISIBLE
            btnRemovePhoto.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.GONE
        } else {
            ivPhoto.visibility = View.GONE
            btnRemovePhoto.visibility = View.GONE
            btnTakePhoto.visibility = View.VISIBLE
        }
    }

    private fun showDeletePhotoDialog(){
        val dialogBuilder = AlertDialog.Builder(this)
        val yesLabel = getString(R.string.btn_yes)
        val cancelLabel = getString(R.string.btn_cancel)
        val title = "Hapus Foto"
        val question = "Menghapus foto akan menghapus foto secara permanen\nApakah anda yakin?"

        dialogBuilder.setMessage(question)
            .setCancelable(false)
            .setPositiveButton(yesLabel) { _, _ ->
                if (deleteSavedPhoto(imageUri)){
                    setPhotoVisibility(false)
                }
            }
            .setNegativeButton(cancelLabel) { dialog, _ -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle(title)
        alert.show()
    }


    private fun getFieldValues(): Measurement {
        val photoPath = if (photoCaptured && imageUri != null) getFilePath(imageUri!!) else ""
        return Measurement(
            title           = etTitle.text.toString(),
            photo           = photoPath ?: "",
            length          = getFieldDoubleValue(etLength),
            width           = getFieldDoubleValue(etWidth),
            height          = getFieldDoubleValue(etHeight),
            wheel_base      = getFieldDoubleValue(etWheelBase),
            foh             = getFieldDoubleValue(etFOH),
            roh             = getFieldDoubleValue(etROH),
            approach_angle  = getFieldDoubleValue(etApproachAngle),
            departure_angle = getFieldDoubleValue(etDepartureAngle),
            date            = Calendar.getInstance().time
        )
    }

    private fun setScanButtonEnable(state: Boolean){
        if (state){
            btnScan.isEnabled = true
            btnScan.alpha = 1F
        } else {
            btnScan.isEnabled = false
            btnScan.alpha = 0.5F
        }
    }

    // Helpers
    private fun String.fullTrim() = trim().replace("\uFEFF", "")
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private fun getFilePath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(projection[0])
            val picturePath: String = cursor.getString(columnIndex) // returns null
            cursor.close()
            return picturePath
        }
        return null
    }
    private fun getFieldDoubleValue(field: EditText): Double {
        val text = field.text.toString()
        if (text.isNullOrBlank())
            return 0.0
        return text.fullTrim().toDouble()
    }
    private fun deleteSavedPhoto(path: String): Boolean{
        if (!path.isNullOrBlank()){
            val fdelete = File(path)
            if (fdelete.exists())
                return fdelete.delete()
        }
        return false
    }
    private fun deleteSavedPhoto(uri: Uri?): Boolean {
        uri?.let {
            return deleteSavedPhoto(getFilePath(it).toString())
        }
        return false
    }

    private fun getBitmapFromPath(path: String): Bitmap?{
        if (!path.isNullOrBlank()){
            val imgFile = File(path)
            if(imgFile.exists()) {
                return BitmapFactory.decodeFile(imgFile.absolutePath)
            }
        }
        return null
    }

}

