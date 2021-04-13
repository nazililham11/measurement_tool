package com.myapps.measurementtool

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.myapps.measurementtool.Utils.Companion.fullTrim
import com.myapps.measurementtool.Utils.Companion.toEditable
import java.net.URI
import java.util.*

// Class Activity/tampilan create/buat pengukuran baru
class CreateActivity : AppCompatActivity() {

    // ----------------------------------------------------------------------------------------
    // Deklarasi Variable, Object, Task, dll
    // ----------------------------------------------------------------------------------------

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


    // Task saat terjadi timeout 
    private val scanTimeoutTask = Runnable {
        if (isScanning){
            // Tampilkan pesan timeout
            Toast.makeText(this@CreateActivity, "Connection Timeout", Toast.LENGTH_LONG).show()

            // Set status menjadi disconect
            setConnectionStatus(ConnectionStatus.DISCONNECT)
            isScanning = false
            
            // Disable tombol scan
            setScanButtonEnable(false)
        }
    }





    // ----------------------------------------------------------------------------------------
    // Activity Events
    // ----------------------------------------------------------------------------------------


    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        sharePrefs = SharePrefs(this)                   // Inisialisasi share prefreces
        mainHandler = Handler(Looper.getMainLooper())   // Inisialisasi Handler (untuk multithreading)

