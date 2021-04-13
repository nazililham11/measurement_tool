package com.myapps.measurementtool

import android.content.Context
import android.content.SharedPreferences

// Class untuk mengkoneksikan dengan shared preferences (tempat menyimpan pengaturan aplikasi)
class SharePrefs(context: Context) {

    // Konstanta
    companion object {
        private const val PREFS_FILENAME = "app_prefs"

        // Key tiap pengaturan 
        private const val KEY_HARDWARE_ADDRESS = "pref_hardware_address"
        private const val KEY_SHOW_GET_STARTED = "pref_show_get_started"
        private const val KEY_APP_FIRST_OPENED = "pref_app_first_opened"
    }

    // Variable Shared preferences
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    // Niali default pengaturan
    val defHardwareAddress = "ws://10.10.10.1"
    val defShowGetStarted = false
    val defAppFirstOpened = true


    // Data pengaturan untuk alamat harware
    var hardwareAddress: String
        get() = sharedPrefs.getString(KEY_HARDWARE_ADDRESS, defHardwareAddress) ?: defHardwareAddress
        set(value) = sharedPrefs.edit().let {
            it.putString(KEY_HARDWARE_ADDRESS, value)
            it.apply()
        }

    // Data pengaturan untuk menampilkan tampilan get started
    var showGetStarted: Boolean
        get() = sharedPrefs.getBoolean(KEY_SHOW_GET_STARTED, defShowGetStarted)
        set(value) = sharedPrefs.edit().let {
            it.putBoolean(KEY_SHOW_GET_STARTED, value)
            it.apply()
        }

    // Data pengaturan menyimpan tenang apakah aplikasi baru pertama kali dibuka
    var appFirstOpened: Boolean
        get() = sharedPrefs.getBoolean(KEY_APP_FIRST_OPENED, defAppFirstOpened)
        set(value) = sharedPrefs.edit().let {
            it.putBoolean(KEY_APP_FIRST_OPENED, value)
            it.apply()
        }

    
    // Fungsi untuk mereset pengaturan
    fun clear(){
        sharedPrefs.edit().let {
            it.clear()
            it.apply()
        }
    }

}