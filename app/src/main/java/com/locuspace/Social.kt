package com.locuspace

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.locuspace.Database.AppDatabase
import com.locuspace.Database.RunDao
import com.locuspace.Database.RunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.library.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Social : BaseActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rootSocial: View
    private lateinit var shareContainer: View


    private lateinit var overlay : View

    private lateinit var ivBackground: ImageView
    private lateinit var btnChooseBackground: MaterialButton
    private lateinit var btnShareInstagram: MaterialButton
    private lateinit var btnShareOther: MaterialButton
    private lateinit var cardSharePreview: MaterialCardView

    private lateinit var tvAppName: TextView
    private lateinit var tvRunDate: TextView
    private lateinit var tvDistanceValue: TextView
    private lateinit var tvTimeValue: TextView
    private lateinit var tvSpeedValue: TextView

    private lateinit var tvDistanceLabel: TextView
    private lateinit var tvTimeLabel: TextView
    private lateinit var tvSpeedLabel: TextView
    private lateinit var tvHashtag: TextView

    private val pickBackgroundImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { ivBackground.setImageURI(it) }
        }

    private val runDao: RunDao by lazy {
        AppDatabase.getInstance(applicationContext).runDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.social)

        initViews()
        setupToolbar()
        setupClicks()
        loadLatestRun()
    }

    private fun initViews() {
        overlay = findViewById(R.id.overlay)
        shareContainer = findViewById(R.id.share_container)
        toolbar = findViewById(R.id.toolbar_social)
        ivBackground = findViewById(R.id.iv_background)
        btnChooseBackground = findViewById(R.id.btn_choose_background)
        btnShareInstagram = findViewById(R.id.btn_share_instagram)
        btnShareOther = findViewById(R.id.btn_share_other)
        cardSharePreview = findViewById(R.id.card_share_preview)

        tvAppName = findViewById(R.id.tv_app_name)
        tvRunDate = findViewById(R.id.tv_run_date)
        tvDistanceValue = findViewById(R.id.tv_distance_value)
        tvTimeValue = findViewById(R.id.tv_time_value)
        tvSpeedValue = findViewById(R.id.tv_speed_value)

        tvDistanceLabel = findViewById(R.id.tv_distance_label)
        tvTimeLabel = findViewById(R.id.tv_time_label)
        tvSpeedLabel = findViewById(R.id.tv_speed_label)
        tvHashtag = findViewById(R.id.tv_hashtag)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClicks() {
        btnChooseBackground.setOnClickListener {
            pickBackgroundImage.launch("image/*")
        }

        btnShareInstagram.setOnClickListener {
            shareImage("com.instagram.android")
        }

        btnShareOther.setOnClickListener {
            shareImage(null)
        }
    }

    /** Load latest run directly from Room */
    private fun loadLatestRun() {
        lifecycleScope.launch {
            val latestRun: RunEntity? = withContext(Dispatchers.IO) {
                runDao.getLatestRun()
            }

            if (latestRun == null) {
                tvDistanceValue.text = "--"
                tvTimeValue.text = "--:--"
                tvSpeedValue.text = "--"
                tvRunDate.text = "No runs yet"
                btnShareInstagram.isEnabled = false
                btnShareOther.isEnabled = false
                return@launch
            }

            // Convert + format
            val distanceKm = latestRun.distanceMeters / 1000.0
            val timeMs = latestRun.durationMillis
            val avgSpeed = latestRun.avgSpeedKmh
            val dateMillis = latestRun.timestamp

            tvDistanceValue.text = String.format("%.2f km", distanceKm)
            tvSpeedValue.text = String.format("%.1f km/h", avgSpeed)
            tvTimeValue.text = formatTime(timeMs)
            tvRunDate.text = formatDate(dateMillis)

            btnShareInstagram.isEnabled = true
            btnShareOther.isEnabled = true
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0)
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatDate(ms: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(ms))
    }

    /** Capture card → Bitmap → share */
    private fun shareImage(targetPackage: String?) {
        // If no run yet, do nothing
        if (!btnShareInstagram.isEnabled && !btnShareOther.isEnabled) return

        val viewToShare = shareContainer   // 🔴 only this part

        if (viewToShare.width == 0 || viewToShare.height == 0) {
            viewToShare.post { shareImage(targetPackage) }
            return
        }

        val bitmap = captureView(viewToShare)
        val uri = saveBitmap(bitmap) ?: return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "#LocusPace")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }

        try {
            startActivity(Intent.createChooser(intent, "Share your run"))
        } catch (e: ActivityNotFoundException) {
            if (targetPackage != null) {
                // fallback to generic share
                shareImage(null)
            }
        }
    }



    private fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }



    private fun saveBitmap(bitmap: Bitmap): Uri? {
        return try {
            val folder = File(cacheDir, "shared_images")
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "share_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            FileProvider.getUriForFile(
                this,
                "com.locuspace.fileprovider",
                file
            )

        } catch (e: Exception) {
            Log.e("Social", "Saving image failed", e)
            null
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
