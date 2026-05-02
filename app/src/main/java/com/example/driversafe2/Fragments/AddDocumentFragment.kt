package com.example.driversafe2.Fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.driversafe2.Document
import com.example.driversafe2.R
import com.example.driversafe2.databinding.FragmentAddDocumentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class AddDocumentFragment : Fragment() {
    private lateinit var binding: FragmentAddDocumentBinding
    private var idimage: Uri? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val pickimage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            idimage = uri
            binding.selectedimage.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.selectimage.setOnClickListener {
            pickimage.launch("image/*")
        }

        binding.adddocument.setOnClickListener {
            val idtype = binding.enteridtype.text.toString().trim()
            val expirydate = binding.enterexpiry.text.toString().trim()

            if (idtype.isBlank() || expirydate.isBlank()) {
                Toast.makeText(requireContext(), "Please Fill All The Entries", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (idimage == null) {
                Toast.makeText(requireContext(), "Select An Image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploaddata(idtype, expirydate, idimage!!)
        }

        binding.bac.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun uploaddata(idtype: String, expirydate: String, imageUri: Uri) {
        if (idimage == null) {
            Toast.makeText(requireContext(), "Select An Image", Toast.LENGTH_SHORT).show()
            return
        }
        val path = getRealPathFromURI(requireContext(), idimage!!)
        if (path == null) {
            Toast.makeText(requireContext(), "Could Not Get An Image", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        val cloudName = "djpdnxjhk"
        val uploadpreset = "driversafe"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadpreset)
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("CLOUDINARY_RESPONSE", "Response: $responseBody")

                if (!response.isSuccessful || responseBody == null) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Upload error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val imageUrl = JSONObject(responseBody).getString("secure_url")
                    saveDataToFirebase(idtype, expirydate, imageUrl)
                } catch (e: Exception) {
                    e.printStackTrace()
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to parse response", Toast.LENGTH_SHORT).show()
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
        val prefs = requireContext().getSharedPreferences("DriverSafePrefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null) ?: return
        val newDocKey = database.child("drivers").child(username).child("Documents").push().key ?: return

        val document = com.example.driversafe2.Document(
            idType = idtype,
            expiryDate = expirydate,
            imageUrl = imageUrl,
            id = newDocKey
        )

        database.child("drivers").child(username).child("Documents").child(newDocKey).setValue(document)
            .addOnSuccessListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Document added successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to save to database", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
