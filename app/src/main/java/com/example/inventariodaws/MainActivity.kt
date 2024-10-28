package com.example.inventariodaws

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.Editable
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TableRow
import android.widget.TextView
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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call


class MainActivity : AppCompatActivity() {

    private lateinit var scanManager: ScanManager
    private lateinit var binding: ActivityMainBinding
    private var scannedCodeL: String? = null
    private var scannedCode: String? = null
    private val dataList = mutableListOf<Pair<String, String>>()
    private var selectedNoPlanta: String? = null


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

        val spinnerNoPlanta: Spinner = findViewById(R.id.spinner_no_planta)

        val noPlantaOptions = arrayOf("DGO", "DGO SUR", "SJDR", "NDD")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, noPlantaOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerNoPlanta.adapter = adapter

        // Recuperar la selección del Spinner desde SharedPreferences
        val selectedPlanta = getSelectedPlanta()
        if (selectedPlanta != null) {
            val position = noPlantaOptions.indexOf(selectedPlanta)
            if (position >= 0) {
                spinnerNoPlanta.setSelection(position)
            }
        }

        spinnerNoPlanta.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedNoPlanta = noPlantaOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedNoPlanta = null
            }
        }


        initScan()
        initUI()

        // Obtener el código escaneado desde LoginActivity
        val scannedCodeFromLogin = intent.getStringExtra("scanned_code")
        if (scannedCodeFromLogin != null) {
            scannedCodeL = scannedCodeFromLogin
            binding.tvUser.text = "no Empleado: $scannedCodeFromLogin"
            binding.tvUser.visibility = View.VISIBLE
        }



    }

    override fun onResume() {
        super.onResume()
        restoreTableState()
        animateTableAppearance()
    }


    private fun animateTableAppearance() {

        binding.tableLayout.visibility = View.INVISIBLE

        binding.tableLayout.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tableLayout.visibility = View.VISIBLE
                }
            })
    }


    private fun restoreTableState() {
        val sharedPreferences = getSharedPreferences("MainActivityPrefs", MODE_PRIVATE)
        val dataListJson = sharedPreferences.getString("dataList", null)
        dataListJson?.let {
            val savedDataList = Gson().fromJson<List<Pair<String, String>>>(it, object : TypeToken<List<Pair<String, String>>>() {}.type)
            savedDataList.forEach { (code, quantity) ->
                addRowToTable(code, quantity)
            }
        }
    }

    private fun getSelectedPlanta(): String? {
        val sharedPreferences = getSharedPreferences("MainActivityPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("selectedPlanta", null)
    }

    private fun initUI() {

        updateSaveButtonState()

        binding.btnLogout.setOnClickListener { logOut() }

        binding.btnSubmit.setOnClickListener {
            val quantity = binding.etQuantity.text.toString()
            if (scannedCode != null && quantity.isNotEmpty()) {

                if (dataList.size == 50) {
                    showConfirmationToSendData()
                } else {
                    addRowToTable(scannedCode!!, quantity)
                    binding.etQuantity.text.clear()
                }
            } else {
                Toast(this).showCustomToast(
                    getString(R.string.enter_quantity),
                    this,
                    R.drawable.alert_icon,
                    "warning",
                    Toast.LENGTH_SHORT
                )
            }
        }

        binding.btnSave.setOnClickListener {
            saveData()
        }

        binding.btnManual.setOnClickListener {
            showKeyboard()
        }

    }


    private fun updateSaveButtonState() {
        if (dataList.isEmpty()) {
            binding.btnSave.isEnabled = false
        } else {
            binding.btnSave.isEnabled = true
        }
    }

    private fun showKeyboard() {

        val inputEditText = EditText(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Ingresar código manualmente")
            .setView(inputEditText)
            .setPositiveButton("Aceptar") { _, _ ->
                val inputText = inputEditText.text.toString()

                binding.tvMessage.text = "Código escaneado: $inputText"
                binding.tvMessage.visibility = View.VISIBLE

                scannedCode = inputText

                binding.etQuantity.visibility = View.VISIBLE
                binding.btnSubmit.visibility = View.VISIBLE
                binding.tableLayout.visibility = View.VISIBLE
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // Mostrar el teclado cuando el diálogo aparece
        inputEditText.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        dialog.show()
    }

    private fun initScan() {
        scanManager = ScanManager()
        scanManager.openScanner()
        scanManager.switchOutputMode(0) // 0 para modo Intent, 1 para modo TextBox
        scanManager.triggerMode = Triggering.HOST
        val filter = IntentFilter().apply {
            addAction("android.intent.ACTION_DECODE_DATA")
        }
        registerReceiver(mReceiver, filter)
    }


    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if ("android.intent.ACTION_DECODE_DATA" == action) {
                val bundle = intent.extras
                val barcodeString = bundle?.getString("barcode_string")
                val toneGen1 = ToneGenerator(AudioManager.STREAM_RING, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
                if (barcodeString != null && barcodeString.isNotEmpty()) {
                    scannedCode = barcodeString
                    binding.tvMessage.text = "Código escaneado: $barcodeString"
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.etQuantity.visibility = View.VISIBLE
                    binding.btnSubmit.visibility = View.VISIBLE
                    binding.tableLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun addRowToTable(code: String, quantity: String) {
        val tableRow = TableRow(this)
        val codeTextView = TextView(this)
        val quantityTextView = TextView(this)
        val deleteButton = ImageButton(this)

        codeTextView.text = code
        codeTextView.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        codeTextView.gravity = Gravity.CENTER

        quantityTextView.text = quantity
        quantityTextView.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        quantityTextView.gravity = Gravity.CENTER

        deleteButton.setImageResource(R.mipmap.ic_trash0_foreground)
        deleteButton.setBackgroundColor(Color.TRANSPARENT)
        deleteButton.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.5f)
        deleteButton.setPadding(0, 0, 0, 0)

        deleteButton.layoutParams.width = 50
        deleteButton.layoutParams.height = 50

        deleteButton.scaleType = ImageView.ScaleType.FIT_CENTER

        deleteButton.setOnClickListener {
            showConfirmationDialog(code, quantity, tableRow)
        }

        tableRow.addView(codeTextView)
        tableRow.addView(quantityTextView)
        tableRow.addView(deleteButton)

        binding.tableLayout.addView(tableRow)

        val separator = View(this)
        val separatorParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)
        separator.setBackgroundColor(Color.GRAY)
        separator.layoutParams = separatorParams
        binding.tableLayout.addView(separator)

        dataList.add(Pair(code, quantity))

        saveDataToSharedPreferences()

        updateSaveButtonState()

    }

    private fun saveDataToSharedPreferences() {
        val dataListJson = Gson().toJson(dataList)
        val sharedPreferences = getSharedPreferences("MainActivityPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("dataList", dataListJson)
        editor.apply()
    }

    private fun showConfirmationToSendData() {
        AlertDialog.Builder(this)
            .setTitle("Limite de datos alcanzado")
            .setMessage("Se han alcanzado 50 datos. ¿Desea enviarlos?")
            .setPositiveButton("Enviar") { _, _ ->
                saveData()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // El usuario cancela, simplemente cerrar el diálogo
                dialog.dismiss()
            }
            .show()
    }

    private fun showConfirmationDialog(code: String, quantity: String, row: TableRow) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar el código $code con la cantidad $quantity?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                binding.tableLayout.removeView(row)
                dataList.remove(Pair(code, quantity))
                dialog.dismiss()
                updateSaveButtonState()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
    private val plantaMappings = mapOf(
        "DGO" to "A",
        "DGO SUR" to "SUR",
        "SJDR" to "SJDR",
        "NDD" to "ND"
    )


    private fun clearSavedData() {
        val sharedPreferences = getSharedPreferences("MainActivityPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("dataList")
        editor.apply()

        dataList.clear()

        binding.tableLayout.removeAllViews()
    }

    private fun saveData() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Estás seguro de que deseas guardar los datos?")
            .setPositiveButton("Sí") { _, _ ->
                val planta = selectedNoPlanta?.let { plantaMappings[it] } ?: "No seleccionado"
                val noEmpleado = scannedCodeL ?: "No escaneado"



        val items = dataList.map { (code, quantity) ->
            Item(code, quantity.toInt())
        }

        val dataItem = DataItem(noEmpleado, planta, items)

        Log.d("SaveData", "Enviando datos: ${dataItem}")


        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()


        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.141.6/materialsapi/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.saveData(dataItem).enqueue(object : retrofit2.Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Datos guardados con éxito", Toast.LENGTH_SHORT).show()

                    saveSelectedPlanta(selectedNoPlanta)
                    startActivity(intent)

                    clearSavedData()
                    finish()
                } else {
                    Log.e("SaveData", "Error al guardar los datos: ${response.errorBody()?.string()}")

                        Toast.makeText(this@MainActivity, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("SaveData", "Error: ${t.message}")
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
            }
            .setNegativeButton("No") { dialog, _ ->
                // El usuario cancela, simplemente cerrar el diálogo
                dialog.dismiss()
            }

            .show()
        saveDataToSharedPreferences()

    }


    // Guardar selección de planta en SharedPreferences
    private fun saveSelectedPlanta(selectedNoPlanta: String?) {
        val sharedPreferences = getSharedPreferences("MainActivityPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selectedPlanta", selectedNoPlanta)
        editor.apply()
    }
    private fun logOut() {

        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { dialog, _ ->
                val sharedPref = getEncryptedSharedPreferences()
                with(sharedPref.edit()) {
                    remove("Access")
                    remove("Name")
                    apply()
                }
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                clearSavedData()
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
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

        unregisterReceiver(mReceiver)
    }
}


