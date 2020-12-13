package com.myapps.measurementtool

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import java.io.File

class Utils {
    companion object {

        fun String.fullTrim() = trim().replace("\uFEFF", "")
        fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

        fun getRealPathFromURI(context: Context, uri: Uri): String? {
            var cursor: Cursor? = null
            try {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null){
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    cursor.moveToFirst()
                    return cursor.getString(columnIndex)
                }
            } finally {
                cursor?.close()
            }
            return null
        }
        fun getBitmapFromPath(path: String?): Bitmap? {
            if (!path.isNullOrBlank()){
                val imgFile = File(path)
                if(imgFile.exists()) {
                    return BitmapFactory.decodeFile(imgFile.absolutePath)
                }
            }
            return null
        }
        fun deleteFileFromPath(path: String?): Boolean {
            if (!path.isNullOrBlank()){
                val file = File(path)
                if (file.exists())
                    return file.delete()
            }
            return false
        }
        fun resizeBitmap(source: Bitmap, maxLength: Int): Bitmap {
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

}