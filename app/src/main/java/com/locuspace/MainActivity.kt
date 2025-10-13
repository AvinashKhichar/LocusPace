package com.locuspace// Replace with your package name
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.graphics.Color
//import android.location.Location
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.os.SystemClock
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.google.android.gms.location.*
//import org.osmdroid.tileprovider.tilesource.TileSourceFactory
//import org.osmdroid.util.GeoPoint
//import org.osmdroid.views.MapView
//import org.osmdroid.views.overlay.Polyline
//import java.util.concurrent.TimeUnit
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var mapView: MapView
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var locationCallback: LocationCallback
//
//    // UI Elements
//    private lateinit var distanceTextView: TextView
//    private lateinit var timeTextView: TextView
//    private lateinit var speedTextView: TextView
//    private lateinit var paceTextView: TextView
//
//    // Timer
//    private val timerHandler = Handler(Looper.getMainLooper())
//    private lateinit var timerRunnable: Runnable
//    private var startTime = 0L
//
//    // Tracking State
//    private var totalDistance = 0.0f // in meters
//    private var lastLocation: Location? = null
//    private val pathPoints = mutableListOf<GeoPoint>()
//    private lateinit var pathOverlay: Polyline
//
//    companion object {
//        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.home)
//
//        // Initialize Views
//        mapView = findViewById(R.id.map)
//        distanceTextView = findViewById(R.id.distance)
//        timeTextView = findViewById(R.id.time)
//        speedTextView = findViewById(R.id.speed)
//        paceTextView = findViewById(R.id.pace)
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        setupMap()
//        setupLocationCallback()
//        setupTimer()
//        checkLocationPermission()
//    }
//
//    private fun setupMap() {
//        mapView.setTileSource(TileSourceFactory.MAPNIK)
//        mapView.setMultiTouchControls(true)
//        mapView.controller.setZoom(18.0)
//        pathOverlay = Polyline().apply {
//            color = Color.BLUE
//            width = 10f
//        }
//        mapView.overlays.add(pathOverlay)
//    }
//
//    private fun setupLocationCallback() {
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                locationResult.lastLocation?.let { newLocation ->
//                    updatePath(newLocation)
//                    updateStats(newLocation)
//                    lastLocation = newLocation
//                }
//            }
//        }
//    }
//
//    private fun setupTimer() {
//        timerRunnable = object : Runnable {
//            override fun run() {
//                val elapsedMillis = SystemClock.elapsedRealtime() - startTime
//                timeTextView.text = formatTime(elapsedMillis)
//                timerHandler.postDelayed(this, 1000) // Update every second
//            }
//        }
//    }
//
//    private fun checkLocationPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
//        } else {
//            startTracking()
//        }
//    }
//
//    private fun startTracking() {
//        Toast.makeText(this, "Tracking started!", Toast.LENGTH_SHORT).show()
//
//        // Start time tracking
//        startTime = SystemClock.elapsedRealtime()
//        timerHandler.post(timerRunnable)
//
//        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
//            .setWaitForAccurateLocation(true)
//            .setMinUpdateIntervalMillis(2000)
//            .setMaxUpdateDelayMillis(10000)
//            .build()
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
//        }
//    }
//
//    private fun stopTracking() {
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//        timerHandler.removeCallbacks(timerRunnable)
//        Toast.makeText(this, "Tracking stopped.", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun updatePath(location: Location) {
//        val newPoint = GeoPoint(location.latitude, location.longitude)
//        pathPoints.add(newPoint)
//        pathOverlay.setPoints(pathPoints)
//        mapView.controller.animateTo(newPoint)
//        mapView.invalidate()
//    }
//
//    private fun updateStats(location: Location) {
//        lastLocation?.let { totalDistance += it.distanceTo(location) }
//        distanceTextView.text = String.format("%.2f km", totalDistance / 1000)
//
//        val speedKmh = location.speed * 3.6f
//        speedTextView.text = String.format("%.1f", speedKmh)
//
//        if (speedKmh > 1) {
//            val paceMinPerKm = 60 / speedKmh
//            val minutes = paceMinPerKm.toInt()
//            val seconds = ((paceMinPerKm - minutes) * 60).toInt()
//            paceTextView.text = String.format("%d'%02d\"", minutes, seconds)
//        } else {
//            paceTextView.text = "0'00\""
//        }
//    }
//
//    private fun formatTime(millis: Long): String {
//        val hours = TimeUnit.MILLISECONDS.toHours(millis)
//        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
//        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
//        return if (hours > 0) {
//            String.format("%02d:%02d:%02d", hours, minutes, seconds)
//        } else {
//            String.format("%02d:%02d", minutes, seconds)
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startTracking()
//            } else {
//                Toast.makeText(this, "Location permission is required for tracking.", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        mapView.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        mapView.onPause()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopTracking()
//    }
//}