package com.example.driversafe2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.driversafe2.databinding.ActivityAddDocumentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.File
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Request

class AddDocuments : AppCompatActivity() {
    private lateinit var binding: ActivityAddDocumentsBinding
    private var idimage: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val pickimage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            idimage = uri
            binding.selectedimage.setImageURI(uri)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.selectimage.setOnClickListener {
            pickimage.launch("image/*")
        }
        binding.adddocument.setOnClickListener {
            val idtype = binding.enteridtype.text.toString().trim()
            val expirydate = binding.enterexpiry.text.toString().trim()

            if (idtype.isBlank() || expirydate.isBlank()) {
                Toast.makeText(this, "Please Fill All The Entries", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (idimage == null) {
                Toast.makeText(this, "Select An Image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploaddata(idtype, expirydate, idimage!!)
        }
        binding.bac.setOnClickListener {
            startActivity(Intent(this,SelectionPage::class.java))
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun uploaddata(idtype: String, expirydate: String, imageUri: Uri) {
        if (idimage == null) {
            Toast.makeText(this, "Select An Image", Toast.LENGTH_SHORT).show()
            return
        }
        val path = getRealPathFromURI(this, idimage!!)
        if (path == null) {
            Toast.makeText(this, "Could Not Get An Image", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        val cloudName = "djpdnxjhk"
        val uploadpreset = "driversafe"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadpreset) // --- FIX: Variable name typo
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddDocuments, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CLOUDINARY_RESPONSE", "Response: $responseBody")

                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread { // --- ADDED THIS ---
                        Toast.makeText(this@AddDocuments, "Upload error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val imageUrl = JSONObject(responseBody).getString("secure_url")
                    saveDataToFirebase(idtype, expirydate, imageUrl)

                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@AddDocuments, "Failed to parse response", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("upload", ".jpg", context.cacheDir)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    private fun saveDataToFirebase(idtype: String, expirydate: String, imageUrl: String) {
        val prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)
        val username = prefs.getString("username", null) ?: return
        val newDocKey = database.child("drivers")
            .child(username)
            .child("Documents")
            .push().key ?: return
        val document = Document(
            idType = idtype,
            expiryDate = expirydate,
            imageUrl = imageUrl,
            id = newDocKey
        )
        database.child("drivers").child(username).child("Documents").child(newDocKey).setValue(document)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this@AddDocuments, "Document added successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(this@AddDocuments, "Failed to save to database", Toast.LENGTH_SHORT).show()
                }
            }

    }

}