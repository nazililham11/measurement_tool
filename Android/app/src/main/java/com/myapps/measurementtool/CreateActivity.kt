package com.myapps.measurementtool

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.net.URI
import java.util.*

class CreateActivity : AppCompatActivity() {


    // Connection Bar
    private lateinit var tvStatus:          TextView
    private lateinit var tvStatusSummary:   TextView
    private lateinit var icStatus:          ImageView
    private lateinit var lytStatus:         LinearLayout
    private lateinit var pbStatus:          ProgressBar
    private lateinit var btnScan:           Button
    
    // Fields
    private lateinit var etTitle:           EditText
    private lateinit var etLength:          EditText
    private lateinit var etWidth:           EditText
    private lateinit var etHeight:          EditText
    private lateinit var etWheelBase:       EditText
    private lateinit var etFOH:             EditText
    private lateinit var etROH:             EditText
    private lateinit var etApproachAngle:   EditText
    private lateinit var etDepartureAngle:  EditText

    // Buttons
    private lateinit var btnSave:           Button

    // Photo
    private lateinit var btnTakePhoto:      Button
    private lateinit var btnRemove:         Button
    private lateinit var ivPhoto:           ImageView

    // Utils
    private lateinit var hardwareProvider:  HardwareProvider
    private lateinit var sharePrefs:        SharePrefs
    private lateinit var mainHandler:       Handler


    private var isScanning: Boolean = false
    private var imageUri: Uri? = null
    private val PERMISSION_CODE = 1000
    private val IMAGE_CAPTURE_CODE = 1001
    private var isSaved = false

    private val scanTimeoutTask = Runnable {
        if (isScanning){
            Toast.makeText(this@CreateActivity, "Connection Timeout", Toast.LENGTH_LONG).show()
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            setScanButtonEnable(false)
        }
    }


    // Override Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        sharePrefs = SharePrefs(this)
        mainHandler = Handler(Looper.getMainLooper())

