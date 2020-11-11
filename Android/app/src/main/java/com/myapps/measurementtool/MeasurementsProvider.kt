package com.myapps.measurementtool

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*
import kotlin.collections.ArrayList

class MeasurementsProvider(context: Context)
    : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION)
{

    companion object {
        private const val DB_VERSION 		  = 1
        private const val DB_NAME 			  = "dataset"
        private const val TABLE_NAME 		  = "measurements"
        private const val COL_ID 			  = "_id"
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


    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                "$COL_ID              INTEGER PRIMARY KEY," +
                "$COL_TITLE           TEXT," +
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
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
        db.execSQL(DROP_TABLE)
        onCreate(db)
    }

    fun insert(measurement: Measurement): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_TITLE,           measurement.title)
        values.put(COL_LENGTH,          measurement.length)
        values.put(COL_WIDTH,           measurement.width)
        values.put(COL_HEIGHT,          measurement.height)
        values.put(COL_WHEEL_BASE, 		measurement.wheel_base)
        values.put(COL_FOH, 			measurement.foh)
        values.put(COL_ROH, 			measurement.roh)
        values.put(COL_APPROACH_ANGLE, 	measurement.approach_angle)
        values.put(COL_DEPARTURE_ANGLE, measurement.departure_angle)
        values.put(COL_DATE, 			measurement.date.time)

        val _success = db.insert(TABLE_NAME, null, values)
        db.close()
        return (Integer.parseInt("$_success") != -1)
    }

    fun String.fullTrim() = trim().replace("\uFEFF", "")

    fun readById(_id: Int): Measurement {
        val measurement = Measurement()
        val db = writableDatabase
        val selectQuery = "SELECT  * FROM $TABLE_NAME WHERE $COL_ID = $_id"
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                measurement.id              = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_ID)))
                measurement.title           = cursor.getString(cursor.getColumnIndex(COL_TITLE))
                measurement.length          = cursor.getString(cursor.getColumnIndex(COL_LENGTH)).fullTrim().toDouble()
                measurement.width           = cursor.getString(cursor.getColumnIndex(COL_WIDTH)).fullTrim().toDouble()
                measurement.height          = cursor.getString(cursor.getColumnIndex(COL_HEIGHT)).fullTrim().toDouble()
                measurement.wheel_base      = cursor.getString(cursor.getColumnIndex(COL_WHEEL_BASE)).fullTrim().toDouble()
                measurement.foh             = cursor.getString(cursor.getColumnIndex(COL_FOH)).fullTrim().toDouble()
                measurement.roh             = cursor.getString(cursor.getColumnIndex(COL_ROH)).fullTrim().toDouble()
                measurement.approach_angle  = cursor.getString(cursor.getColumnIndex(COL_APPROACH_ANGLE)).fullTrim().toDouble()
                measurement.departure_angle = cursor.getString(cursor.getColumnIndex(COL_DEPARTURE_ANGLE)).fullTrim().toDouble()
                measurement.date            = Date(cursor.getString(cursor.getColumnIndex(COL_DATE)).toLong())
            }
        }
        cursor.close()
        return measurement
    }

    val read: ArrayList<Measurement>
        get() {
            val measurementList = ArrayList<Measurement>()
            val db = writableDatabase
            val selectQuery = "SELECT  * FROM $TABLE_NAME"
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val measurement = Measurement()
                    measurement.id              = Integer.parseInt(cursor.getString(cursor.getColumnIndex(COL_ID)))
                    measurement.title           = cursor.getString(cursor.getColumnIndex(COL_TITLE))
                    measurement.length          = cursor.getString(cursor.getColumnIndex(COL_LENGTH)).fullTrim().toDouble()
                    measurement.width           = cursor.getString(cursor.getColumnIndex(COL_WIDTH)).fullTrim().toDouble()
                    measurement.height          = cursor.getString(cursor.getColumnIndex(COL_HEIGHT)).fullTrim().toDouble()
                    measurement.wheel_base      = cursor.getString(cursor.getColumnIndex(COL_WHEEL_BASE)).fullTrim().toDouble()
                    measurement.foh             = cursor.getString(cursor.getColumnIndex(COL_FOH)).fullTrim().toDouble()
                    measurement.roh             = cursor.getString(cursor.getColumnIndex(COL_ROH)).fullTrim().toDouble()
                    measurement.approach_angle  = cursor.getString(cursor.getColumnIndex(COL_APPROACH_ANGLE)).fullTrim().toDouble()
                    measurement.departure_angle = cursor.getString(cursor.getColumnIndex(COL_DEPARTURE_ANGLE)).fullTrim().toDouble()
                    measurement.date            = Date(cursor.getString(cursor.getColumnIndex(COL_DATE)).toLong())
                    measurementList.add(measurement)
                }
            }
            cursor.close()
            return measurementList
    }

    fun update(measurement: Measurement): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_TITLE,           measurement.title)
        values.put(COL_LENGTH,          measurement.length)
        values.put(COL_WIDTH,           measurement.width)
        values.put(COL_HEIGHT,          measurement.height)
        values.put(COL_WHEEL_BASE, 		measurement.wheel_base)
        values.put(COL_FOH, 			measurement.foh)
        values.put(COL_ROH, 			measurement.roh)
        values.put(COL_APPROACH_ANGLE, 	measurement.approach_angle)
        values.put(COL_DEPARTURE_ANGLE, measurement.departure_angle)
        values.put(COL_DATE, 			measurement.date.time)

        val _success = db.update(TABLE_NAME, values, "$COL_ID=?", arrayOf(measurement.id.toString())).toLong()
        db.close()
        return Integer.parseInt("$_success") != -1
    }

    fun delete(_id: Int): Boolean {
        val db = this.writableDatabase
        val _success = db.delete(TABLE_NAME, "$COL_ID=?", arrayOf(_id.toString())).toLong()
        db.close()
        return Integer.parseInt("$_success") != -1
    }

}