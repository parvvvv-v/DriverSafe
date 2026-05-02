package com.example.driversafe2

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.driversafe2.databinding.ActivityJourneyMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class JourneyMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityJourneyMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private lateinit var mapView: MapView
    private lateinit var cameraExecutor: ExecutorService
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    private val sleepThresholdMs = 2000L // 2 seconds
    private var lastClosedTime = 0L
    private var eyesClosed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJourneyMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("DriverSafePrefs", MODE_PRIVATE)

        val from = intent.getStringExtra("from")
        val to = intent.getStringExtra("to")
        Toast.makeText(this, "Journey: $from → $to", Toast.LENGTH_SHORT).show()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        }

        startCameraForEyeDetection()
        startLocationTracking()

    }

    // MAP setup
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val startLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
                val endPoint = LatLng(location.latitude + 0.02, location.longitude + 0.02)
                map.addPolyline(PolylineOptions().add(startLatLng, endPoint).width(8f))
            } else {
                Toast.makeText(this, "Unable to get GPS location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // CAMERA + MLKit eye detection
    private fun startCameraForEyeDetection() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewView = binding.cameraPreview

            val preview = androidx.camera.core.Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            preview.setSurfaceProvider { request ->
                val surface = previewView.holder.surface
                if (surface != null) {
                    request.provideSurface(surface, cameraExecutor) { }
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // EYE DETECTION logic
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector = FaceDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { faces ->
                detectSleep(faces)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private var isAlertTriggered = false

    private fun detectSleep(faces: List<Face>) {
        for (face in faces) {
            val leftEye = face.leftEyeOpenProbability ?: 1.0f
            val rightEye = face.rightEyeOpenProbability ?: 1.0f
            val currentTime = System.currentTimeMillis()

            if (leftEye < 0.3 && rightEye < 0.3) {
                // Eyes closed
                if (!eyesClosed) {
                    eyesClosed = true
                    lastClosedTime = currentTime
                } else if (currentTime - lastClosedTime > sleepThresholdMs && !isAlertTriggered) {
                    // Eyes closed long enough AND alert not yet triggered
                    isAlertTriggered = true
                    runOnUiThread {
                        binding.alertText.visibility = View.VISIBLE
                        playAlertSound()
                        saveAlertEventToDatabase()
                    }
                }
            } else {
                // Eyes open again → reset everything
                eyesClosed = false
                isAlertTriggered = false
                runOnUiThread {
                    binding.alertText.visibility = View.GONE
                }
            }
        }
    }


    private fun playAlertSound() {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
    }
    // ✅ SAVE ALERTS TO DRIVER NODE IN FIREBASE
    private fun saveAlertEventToDatabase() {
        val username = prefs.getString("username", null)
        if (username == null) {
            Toast.makeText(this, "Driver info not found!", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = System.currentTimeMillis()
        val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

        val alertData = mapOf(
            "timestamp" to timestamp,
            "time" to time,
            "date" to date
        )

        val driverAlertRef = FirebaseDatabase.getInstance()
            .getReference("drivers")
            .child(username)
            .child("alerts")

        driverAlertRef.push().setValue(alertData)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
