package com.locuspace

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.locuspace.Database.AppDatabase
import com.locuspace.Database.RunDao
import com.locuspace.Database.RunEntity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream
import kotlin.jvm.java


class TrackRun : BaseActivity(){


    private lateinit var menu : ImageView
    private lateinit var mapView: MapView
    private lateinit var timeText : TextView
    private lateinit var startButton : Button
    private lateinit var finishButton : Button

    private lateinit var musicbutton : ImageView

    //location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //databse
    private lateinit var db: AppDatabase
    private lateinit var runDao: RunDao


    //notiification
    private val NOTIF_REQ_CODE = 2001



    //map markers
    private var userMarker: Marker? = null
    private var pathLine: Polyline? = null
    private val pathPoints = mutableListOf<GeoPoint>()
    private val LOCATION_PERMISSION = 1001


    private var isRunning  = false
    private var baseTime = 0L
    private var elapsedTime = 0L

    private val handler = android.os.Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable{
        override fun run(){
            val now = SystemClock.elapsedRealtime()
            val currentElapsed = now - baseTime
            updateTimeText(elapsedTime + currentElapsed)
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.home)


        //location preference
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        //database
        db = AppDatabase.getInstance(this)
        runDao = db.runDao()

        //current location(realtime)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    updateUserLocation(location.latitude, location.longitude)
                }
            }
        }

        requestLocationPermission()


        //notification
        requestNotificationPermissionIfNeeded()

        initmap()


//        val mapController = mapView.controller
//        mapController.setZoom(15.0)
//        val marker = Marker(mapView)
//
//        val startPoint = GeoPoint(18.605701, 73.874417)
//        mapController.setCenter(startPoint)


        initfun()

        finishButton.visibility = View.GONE
        updateTimeText(0L)

        onclick()
        enableImmersiveMode()
    }

    private fun initmap(){
        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
    }


    private fun initfun(){
        timeText = findViewById(R.id.time)
        startButton = findViewById(R.id.start_button)
        finishButton = findViewById(R.id.stop_button)
        musicbutton = findViewById(R.id.music_icon)
        menu = findViewById(R.id.nav)
    }

    private fun onclick(){
        finishButton.setOnClickListener {
            finishRun()
        }

        startButton.setOnClickListener {
            if(!isRunning){
                startStopWatch()
                finishButton.visibility = View.VISIBLE
            }else{
                stopStopWatch()
            }
        }

        musicbutton.setOnClickListener {
            openSpotify()
        }

        menu.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            startActivity(intent)
        }


    }



    private fun calculateTotalDistanceMeters(): Double{
        if(pathPoints.size<2)return 0.0

        var distance = 0.0
        for(i in 1 until pathPoints.size){
            distance += pathPoints[i-1].distanceToAsDouble(pathPoints[i])
        }
        return distance
    }


    private fun finishRun(){
        stopForegroundTrackingService()
        val totalMillis = if(isRunning){
            val now = SystemClock.elapsedRealtime()
            elapsedTime+(now-baseTime)
        }else{
            elapsedTime
        }

        val distanceMeters = calculateTotalDistanceMeters()


        val hours = totalMillis/3600000
        val avgSpeedKmh = if(totalMillis>0){
            (distanceMeters/1000.0)/totalMillis
        }else{
            0.0
        }

        val runTimestamp = System.currentTimeMillis()

        val run = RunEntity(
            timestamp = System.currentTimeMillis(),
            durationMillis = totalMillis,
            distanceMeters = distanceMeters,
            avgSpeedKmh = avgSpeedKmh
        )


        lifecycleScope.launch {
            saveSnapshotForRun(runTimestamp)
            runDao.insert(run)
        }

        val detailIntent = Intent(this, RunDetailActivity::class.java).apply {
            putExtra("timestamp", runTimestamp)
            putExtra("durationMillis", totalMillis)
            putExtra("distanceMeters", distanceMeters)
            putExtra("avgSpeedKmh", avgSpeedKmh)
            putParcelableArrayListExtra("pathPoints", ArrayList(pathPoints))
        }
        startActivity(detailIntent)

        resetStopWatch()
        finishButton.visibility = View.GONE
    }


    private fun startStopWatch(){
        isRunning = true
        startButton.text = "STOP"

        startForegroundTrackingService()

        baseTime = SystemClock.elapsedRealtime()
        handler.post(updateRunnable)

        pathPoints.clear()
        if (pathLine == null) {
            pathLine = Polyline().apply {
                setPoints(pathPoints)
            }
            mapView.overlays.add(pathLine)
        } else {
            pathLine!!.setPoints(pathPoints)
        }
    }

    private fun stopStopWatch(){
        isRunning = false
        startButton.text = "START"
        stopForegroundTrackingService()

        val now = SystemClock.elapsedRealtime()
        elapsedTime += now - baseTime

        handler.removeCallbacks(updateRunnable)


    }

    private fun resetStopWatch(){
        isRunning = false
        handler.removeCallbacks(updateRunnable)
        elapsedTime =0L
        baseTime = 0L
        updateTimeText(0L)
        startButton.text = "START"

        pathPoints.clear()
        pathLine?.setPoints(pathPoints)
        mapView.invalidate()
    }

    private fun updateTimeText(milis : Long){
        val totalSeconds = milis/1000
        val minutes = totalSeconds/60
        val seconds = totalSeconds%60
        timeText.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun onResume(){
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION
            )
        } else {
            startLocationUpdates()
        }
    }

