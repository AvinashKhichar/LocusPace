package com.locuspace

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class RunDetailActivity : BaseActivity(){

    private lateinit var mapView: MapView

    private lateinit var tvDistanceValue: TextView
    private lateinit var tvTimeValue: TextView
    private lateinit var tvSpeedValue: TextView
    private lateinit var tvPaceValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.track)

        // ----- Toolbar -----
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_detail)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ----- Osmdroid init -----
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        mapView = findViewById(R.id.map_view)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // ----- Stats views -----
        tvDistanceValue = findViewById(R.id.tv_distance_value)
        tvTimeValue = findViewById(R.id.tv_time_value)
        tvSpeedValue = findViewById(R.id.tv_speed_value)
        tvPaceValue = findViewById(R.id.tv_pace_value)

        // ----- Get extras from Intent -----
        val runId = intent.getLongExtra("runId", -1L) // not used yet, but available
        val timestamp = intent.getLongExtra("timestamp", 0L)
        val distanceMeters = intent.getDoubleExtra("distanceMeters", 0.0)
        val durationMillis = intent.getLongExtra("durationMillis", 0L)
        val avgSpeedKmh = intent.getDoubleExtra("avgSpeedKmh", 0.0)


        val pathPoints =
            intent.getParcelableArrayListExtra<GeoPoint>("pathPoints") ?: arrayListOf()

        if (pathPoints.size > 1) {
            showFullPath(pathPoints)
        }
        // ----- Toolbar title from timestamp -----
        if (timestamp > 0L) {
            val date = java.util.Date(timestamp)
            val fmt = java.text.SimpleDateFormat(
                "dd MMM, hh:mm a",
                java.util.Locale.getDefault()
            )
            toolbar.title = fmt.format(date)
        }

        // ----- Fill stats -----
        val distanceKm = distanceMeters / 1000.0
        tvDistanceValue.text = String.format("%.2f km", distanceKm)
        tvTimeValue.text = formatDuration(durationMillis)
        tvSpeedValue.text = String.format("%.1f km/h", avgSpeedKmh)

        val paceSecondsPerKm = if (distanceKm > 0) {
            (durationMillis / 1000.0) / distanceKm
        } else 0.0
        tvPaceValue.text = formatPace(paceSecondsPerKm)

        // ----- Draw path if available -----
        if (pathPoints.size > 1) {
            showFullPath(pathPoints)
        } else {
            // fallback: just set a default zoom if you want
            mapView.controller.setZoom(15.0)
        }

        enableImmersiveMode()
    }

    private fun showFullPath(points: List<GeoPoint>) {
        if (points.size < 2) return

        val polyline = Polyline().apply {
            setPoints(points)
            outlinePaint.strokeWidth = 8f

        }
        mapView.overlays.add(polyline)

        // Run AFTER the map has been laid out, so zoomToBoundingBox works
        mapView.post {
            centerMapToPath(points)
            mapView.invalidate()
        }
    }


    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun formatPace(paceSecondsPerKm: Double): String {
        if (paceSecondsPerKm <= 0.0 || paceSecondsPerKm.isNaN()) {
            return "--:-- /km"
        }
        val totalSeconds = paceSecondsPerKm.toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d /km", minutes, seconds)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun centerMapToPath(points: List<GeoPoint>) {
        if (points.isEmpty()) return

        val box = BoundingBox.fromGeoPoints(points)

        // Zoom so the whole path fits
        mapView.zoomToBoundingBox(box, true)

        // Extra: set center to the middle of the box
        val centerLat = (box.latNorth + box.latSouth) / 2
        val centerLon = (box.lonEast + box.lonWest) / 2
        mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
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