        bindToUI()                  // Menghubungkan variable dengan element UI
        initHardwareProvider()      // Inisialisasi koneksi websocket dengan hardware
    }

    // Event saat activity dihentikan sementara(berpindah ke aplikasi lain misalnya)
    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(scanTimeoutTask)
    }

    // Event saat activity ditutup
    override fun onDestroy() {
        super.onDestroy()

        // Apabila data belum di save 
        if (!isSaved){
            Utils.deleteFileFromPath(capturedImage)     // Hapus foto apabila ada  
        }
        
        hardwareProvider.onDestroy()    // Tutup komunikasi dengan hardware
    }

    // Event saat mendapat hasil dari permintaan permission/izin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        onCameraPermissionResult(requestCode, grantResults)
    }

    // Event saat mendapatkan hasil foto
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onImageCaptured(resultCode)
    }





    // ----------------------------------------------------------------------------------------
    // Fungsi dekorasi UI
    // ----------------------------------------------------------------------------------------
    
    // Fungsi untuk menghubungkan variable dengan elemen pada UI (Tombol, label, gambar, dll)
    //      Dibutuhkan apabila kita ingin mengambil atau mengubah elemn pada UI
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

        // Tambahkan event pada saat tombol ditekan 
        btnSave.setOnClickListener { saveChanges() }
        btnScan.setOnClickListener { scanDistance() }
        btnTakePhoto.setOnClickListener { if (permissionCheck()) openCamera() }
        btnRemove.setOnClickListener { removePhoto() }
    }

    // Mengambil isi dari field dan memberikannya dalam bentuk object Measurement
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

    // Fungsi untuk mengubah UI sesuai dengan status koneksi 
    private fun setConnectionStatus(status: ConnectionStatus){
        runOnUiThread {
            when (status) {
                // Apabila status Connect
                ConnectionStatus.CONNECTED -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.green))       // Ganti backgorund
                    tvStatus.text = resources.getString(R.string.connected)                         // Ganti text status  
                    icStatus.setImageResource(R.drawable.ic_baseline_check_24)                      // Ganti icon
                    icStatus.visibility = View.VISIBLE                                              // Munculkan icon status
                    pbStatus.visibility = View.GONE                                                 // Hilangkan loading 
                    setScanButtonEnable(true)                                                       // Enable tombol scan 
                }

                // Apabila status Disconnect
                ConnectionStatus.DISCONNECT -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.red))         // Ganti background
                    tvStatus.text = resources.getString(R.string.disconnected)                      // Ganti text status
                    icStatus.setImageResource(R.drawable.ic_baseline_clear_24)                      // Ganti icon
                    icStatus.visibility = View.VISIBLE                                              // Munculkan icon status
                    pbStatus.visibility = View.GONE                                                 // Hilangkan loading 
                    setScanButtonEnable(false)                                                      // Disable tombol scan 
                }
                
                // Apabila status Connected
                ConnectionStatus.CONNECTING -> {
                    lytStatus.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))        // Ganti background
                    tvStatus.text = resources.getString(R.string.connecting)                        // Ganti text status
                    icStatus.visibility = View.GONE                                                 // Holangkan icon status
                    pbStatus.visibility = View.VISIBLE                                              // Munculkan loading 
                    setScanButtonEnable(false)                                                      // Disable tombol scan 
                }
            }
        }
    }

    // Fungsi untuk mengubah status tombol scan 
    private fun setScanButtonEnable(state: Boolean){
        btnScan.isEnabled = state                       // Ubah status tombol 
        btnScan.alpha = if (state) 1F else 0.5F         // Ubah opacity tombol
    }

    // Fungsi untuk mengubah visibility photo
    private fun setPhotoVisibility(state: Boolean){
        ivPhoto.visibility = if (state) View.VISIBLE else View.GONE         // Ubah visibility foto
        btnTakePhoto.visibility = if (state) View.GONE else View.VISIBLE    // Ubah visibility tombol take photo
        btnRemove.visibility = if (state) View.VISIBLE else View.GONE       // Ubah visibility tombol remove
    }

    // Variable lokasi gambar yang telah diambil
    private var capturedImage: String = ""
        set(value){

            // Apabila variable diubah menjadi bernilai blank
            if (value.isBlank()){
                setPhotoVisibility(false)   // Sembunyikan foto 

            // Apabila variable diubah menjadi bernilai blank
            } else {

                // Ambil foto dari lokasi path
                Utils.getBitmapFromPath(value)?.let { image ->

                    // Resize menjadi 500x500 dan tampilkan pada UI
                    ivPhoto.setImageBitmap(Utils.resizeBitmap(image, 500))
                    setPhotoVisibility(true)

                // Apabila gambar tidak ditemukan
                } ?: run {
                    Toast.makeText(this@CreateActivity, "Image Not Found", Toast.LENGTH_LONG).show()
                }
            }
            field = value
        }

    // Fungsi untuk mendapatkan field yang berupa angka 
    private fun numbersField(): Array<EditText> {
        return arrayOf(
            etLength,
            etWidth,
            etHeight,
            etWheelBase,
            etFOH,
            etROH,
            etApproachAngle,
            etDepartureAngle
        )
    }

    


    // ----------------------------------------------------------------------------------------
    // Fungsi dan Event untuk koneksi dengan hardware
    // ----------------------------------------------------------------------------------------
    
    // Fungsi inisialisasi koneksi dengan hardware
    private fun initHardwareProvider(){
        // Ubah status 
        setConnectionStatus(ConnectionStatus.CONNECTING)                            
        
        // Ambil hardware address(ip addres) dari shared preferences
        hardwareProvider = HardwareProvider(URI(sharePrefs.hardwareAddress))        
        
        // Hubungkan dengan hardware 
        hardwareProvider.reciveMessageCallback = { msg -> onMessageReceived(msg) }  
        hardwareProvider.connectionChangedCallback = { status -> setConnectionStatus(status) }
        hardwareProvider.connectionLostTimeout = 2500
        hardwareProvider.connect()
    }

    // Fungsi untuk meminta data jarak dari hardware
    private fun scanDistance(){
        setScanButtonEnable(false)                      // Disable tombol scan 
        isScanning = true
        mainHandler.postDelayed(scanTimeoutTask, 5000)  // Buat timeout selama 5000ms(5detik)
        hardwareProvider.scanDistance()                 // Kirimkan perintah scan
    }

    // Event saat menerima pesan dari hardware 
    private fun onMessageReceived(msg: String?){

        // Ambil nilai dari jarak dari pesan
        val value = msg?.replace("Distance: ", "")?.trim()?.toDoubleOrNull() ?: 0.0

        runOnUiThread {
            // Apabila masih melakukan proses scan 
            if (isScanning){
                // Hentikan tampilan proses scan
                mainHandler.removeCallbacks(scanTimeoutTask)   
                isScanning = false                      
                setScanButtonEnable(true)                       
            }

            // Isihkan nilai jarak ke field angka yang sednag aktif/focused
            for (field in numbersField()) {
                if (field.isFocused){
                    field.text = value.toString().fullTrim().toEditable()
                    break
                }
            }
        }
    }

    
    
    
    // ----------------------------------------------------------------------------------------
    // Fungsi untuk koneksi dengan database
    // ----------------------------------------------------------------------------------------

    // Fungsi untuk menyimpan data 
    private fun saveChanges(){
        val measurement = getFieldValues()                  // Ambil isi dari field
        val validation = measurement.validate(this)         // Validasi isi field 

        // Apabla terdapat error pada field
        if (validation.isError){
            Toast.makeText(this@CreateActivity, validation.errorMessage, Toast.LENGTH_LONG).show()

        // Apabla tidak ada error
        } else {
            // Simpan dalam databae 
            val measurementsProvider = MeasurementsProvider(this)
            val result = measurementsProvider.insert(measurement)   

            // Apabila proses simpan berhasil
            if (result) {
                val message = getString(R.string.toast_msg_create_completed).replace("$", measurement.title)
                Toast.makeText(this@CreateActivity, message, Toast.LENGTH_SHORT).show()
                isSaved = true
                finish()
            }
        }
    }





    // ----------------------------------------------------------------------------------------
    // Fungsi, Event, Variable yang berhubungan dengan foto dan kamera
    // ----------------------------------------------------------------------------------------

    // Fungsi untuk menghapus foto
    private fun removePhoto(){
        Utils.deleteFileFromPath(capturedImage)     
        capturedImage = ""                          
    }

    // Fungsi untuk membuka kamera
    private fun openCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    // Fungsi untuk mengecek permission/izin untuk menggunakan kamera dan melakukan tulis ke storage 
    private fun permissionCheck(): Boolean {

        // Cek Versi SDK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Cek permission camera dan write to storage 
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permission, PERMISSION_CODE)
                return false
            }
        }
        return true
    }

    // Fungsi setelah menerima hasil dari permission 
    private fun onCameraPermissionResult(requestCode: Int, grantResults: IntArray) {
        // Apabila kode permission sesuai
        if (requestCode == PERMISSION_CODE){
            // Apabila permission dioerbolehkan  
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Buka Kamera
                    openCamera()

            // Apabila permission ditolak
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi setelah proses pengambilan gambar
    private fun onImageCaptured(resultCode: Int) {
        // Apabila pengambilan gambar berhasil 
        if (resultCode == Activity.RESULT_OK){
            // Ambil image path dan isihkan path pada variable capturedImage
            imageUri?.let { uri ->
                val path = Utils.getRealPathFromURI(this, uri)
                path?.let { imagePath -> capturedImage = imagePath }
            }
        }
    }


}

