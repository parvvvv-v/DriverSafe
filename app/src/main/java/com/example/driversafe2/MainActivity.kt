package com.example.driversafe2

import Driver
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.driversafe2.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private lateinit var prefs: SharedPreferences
    private var selectedGender = ""

    private val camerapermissionlauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                binding.CameraPermissionbutton.text = "Permission Granted"
                binding.CameraPermissionbutton.isEnabled = false
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
                checkallPermission()
            } else Toast.makeText(this, "Camera Permission Not Granted", Toast.LENGTH_SHORT).show()
        }

    private val GpsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                binding.GpsPermissionButton.text = "Permission Granted"
                binding.GpsPermissionButton.isEnabled = false
                Toast.makeText(this, "GPS Permission Granted", Toast.LENGTH_SHORT).show()
                checkallPermission()
            } else Toast.makeText(this, "GPS Permission Not Granted", Toast.LENGTH_SHORT).show()
        }

    private fun checkallPermission() {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        val gpsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        binding.nextbutton.isEnabled = cameraGranted && gpsGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)
        database = FirebaseDatabase.getInstance().reference

        if (prefs.getBoolean("setup_done", false)) {
            startActivity(Intent(this, SelectionPage::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gender spinner
        val genders = arrayOf("Select Gender", "Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, genders)
        binding.genderSpinner.adapter = adapter
        binding.genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>, v: View?, pos: Int, id: Long) {
                selectedGender = if (pos != 0) genders[pos] else ""
            }
            override fun onNothingSelected(p0: AdapterView<*>) {}
        }

        // DOB picker
        binding.dobentry.setOnClickListener {
            val c = java.util.Calendar.getInstance()
            val dp = android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    binding.dobentry.setText("%02d/%02d/%04d".format(d, m + 1, y))
                },
                c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        binding.CameraPermissionbutton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                binding.CameraPermissionbutton.text = "Permission Granted"
                binding.CameraPermissionbutton.isEnabled = false
            } else camerapermissionlauncher.launch(Manifest.permission.CAMERA)
            checkallPermission()
        }

        binding.GpsPermissionButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                binding.GpsPermissionButton.text = "Permission Granted"
                binding.GpsPermissionButton.isEnabled = false
            } else GpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            checkallPermission()
        }

        binding.nextbutton.setOnClickListener {
            val name = binding.nameentry.text.toString().trim()
            val vehicle = binding.vehiclenumber.text.toString().trim()
            val dob = binding.dobentry.text.toString().trim()

            if (name.isBlank()) {
                binding.nameentry.error = "Name is required"; return@setOnClickListener
            }
            if (vehicle.isBlank()) {
                binding.vehiclenumber.error = "Vehicle number is required"; return@setOnClickListener
            }
            if (selectedGender.isBlank()) {
                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (dob.isBlank()) {
                binding.dobentry.error = "Date of Birth is required"; return@setOnClickListener
            }
            if (!binding.nextbutton.isEnabled) {
                Toast.makeText(this, "Please grant all permissions", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }

            saveDriverData(name, vehicle, selectedGender, dob)
            prefs.edit()
                .putBoolean("setup_done", true)
                .putString("username", name)
                .putString("gender", selectedGender)
                .putString("dob", dob)
                .apply()
            startActivity(Intent(this, SelectionPage::class.java))
            finish()
        }

        checkallPermission()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun saveDriverData(name: String, vehicle: String, gender: String, dob: String) {
        val driver = Driver(name, vehicle, gender, dob)
        database.child("drivers").child(name).setValue(driver)
            .addOnSuccessListener { Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Save failed: ${it.message}", Toast.LENGTH_LONG).show() }
    }
}
