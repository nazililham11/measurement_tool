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

// Class Activity/tampilan settings
class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsFragment: SettingsFragment

    // Event saat activity pertama kali dijalankan
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inisialisasi fragment settings
        settingsFragment = SettingsFragment(this)
        settingsFragment.resetSettingsCallback = { showResetDialog() }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, settingsFragment)
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // Fungsi untuk memunculkan dialog pilihan Yes/No pada tombol reset 
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

    // Fungsi untuk mereset pengaturan
    private fun resetSettings() {
        val sharePrefs = SharePrefs(this)
        sharePrefs.clear()
    }


    // Class untuk mengkoneksikan tampilan settings dengan shared preferences
    class SettingsFragment(context: Context) : PreferenceFragmentCompat(),  SharedPreferences.OnSharedPreferenceChangeListener {

        private var sharePrefs: SharePrefs = SharePrefs(context)
        var resetSettingsCallback: (()->Unit)? = null

        // Event saat dibuatnya preferences 
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.app_settings, rootKey)
        }

        // Event saat activity pertama kali dijalankan
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Mencari data pengaturan/preference berdasarkan key
            val showGetStarted: SwitchPreferenceCompat? = findPreference(KEY_SHOW_GET_STARTED)
            val hardwareAddress: EditTextPreference? = findPreference(KEY_HARDWARE_ADDRESS)

            // Menyesuaikan tampilan berdasarkan preference
            showGetStarted?.isChecked = sharePrefs.showGetStarted
            hardwareAddress?.text = sharePrefs.hardwareAddress
        }

        // Event saat activity dihentikan sementara (berpindah ke aplikasi lain misalnya)    
        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        // Event saat activity dilanjutkan 
        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        // Event saat terdapat preference yang ditekan 
        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            // Apabila key bernilai reset
            if (preference?.key == "reset_default"){
                // Reset pengaturan
                resetSettingsCallback?.invoke()
            }

            return super.onPreferenceTreeClick(preference)
        }

        // Event saat terdapat data pengaturan yang berubah
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key.equals(KEY_SHOW_GET_STARTED)) {
                // Ubah juga nilai pada variable 
                val pref: SwitchPreferenceCompat? = findPreference(key!!)
                pref?.isChecked?.let {
                    sharePrefs.showGetStarted = it
                }
            } else if (key.equals(KEY_HARDWARE_ADDRESS)){
                // Ubah juga nilai pada variable 
                val pref: EditTextPreference? = findPreference(key!!)
                pref?.text?.let {
                    sharePrefs.hardwareAddress = it
                }
            }
        }
    }

    // Konstanta untuk key data pengaturan pada shared preferences 
    companion object {
        const val KEY_SHOW_GET_STARTED = "show_get_started_screen"
        const val KEY_HARDWARE_ADDRESS = "hardware_address"
    }

}
