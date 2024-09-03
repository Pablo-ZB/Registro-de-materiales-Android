package com.example.inventariodaws

import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.device.ScanManager
import android.device.scanner.configuration.Triggering
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.Animatable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.inventariodaws.databinding.ActivityMainBinding
import com.example.inventariodaws.util.showCustomToast


class MainActivity : AppCompatActivity() {


    private lateinit var scanManager: ScanManager

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(15, systemBars.top + 10, 15, systemBars.bottom)
            insets
        }

        initScan()
        initUI()
    }


    private fun initUI() {
        binding.btnLogout.setOnClickListener{ logOut() }
    }

    private fun initScan(){
        scanManager = ScanManager()
        scanManager.openScanner()
        scanManager.switchOutputMode(0) // 0 para modo Intent, 1 para modo TextBox
        scanManager.triggerMode = Triggering.HOST
        val filter = IntentFilter().apply {
            addAction("android.intent.ACTION_DECODE_DATA")
            addAction("scanner_capture_image_result")
        }
        registerReceiver(mReceiver, filter)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if ("android.intent.ACTION_DECODE_DATA" == action) {
                val bundle = intent.extras;
                val barcodeString = bundle?.getString("barcode_string")
                val toneGen1 = ToneGenerator(AudioManager.STREAM_RING, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                if (barcodeString != null && barcodeString != "")
                {
                    printInformation(barcodeString);
//                    val builder = AlertDialog.Builder(this@MainActivity)
//                    builder.setTitle("Texto escaneado")
//                    builder.setMessage("Texto: $barcodeString")
//                    builder.show()
                }
            } else if ("scanner_capture_image_result" == action) {
                val imageData = intent.getByteArrayExtra("mMap")
                if (imageData != null && imageData.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    if (bitmap != null) {
                        Log.i("onReceive", "Success to get a bitmap")
                        //Success to get a bitmap
                    } else {
                        Log.i("onReceive", "Fail to get a bitmap")
                        //Failed to get a bitmap
                    }
                } else {
                    Log.i("onReceive", "ignore imageData:$imageData")
                }
            }
        }
    }

    private fun barcodeIsValid(barcodeString: String): Boolean
    {
        //check if the barcode is valid
        return true
    }

    private fun printInformation(barcodeString: String)
    {
        if(barcodeString == "40140770")
        {
            binding.tvMessage.visibility = View.VISIBLE
            binding.lyTitles.visibility = View.GONE
            binding.lyContent.visibility = View.GONE
            Toast(this).showCustomToast(
                getString(R.string.qr_invalid),
                this,
                R.drawable.rounded_close_icon,
                "error",
                Toast.LENGTH_SHORT
            )
            return

        }

        binding.tvMessage.visibility = View.GONE
        binding.lyTitles.visibility = View.VISIBLE
        binding.lyContent.visibility = View.VISIBLE

    }

    private fun logOut() {
        val sharedPref = getEncryptedSharedPreferences()
        with(sharedPref.edit()) {
            remove("Access")
            remove("Name")
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    //region $Metodos manuales
    private fun startScanning() {
        scanManager.startDecode()
    }

    private fun stopScanning() {
        scanManager.stopDecode()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanManager.stopDecode()
        scanManager.closeScanner()

        unregisterReceiver(mReceiver)
    }
    //endregion

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "myAppPrefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}