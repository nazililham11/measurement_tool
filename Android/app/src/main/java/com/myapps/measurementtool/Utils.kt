package com.myapps.measurementtool

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import java.io.File

// Class utility/utilitas
class Utils {
    companion object {

        // Fungsi untuk mengubah string untuk membantu proses konversi data
        fun String.fullTrim() = trim().replace("\uFEFF", "")

        // Fungsi untuk mengubah string ke field editable 
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        // Fungsi untuk memndapatkan path dari URI
        fun getRealPathFromURI(context: Context, uri: Uri): String? {
            var cursor: Cursor? = null
            try {
                val projection = arrayOf(MediaStore.Images.Media.DATA)
                cursor = context.contentResolver.query(uri, projection, null, null, null)
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

        // Fungsi untuk mengambil bitmap/gambar berdasarkan path
        fun getBitmapFromPath(path: String?): Bitmap? {

            // Apakah path bernilai kosong
            if (!path.isNullOrBlank()){
                val imgFile = File(path)
                
                // Apakah file ada  
                if(imgFile.exists()) {
                
                    // Ambil gambar
                    return BitmapFactory.decodeFile(imgFile.absolutePath)
                }
            }
            return null
        }

        // Fungsi untuk menghapus gambar berdasarkan path
        fun deleteFileFromPath(path: String?): Boolean {
            
            // Apakah path bernilai kosong
            if (!path.isNullOrBlank()){
                val file = File(path)

                // Apakah file ada  
                if (file.exists())
                
                    // Hapus file
                    return file.delete()
            }
            return false
        }

        // Fungsi untuk mengubah (memperkecil) ukuran gambar
        fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
            try {
                // Apakah tinggi lebih dari lebar 
                if (source.height >= source.width) {
                    
                    // Apakah tinggi kurang dari lebar max
                    //   Gambar telah lebih kecil dari ukuran yang diinginkan
                    if (source.height <= maxLength) { 
                        // Keluar dari fungsi dan berikan gambar awal
                        return source
                    }
                    // Hitung lebar target berdasarkan rasio
                    val aspectRatio = source.width.toDouble() / source.height.toDouble()
                    val targetWidth = (maxLength * aspectRatio).toInt()

                    // Resize gambar dan keluar dari fungsi 
                    return Bitmap.createScaledBitmap(source, targetWidth, maxLength, false)

                
                // Apakah tinggi kurang dari lebar 
                } else {
                    
                    // Apakah lebar kurang dari lebar max 
                    if (source.width <= maxLength) { 
                        // Keluar dari fungsi dan berikan gambar awal
                        return source
                    }

                    // Hitung lebar target berdasarkan rasio
                    val aspectRatio = source.height.toDouble() / source.width.toDouble()
                    val targetHeight = (maxLength * aspectRatio).toInt()

                    // Resize gambar dan keluar dari fungsi 
                    return Bitmap.createScaledBitmap(source, maxLength, targetHeight, false)
                }
            
            // Apabila terdapat error 
            } catch (e: Exception) {
                // Keluar dari fungsi dan berikan gambar awal
                return source
            }
        }
    }

}