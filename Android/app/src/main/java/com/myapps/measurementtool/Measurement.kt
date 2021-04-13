package com.myapps.measurementtool

import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

// Class untuk menampung nilai mesurement
@Parcelize
data class Measurement(

    var id: Int = 0,
    var title: String = "",
    var photo: String = "",
    var length: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var wheel_base: Double = 0.0,
    var foh: Double = 0.0,
    var roh: Double = 0.0,
    var approach_angle: Double = 0.0,
    var departure_angle: Double = 0.0,
    var date: Date = Calendar.getInstance().time

) : Parcelable {

    // Fungsi untuk proses validasi 
    fun validate(context: Context): ValidationResult {
        when {
            // Apakah Nilai title kosong
            this.title.isBlank() -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.title, false, context))
            }
            // Apakah nilai length kurang dari atau sama dengan nol
            this.length <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_length, true, context))
            }
            // Apakah nilai width kurang dari atau sama dengan nol
            this.width <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_width, true, context))
            }
            // Apakah nilai height kurang dari atau sama dengan nol
            this.height <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_height, true, context))
            }
            // Apakah nilai wheel_base kurang dari atau sama dengan nol
            this.wheel_base <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_wheel_base, true, context))
            }
            // Apakah nilai foh kurang dari atau sama dengan nol
            this.foh <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_foh, true, context))
            }
            // Apakah nilai roh kurang dari atau sama dengan nol
            this.roh <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_roh, true, context))
            }
            // Apakah nilai approach_angle kurang dari atau sama dengan nol
            this.approach_angle <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_approach_angle, true, context))
            }
            // Apakah nilai departure_angle kurang dari atau sama dengan nol
            this.departure_angle <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_departure_angle, true, context))
            }
            else -> {
                return ValidationResult(false, "OK")
            }
        }
    }

    // Fungsi untuk mendapatkan pesan error pada field yang kosong
    private fun getEmptyFieldErrorMsg(field_id: Int, isNumeric: Boolean, context: Context): String{
        val msg = if (!isNumeric)
            context.getString(R.string.error_msg_field_empty)
        else
            context.getString(R.string.error_msg_field_empty_or_less_zero)

        val fieldName: String = context.getString(field_id)
        return msg.replace("$", fieldName)

    }
}
