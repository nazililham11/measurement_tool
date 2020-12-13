package com.myapps.measurementtool

import android.content.Context
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

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

    fun validate(context: Context): ValidationResult {
        when {
            this.title.isBlank() -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.title, false, context))
            }
            this.length <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_length, true, context))
            }
            this.width <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_width, true, context))
            }
            this.height <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_height, true, context))
            }
            this.wheel_base <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_wheel_base, true, context))
            }
            this.foh <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_foh, true, context))
            }
            this.roh <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_roh, true, context))
            }
            this.approach_angle <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_approach_angle, true, context))
            }
            this.departure_angle <= 0.0 -> {
                return ValidationResult(true, getEmptyFieldErrorMsg(R.string.hint_departure_angle, true, context))
            }
            else -> {
                return ValidationResult(false, "OK")
            }
        }
    }

    private fun getEmptyFieldErrorMsg(field_id: Int, isNumeric: Boolean, context: Context): String{
        val msg = if (!isNumeric)
            context.getString(R.string.error_msg_field_empty)
        else
            context.getString(R.string.error_msg_field_empty_or_less_zero)

        val fieldName: String = context.getString(field_id)
        return msg.replace("$", fieldName)

    }
}
