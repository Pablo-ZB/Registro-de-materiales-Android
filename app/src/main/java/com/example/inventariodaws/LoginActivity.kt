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


@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var retrofit: Retrofit
    private val handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        retrofit = getRetrofit()
        val authToken = getAuthToken()
        if (authToken != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            initUI()
        }
    }

    private fun initUI() {
        binding.activityLoginButton.root.setOnClickListener {
            val progressbar = BtnLoadingProgressBar(it)
            login(progressbar)
        }
    }

    private fun login(progressbar: BtnLoadingProgressBar) {

        val txtUserVal = binding.inputUser.text.toString()
        val txtPasswordVal = binding.inputPassword.text.toString()

        if (txtUserVal.isEmpty() || txtPasswordVal.isEmpty()) {
            Toast(this).showCustomToast(
                getString(R.string.user_required),
                this,
                R.drawable.alert_icon,
                "warning",
                Toast.LENGTH_SHORT
            )
        } else {
            progressbar.setLoading()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dataLogin = LoginData(txtUserVal, txtPasswordVal)
                    val myResponse: Response<LoginDataResponse> =
                        retrofit.create(ApiService::class.java).loginApp(dataLogin)
                    runOnUiThread {
                        if (myResponse.isSuccessful) {
                            val Access = myResponse.body()!!.acesso
                            val UserName = myResponse.body()!!.user
                            saveAuthToken(Access,UserName)
                            Toast(this@LoginActivity).showCustomToast(
                                getString(R.string.welcome),
                                this@LoginActivity,
                                R.drawable.check_icon,
                                "success",
                                Toast.LENGTH_SHORT
                            )
                            progressbar.setState(true) {
                                progressbar.reset()
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            binding.inputPassword.text.clear()
                            Toast(this@LoginActivity).showCustomToast(
                                getString(R.string.invalidUser),
                                this@LoginActivity,
                                R.drawable.alert_icon,
                                "error",
                                Toast.LENGTH_SHORT
                            )
                            progressbar.setState(false) {
                                progressbar.reset()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    runOnUiThread {
                        progressbar.setState(false) {
                            Toast(this@LoginActivity).showCustomToast(
                                getString(R.string.error_login),
                                this@LoginActivity,
                                R.drawable.alert_icon,
                                "error",
                                Toast.LENGTH_SHORT
                            )
                            handler.postDelayed({
                                progressbar.reset()
                            }, 0)
                        }
                    }
                }
            }
        }
    }

    private fun saveAuthToken(access: String,name: String) {
        val sharedPref = getEncryptedSharedPreferences()
        with(sharedPref.edit()) {
            putString("Access", access)
            putString("Name", name)
            apply()
        }
    }

    private fun getAuthToken(): String? {
        val sharedPref = getEncryptedSharedPreferences()
        return sharedPref.getString("Access", null)
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

    private fun getRetrofit(): Retrofit {
        return Retrofit
            .Builder()
            .baseUrl("http://192.168.141.6:87/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


}