package com.example.measurementtool

import android.content.Context
import android.content.SharedPreferences

class SharePrefs(context: Context) {

        companion object {
            private const val PREFS_FILENAME = "app_prefs"

            private const val KEY_HARDWARE_ADDRESS = "pref_hardware_address"
            private const val KEY_SHOW_GET_STARTED = "pref_show_get_started"
            private const val KEY_APP_FIRST_OPENED = "pref_app_first_opened"
        }

        private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        val defHardwareAddress = "ws://192.168.0.101"
        var hardwareAddress: String
            get() = sharedPrefs.getString(KEY_HARDWARE_ADDRESS, defHardwareAddress) ?: defHardwareAddress
            set(value) = sharedPrefs.edit().let {
                it.putString(KEY_HARDWARE_ADDRESS, value)
                it.apply()
            }

        val defShowGetStarted = false
        var showGetStarted: Boolean
            get() = sharedPrefs.getBoolean(KEY_SHOW_GET_STARTED, defShowGetStarted)
            set(value) = sharedPrefs.edit().let {
                it.putBoolean(KEY_SHOW_GET_STARTED, value)
                it.apply()
            }

        val defAppFirstOpened = true
        var appFirstOpened: Boolean
            get() = sharedPrefs.getBoolean(KEY_APP_FIRST_OPENED, defAppFirstOpened)
            set(value) = sharedPrefs.edit().let {
                it.putBoolean(KEY_APP_FIRST_OPENED, value)
                it.apply()
            }

        fun clear(){
            sharedPrefs.edit().let {
                it.clear()
                it.apply()
            }
        }

}