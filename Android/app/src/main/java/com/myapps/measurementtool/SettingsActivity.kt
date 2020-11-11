package com.myapps.measurementtool

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsFragment: SettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsFragment = SettingsFragment(this)
        settingsFragment.resetSettingsCallback = { showResetDialog() }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, settingsFragment)
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showResetDialog(){
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure want to reset all settings to default ?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> resetSettings() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        val alert = dialogBuilder.create()
        alert.setTitle("Delete Data")
        alert.show()
    }

    private fun resetSettings() {
        val sharePrefs = SharePrefs(this)
        sharePrefs.clear()
    }


    class SettingsFragment(context: Context) : PreferenceFragmentCompat(),  SharedPreferences.OnSharedPreferenceChangeListener {

        private var sharePrefs: SharePrefs = SharePrefs(context)
        var resetSettingsCallback: (()->Unit)? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_settings, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val showGetStarted: SwitchPreferenceCompat? = findPreference(KEY_SHOW_GET_STARTED)
            val hardwareAddress: EditTextPreference? = findPreference(KEY_HARDWARE_ADDRESS)

            showGetStarted?.isChecked = sharePrefs.showGetStarted
            hardwareAddress?.text = sharePrefs.hardwareAddress
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if (preference?.key == "reset_default"){
                resetSettingsCallback?.invoke()
            }

            return super.onPreferenceTreeClick(preference)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Log.d("SharedPrefChanged", "key $key")
            if (key.equals(KEY_SHOW_GET_STARTED)) {
                val pref: SwitchPreferenceCompat? = findPreference(key!!)
                pref?.isChecked?.let {
                    sharePrefs.showGetStarted = it
                }
            } else if (key.equals(KEY_HARDWARE_ADDRESS)){
                val pref: EditTextPreference? = findPreference(key!!)
                pref?.text?.let {
                    sharePrefs.hardwareAddress = it
                }
            }
        }
    }


    companion object {
        const val KEY_SHOW_GET_STARTED = "show_get_started_screen"
        const val KEY_HARDWARE_ADDRESS = "hardware_address"
    }

}
