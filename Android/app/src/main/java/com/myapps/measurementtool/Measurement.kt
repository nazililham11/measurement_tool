package com.myapps.measurementtool

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Measurement(
    var id: Int = 0,
    var title: String = "",
    var length: Double = 0.0,
    var width: Double = 0.0,
    var height: Double = 0.0,
    var wheel_base: Double = 0.0,
    var foh: Double = 0.0,
    var roh: Double = 0.0,
    var approach_angle: Double = 0.0,
    var departure_angle: Double = 0.0,
    var date: Date = Calendar.getInstance().time
) : Parcelable