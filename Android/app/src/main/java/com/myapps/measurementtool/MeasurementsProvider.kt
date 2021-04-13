package com.myapps.measurementtool

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*
import kotlin.collections.ArrayList

class MeasurementsProvider(context: Context)
    : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION)
{

    // ----------------------------------------------------------------------------------------
    // Konstanta
    // ----------------------------------------------------------------------------------------
    
    // Konstanta database
    companion object {
        private const val DB_VERSION 		  = 1
        private const val DB_NAME 			  = "dataset"
        private const val TABLE_NAME 		  = "measurements"
        
        // Nama tiap kolom pada database
        private const val COL_ID 			  = "_id"
        private const val COL_PHOTO 		  = "photo"
        private const val COL_TITLE 		  = "title"
        private const val COL_LENGTH 		  = "length"
        private const val COL_WIDTH 		  = "width"
        private const val COL_HEIGHT 		  = "height"
        private const val COL_WHEEL_BASE 	  = "wheel_base"
        private const val COL_FOH 			  = "foh"
        private const val COL_ROH 			  = "roh"
        private const val COL_APPROACH_ANGLE  = "approach_angle"
        private const val COL_DEPARTURE_ANGLE = "departure_angle"
        private const val COL_DATE 			  = "date"
    }


    
    // ----------------------------------------------------------------------------------------
    // Event
    // ----------------------------------------------------------------------------------------
    

    // Event saat membuat database baru
    override fun onCreate(db: SQLiteDatabase) {
        // Query untuk database baru
        val createTable = "CREATE TABLE $TABLE_NAME (" +
                "$COL_ID              INTEGER PRIMARY KEY," +
                "$COL_TITLE           TEXT," +
                "$COL_PHOTO           TEXT," +
                "$COL_LENGTH          DOUBLE," +
                "$COL_WIDTH           DOUBLE," +
                "$COL_HEIGHT          DOUBLE," +
                "$COL_WHEEL_BASE 	  DOUBLE," +
                "$COL_FOH 			  DOUBLE," +
                "$COL_ROH 			  DOUBLE," +
                "$COL_APPROACH_ANGLE  DOUBLE," +
                "$COL_DEPARTURE_ANGLE DOUBLE," +
                "$COL_DATE 			  LONG" +
                ");"
        // Eksekusi Query
        db.execSQL(createTable)
    }

    // Event saat memperbarui database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Hapus database 
        val dropTable = "DROP TABLE IF EXISTS $TABLE_NAME"
        db.execSQL(dropTable)

        // Buat database baru
        onCreate(db)
    }



    // ----------------------------------------------------------------------------------------
    // CRUD Database (Create, Read, Update, Delete)
    // ----------------------------------------------------------------------------------------
    

    // Fungsi untuk memasukan data baru 
    fun insert(measurement: Measurement): Boolean {
        // Masukan data
        val db = this.writableDatabase
        val values = getContentValues(measurement)
        val result = db.insert(TABLE_NAME, null, values)

        // Tutup koneksi dengan database
        db.close()
        return (Integer.parseInt("$result") != -1)
    }

    // Mengambil data berdasarkan id
    fun readById(_id: Int): Measurement {
        var measurement = Measurement()
        // Buat query
        val selectQuery = "SELECT  * FROM $TABLE_NAME WHERE $COL_ID = $_id"
        // Ambil data 
        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        try {
            // Apabila data ditemukan 
            if (cursor != null) {
                // Ambil semua data yang muncul
                while (cursor.moveToNext()) {
                    measurement = this.readDataFromCursor(cursor)
                }
            }
        } finally {
            // Tutup kokesi dengan database 
            cursor.close()
            db.close()
        }
        return measurement
    }

    // Ambil semua data
    fun read(): ArrayList<Measurement> {
        val measurementList = ArrayList<Measurement>()
        // Buat query 
        val selectQuery = "SELECT  * FROM $TABLE_NAME"
        // Ambil data
        val db = writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        try {
            // Apabila terdapat data  
            if (cursor != null) {
                // Ambil semua data yang muncul
                while (cursor.moveToNext()) {
                    val measurement = this.readDataFromCursor(cursor)
                    measurementList.add(measurement)
                }
            }
        } finally {
            // Tutup kokesi dengan database 
            cursor.close()
            db.close()
        }
        return measurementList
    }

    // Fungsi untuk memperbarui salah satu data
    fun update(measurement: Measurement): Boolean {
        val db = this.writableDatabase
        val values = this.getContentValues(measurement)
        val result = db.update(TABLE_NAME, values, "$COL_ID=?", arrayOf(measurement.id.toString())).toLong()
        db.close()
        return Integer.parseInt("$result") != -1
    }

    // Fungsi untuk menghapus data berdasarkan id
    fun delete(_id: Int): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$COL_ID=?", arrayOf(_id.toString())).toLong()
        db.close()
        return Integer.parseInt("$result") != -1
    }


    // ----------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------
    

    // Fungsi untuk mengisikan kolom tabel dengan variable pada objek Measurement 
    private fun getContentValues(measurement: Measurement): ContentValues {
        val values = ContentValues()
        values.put(COL_TITLE,           measurement.title)
        values.put(COL_PHOTO,           measurement.photo)
        values.put(COL_LENGTH,          measurement.length)
        values.put(COL_WIDTH,           measurement.width)
        values.put(COL_HEIGHT,          measurement.height)
        values.put(COL_WHEEL_BASE, 		measurement.wheel_base)
        values.put(COL_FOH, 			measurement.foh)
        values.put(COL_ROH, 			measurement.roh)
        values.put(COL_APPROACH_ANGLE, 	measurement.approach_angle)
        values.put(COL_DEPARTURE_ANGLE, measurement.departure_angle)
        values.put(COL_DATE, 			measurement.date.time)

        return values
    }

    // Fungsi untuk mengambil data dari tiap kolom dan mengisikannnya ke variable pada objek Measurement
    private fun readDataFromCursor(cursor: Cursor): Measurement {
        val measurement = Measurement()
        measurement.id              = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_ID)))
        measurement.title           = cursor.getString(cursor.getColumnIndex(COL_TITLE))
        measurement.photo           = cursor.getString(cursor.getColumnIndex(COL_PHOTO))
        measurement.length          = cursor.getString(cursor.getColumnIndex(COL_LENGTH)).fullTrim().toDouble()
        measurement.width           = cursor.getString(cursor.getColumnIndex(COL_WIDTH)).fullTrim().toDouble()
        measurement.height          = cursor.getString(cursor.getColumnIndex(COL_HEIGHT)).fullTrim().toDouble()
        measurement.wheel_base      = cursor.getString(cursor.getColumnIndex(COL_WHEEL_BASE)).fullTrim().toDouble()
        measurement.foh             = cursor.getString(cursor.getColumnIndex(COL_FOH)).fullTrim().toDouble()
        measurement.roh             = cursor.getString(cursor.getColumnIndex(COL_ROH)).fullTrim().toDouble()
        measurement.approach_angle  = cursor.getString(cursor.getColumnIndex(COL_APPROACH_ANGLE)).fullTrim().toDouble()
        measurement.departure_angle = cursor.getString(cursor.getColumnIndex(COL_DEPARTURE_ANGLE)).fullTrim().toDouble()
        measurement.date            = Date(cursor.getString(cursor.getColumnIndex(COL_DATE)).toLong())
        return measurement
    }


    // Fungsi untuk membantu saat proses konversi data
    private fun String.fullTrim() = trim().replace("\uFEFF", "")

}