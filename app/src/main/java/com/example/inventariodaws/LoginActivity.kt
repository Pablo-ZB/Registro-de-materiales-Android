package com.example.inventariodaws

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.inventariodaws.databinding.ActivityLoginBinding
import com.example.inventariodaws.models.LoginData
import com.example.inventariodaws.models.LoginDataResponse
import com.example.inventariodaws.services.ApiService
import com.example.inventariodaws.util.BtnLoadingProgressBar
import com.example.inventariodaws.util.showCustomToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.device.ScanManager
import android.device.scanner.configuration.Triggering
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator


import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.motion.widget.Animatable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventariodaws.databinding.ActivityMainBinding
import com.example.inventariodaws.util.showCustomToast
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.http.Body
import retrofit2.http.POST

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginScanManager: ScanManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkIfAuthenticated()

        initUI()
        loginInitScan()
    }


    private fun checkIfAuthenticated() {
        val sharedPref = getEncryptedSharedPreferences()
        val savedUserCode = sharedPref.getString("Access", null)
        if (savedUserCode != null) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.putExtra("scanned_code", savedUserCode)
            startActivity(intent)
            finish()
        }
    }

    private fun initUI() {
        binding.activityLoginButton.root.setOnClickListener {
            val txtUserVal = binding.inputUser.text.toString()

            if (txtUserVal.isEmpty() || txtUserVal.length != 8 || !txtUserVal.all { it.isDigit() }) {
                Toast(this).showCustomToast(
                    getString(R.string.user_required),
                    this,
                    R.drawable.alert_icon,
                    "warning",
                    Toast.LENGTH_SHORT
                )
            } else {
                val progressbar = BtnLoadingProgressBar(it)
                progressbar.setLoading()
                authenticateUser(txtUserVal, progressbar)
            }
        }
    }

    private fun loginInitScan() {
        loginScanManager = ScanManager()
        loginScanManager.openScanner()
        loginScanManager.switchOutputMode(0) // 0 para modo Intent, 1 para modo TextBox
        loginScanManager.triggerMode = Triggering.HOST
        val filter = IntentFilter().apply {
            addAction("android.intent.ACTION_DECODE_DATA")
        }
        registerReceiver(loginReceiver, filter)
   }

    private val loginReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if ("android.intent.ACTION_DECODE_DATA" == action) {
                val bundle = intent.extras
                val barcodeString = bundle?.getString("barcode_string")
                val toneGen1 = ToneGenerator(AudioManager.STREAM_RING, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                if (barcodeString != null && barcodeString.isNotEmpty()) {
                    binding.inputUser.setText(barcodeString)

                    val progressbar = BtnLoadingProgressBar(findViewById(R.id.activity_login_button))
                    progressbar.setLoading()
                    authenticateUser(barcodeString, progressbar)
                }
            }
        }
    }




    private fun authenticateUser(userCode: String, progressbar: BtnLoadingProgressBar) {
        if (userCode.length == 8 && userCode.all { it.isDigit() }) {

            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.141.6/materialsapi/") //  URL de la API
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(com.example.inventariodaws.ApiService::class.java)

            val loginRequest = LoginRequest(noEmpleado = userCode)
            apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            saveAuthToken(userCode)
                            progressbar.setState(true) {
                                progressbar.reset()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.putExtra("scanned_code", userCode)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            showError("Error: Response body is null")
                            progressbar.reset()
                        }
                    } else {
                        showError("Error: ${response.errorBody()?.string()}")
                        progressbar.reset()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    showError("Failure: ${t.message}")
                    progressbar.reset()
                }
            })
        } else {

            Toast(this).showCustomToast(
                getString(R.string.invalidUser),
                this,
                R.drawable.alert_icon,
                "error",
                Toast.LENGTH_SHORT
            )

            progressbar.setState(false) {
                progressbar.reset()
            }
        }
    }

    private fun showError(message: String) {
        Toast(this).showCustomToast(
            message,
            this,
            R.drawable.alert_icon,
            "error",
            Toast.LENGTH_SHORT
        )
    }




    private fun saveAuthToken(userCode: String) {
        val sharedPref = getEncryptedSharedPreferences()
        with(sharedPref.edit()) {
            putString("Access", userCode)
            apply()
        }
    }

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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(loginReceiver) // Desregistrar el receptor del escáner de login
    }
}