package com.example.driversafe2.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.example.driversafe2.JourneyMapActivity
import com.example.driversafe2.ProfileActivity
import com.example.driversafe2.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import java.util.*

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var imageSlider: ImageSlider
    private lateinit var articles: JSONArray
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 5 * 60 * 1000L // 5 minutes
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        imageSlider = binding.imageSlider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        getLocation()
        fetchNewsImages()
        startAutoRefresh()
        val des = binding.destination.text.toString().trim()
        binding.startjourney.setOnClickListener {
            if(des.isBlank()) {
                Toast.makeText(requireContext(), "Enter The Destination", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(),JourneyMapActivity::class.java))
            }
        }
        binding.profilebut.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
        binding.submitfeedbut.setOnClickListener {
            val feedbackText = binding.feedbackentry.text.toString().trim()
            if (feedbackText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your feedback first", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("harshjti0307@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "User Feedback - DriverSafe")
                    putExtra(Intent.EXTRA_TEXT, feedbackText)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Send Feedback"))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
                }
            }
        }


        return binding.root
    }
    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val editText: TextView = binding.locationentry
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: ""
                            val state = address.adminArea ?: ""
                            val country = address.countryName ?: ""
                            val fullAddress = "$city, $state, $country"
                            editText.setText(fullAddress)
                        } else {
                            editText.setText("Unable to get address")
                        }
                    } catch (e: Exception) {
                        editText.setText("Error: ${e.message}")
                    }
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchNewsImages() {
        val url = "https://sih-news-service.onrender.com/news"
        val imageList = ArrayList<SlideModel>()
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    articles = response.getJSONArray("articles")
                    for (i in 0 until articles.length()) {
                        val article = articles.getJSONObject(i)
                        val imageUrl = article.optString("urlToImage", "")
                        if (imageUrl.isNotEmpty() && imageUrl != "null") {
                            imageList.add(SlideModel(imageUrl, ScaleTypes.CENTER_CROP))
                        }
                    }
                    imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)
                    imageSlider.setItemClickListener(object : ItemClickListener {
                        override fun doubleClick(position: Int) {}
                        override fun onItemSelected(position: Int) {
                            try {
                                val clickedArticle = articles.getJSONObject(position)
                                val articleUrl = clickedArticle.optString("url", "")
                                if (articleUrl.isNotEmpty()) {
                                    val browserIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(articleUrl)
                                    )
                                    startActivity(browserIntent)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "No URL available for this article",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "Error opening article",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to parse news data.", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(requireContext(), "Network error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun startAutoRefresh() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchNewsImages()
                handler.postDelayed(this, refreshInterval)
            }
        }, refreshInterval)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }
}