        bindToUI()
        initHardwareProvider()
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(scanTimeoutTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSaved){
            deleteFileFromPath(capturedImage)
        }
        hardwareProvider.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        onCameraPermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onImageCaptured(requestCode, resultCode, data)
    }




    // UI Methods
    private fun bindToUI(){
        lytStatus           = findViewById(R.id.ly_status)
        tvStatus            = findViewById(R.id.tv_status)
        tvStatusSummary     = findViewById(R.id.tv_summary)
        icStatus            = findViewById(R.id.ic_status)
        pbStatus            = findViewById(R.id.pb_status)
        
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

        btnTakePhoto        = findViewById(R.id.btn_take_photo)
        btnRemove           = findViewById(R.id.btn_remove)
        ivPhoto             = findViewById(R.id.iv_photo)

        tvStatusSummary.text = sharePrefs.hardwareAddress

        btnSave.setOnClickListener { saveChanges() }
        btnScan.setOnClickListener { scanDistance() }
        btnTakePhoto.setOnClickListener { if (permissionCheck()) openCamera() }
        btnRemove.setOnClickListener { removePhoto() }
    }

    private fun getFieldValues(): Measurement {
        return Measurement(
            title           = etTitle.text.toString(),
            photo           = capturedImage,
            length          = etLength.text.toString().toDoubleOrNull() ?: 0.0,
            width           = etWidth.text.toString().toDoubleOrNull() ?: 0.0,
            height          = etHeight.text.toString().toDoubleOrNull() ?: 0.0,
            wheel_base      = etWheelBase.text.toString().toDoubleOrNull() ?: 0.0,
            foh             = etFOH.text.toString().toDoubleOrNull() ?: 0.0,
            roh             = etROH.text.toString().toDoubleOrNull() ?: 0.0,
            approach_angle  = etApproachAngle.text.toString().toDoubleOrNull() ?: 0.0,
            departure_angle = etDepartureAngle.text.toString().toDoubleOrNull() ?: 0.0,
            date            = Calendar.getInstance().time
        )
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
                    setScanButtonEnable(true)
                }
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    tvStatus.text = resources.getString(R.string.disconnected)
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)
                    icStatus.visibility = View.VISIBLE
                    pbStatus.visibility = View.GONE
                    setScanButtonEnable(false)
                }
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
                    tvStatus.text = resources.getString(R.string.connecting)
                    icStatus.visibility = View.GONE
                    pbStatus.visibility = View.VISIBLE
                    setScanButtonEnable(false)
                }
            }
        }
    }

    private fun setScanButtonEnable(state: Boolean){
        btnScan.isEnabled = state
        btnScan.alpha = if (state) 1F else 0.5F
    }

    private fun setPhotoVisibility(state: Boolean){
        ivPhoto.visibility = if (state) View.VISIBLE else View.GONE
        btnTakePhoto.visibility = if (state) View.GONE else View.VISIBLE
        btnRemove.visibility = if (state) View.VISIBLE else View.GONE
    }

    private var capturedImage: String = ""
        set(value){
            if (value.isBlank()){
                setPhotoVisibility(false)
            } else {
                getBitmapFromPath(value)?.let { image ->
                    ivPhoto.setImageBitmap(resizeBitmap(image, 500))
                    setPhotoVisibility(true)
                } ?: run {
                    Toast.makeText(this@CreateActivity, "Image Not Found", Toast.LENGTH_LONG).show()
                }
            }
            Log.d("PhotoStuff", "Change capturedPhoto \nFrom:$field\nTo:$value")
            field = value
        }



    // Hardware Connection Methods
    private fun initHardwareProvider(){
        setConnectionStatus(ConnectionStatus.CONNECTING)
        hardwareProvider = HardwareProvider(URI(sharePrefs.hardwareAddress))
        hardwareProvider.reciveMessageCallback = { msg -> onMessageReceived(msg) }
        hardwareProvider.connectionChangedCallback = { status -> setConnectionStatus(status) }
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()
    }

    private fun scanDistance(){
        setScanButtonEnable(false)
        isScanning = true
        mainHandler.postDelayed(scanTimeoutTask, 5000)
        hardwareProvider.scanDistance()
    }

    private fun onMessageReceived(msg: String?){
        val value = msg?.replace("Distance: ", "")?.trim()?.toDoubleOrNull() ?: 0.0

        runOnUiThread {
            if (isScanning){
                mainHandler.removeCallbacks(scanTimeoutTask)
                isScanning = false
                setScanButtonEnable(true)
            }
            for (field in numbersField()) {
                if (field.isFocused){
                    field.text = value.toString().fullTrim().toEditable()
                    break
                }
            }
        }
    }

    

    // Database
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
                isSaved = true
                finish()
            }
        }
    }



    // Camera Methods
    private fun removePhoto(){
        deleteFileFromPath(capturedImage)
        capturedImage = ""
    }

    private fun openCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    private fun permissionCheck(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permission, PERMISSION_CODE)
                return false
            }
        }
        return true
    }

    private fun onCameraPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE){
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onImageCaptured(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK){
            imageUri?.let { uri ->
                val path = getRealPathFromURI(this, uri)
                path?.let { imagePath ->
                    capturedImage = imagePath
                    Log.d("Image Path", imagePath)
                }
            }
        }
    }




    // Helpers
    private fun String.fullTrim() = trim().replace("\uFEFF", "")
    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
    private fun numbersField(): Array<EditText> = arrayOf(etLength, etWidth, etHeight, etWheelBase, etFOH, etROH, etApproachAngle, etDepartureAngle)
    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(uri, proj, null, null, null)
            if (cursor != null){
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }
    private fun getBitmapFromPath(path: String?): Bitmap? {
        if (!path.isNullOrBlank()){
            val imgFile = File(path)
            if(imgFile.exists()) {
                return BitmapFactory.decodeFile(imgFile.absolutePath)
            }
        }
        return null
    }
    private fun deleteFileFromPath(path: String?): Boolean {
        if (!path.isNullOrBlank()){
            val file = File(path)
            if (file.exists())
                return file.delete()
        }
        return false
    }
    private fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
        try {
            if (source.height >= source.width) {
                if (source.height <= maxLength) { // if image height already smaller than the required height
                    return source
                }

                val aspectRatio = source.width.toDouble() / source.height.toDouble()
                val targetWidth = (maxLength * aspectRatio).toInt()
                return Bitmap.createScaledBitmap(source, targetWidth, maxLength, false)
            } else {
                if (source.width <= maxLength) { // if image width already smaller than the required width
                    return source
                }

                val aspectRatio = source.height.toDouble() / source.width.toDouble()
                val targetHeight = (maxLength * aspectRatio).toInt()

                return Bitmap.createScaledBitmap(source, maxLength, targetHeight, false)
            }
        } catch (e: Exception) {
            return source
        }
    }
}