//    private fun getCurrentLocation() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) return
//
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                val userPoint = GeoPoint(location.latitude, location.longitude)
//
//                // Move map to this point
//                val controller = mapView.controller
//                controller.setZoom(17.0)
//                controller.setCenter(userPoint)
//
//                // Add marker
//                val marker = Marker(mapView)
//                marker.position = userPoint
//                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//                marker.title = "You are here"
//                mapView.overlays.add(marker)
//            }
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, now get the location
                startLocationUpdates()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateUserLocation(lat: Double, lon: Double) {
        val newPoint = GeoPoint(lat, lon)

        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                position = newPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "You"
            }
            mapView.overlays.add(userMarker)
            mapView.controller.setZoom(18.0)
        }else {
            userMarker!!.position = newPoint
        }

        mapView.controller.setCenter(newPoint)
        if (isRunning) {
            pathPoints.add(newPoint)

            if (pathLine == null) {
                pathLine = Polyline().apply {
                    setPoints(pathPoints)
                }
                mapView.overlays.add(pathLine)
            } else {
                pathLine!!.setPoints(pathPoints)
            }
        }

        mapView.invalidate()
    }

    private fun openSpotify() {
         val spotifyUrl = "https://open.spotify.com/"

        val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl)).apply {
            `package` = "com.spotify.music"
        }
        if (spotifyIntent.resolveActivity(packageManager) != null) {
            startActivity(spotifyIntent)
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(spotifyUrl)
            )
            startActivity(webIntent)
        }
    }

    private fun startForegroundTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        startForegroundService(intent)
    }

    private fun stopForegroundTrackingService() {
        val intent = Intent(this, TrackingService::class.java)
        stopService(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIF_REQ_CODE
                )
            }
        }
    }





    private fun captureRouteSnapshot(): Bitmap? {
        if (mapView.width == 0 || mapView.height == 0) return null

        val bitmap = Bitmap.createBitmap(
            mapView.width,
            mapView.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        mapView.draw(canvas)   // draw map + overlays + path
        return bitmap

    }

    private fun getSnapshotFile(runTimestamp: Long): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
        return File(dir, "run_${runTimestamp}.png")
    }

    private fun saveSnapshotForRun(runTimestamp: Long) {
        val bitmap = captureRouteSnapshot() ?: return
        val file = getSnapshotFile(runTimestamp)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
    }


    private fun enableImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }
}