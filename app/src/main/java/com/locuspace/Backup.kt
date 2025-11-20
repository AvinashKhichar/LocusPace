package com.locuspace

import android.net.Uri
import android.os.Bundle
import android.util.Xml
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
import com.locuspace.Database.AppDatabase
import com.locuspace.Database.RunDao
import com.locuspace.Database.RunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class Backup : BaseActivity(){

    private lateinit var runDao: RunDao

    private lateinit var btnExportGpx: MaterialTextView
    private lateinit var btnImportGpx: MaterialTextView
    private lateinit var btnBackupDrive: MaterialTextView

    // ---- ActivityResult launchers ----

    // Create a new GPX file (user chooses location: local / Drive / etc.)
    private val exportGpxLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/xml")) { uri ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    exportRunsToGpx(uri)
                }
            }
        }

    // Open an existing GPX file
    private val importGpxLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    importRunsFromGpx(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.backup) // <- change if your file name differs

        // Room
        val db = AppDatabase.getInstance(this)
        runDao = db.runDao()

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_backup)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnExportGpx = findViewById(R.id.btn_export_gpx)
        btnImportGpx = findViewById(R.id.btn_import_gpx)
        btnBackupDrive = findViewById(R.id.btn_backup_drive)

        enableImmersiveMode()
        setupClickListeners()
    }

    private fun setupClickListeners() {

        // 1) Export to GPX
        btnExportGpx.setOnClickListener {
            // Suggest a filename
            exportGpxLauncher.launch("locuspace_runs.gpx")
        }

        // 2) Import from GPX
        btnImportGpx.setOnClickListener {
            // Allow XML / GPX mime types
            importGpxLauncher.launch(
                arrayOf(
                    "application/xml",
                    "text/xml",
                    "application/gpx+xml"
                )
            )
        }

        // 3) “Backup to Google Drive” – same as export,
        // user just picks Google Drive in the system file picker.
        btnBackupDrive.setOnClickListener {
            val name = "locuspace_backup_${System.currentTimeMillis()}.gpx"
            exportGpxLauncher.launch(name)
        }
    }

    // ---------------- EXPORT ----------------

    private suspend fun exportRunsToGpx(uri: Uri) {
        try {
            val runs = runDao.getAllRunsList() // suspending call

            if (runs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Backup, "No runs to export", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val gpxString = buildGpx(runs)

            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(gpxString.toByteArray(StandardCharsets.UTF_8))
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@Backup, "Exported ${runs.size} runs", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Backup, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildGpx(runs: List<RunEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="Locuspace">""").append("\n")

        for (run in runs) {
            sb.append("  <run")
            sb.append(""" timestamp="${run.timestamp}"""")
            sb.append(""" durationMillis="${run.durationMillis}"""")
            sb.append(""" distanceMeters="${run.distanceMeters}"""")
            sb.append(""" avgSpeedKmh="${run.avgSpeedKmh}"""")
            sb.append(" />\n")
        }

        sb.append("</gpx>")
        return sb.toString()
    }

    // ---------------- IMPORT ----------------

    private suspend fun importRunsFromGpx(uri: Uri) {
        try {
            val newRuns = readRunsFromGpx(uri)

            if (newRuns.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Backup, "No runs found in file", Toast.LENGTH_SHORT).show()
                }
                return
            }

            for (run in newRuns) {
                runDao.insert(run)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@Backup, "Imported ${newRuns.size} runs", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Backup, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun readRunsFromGpx(uri: Uri): List<RunEntity> {
        val result = mutableListOf<RunEntity>()

        val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return emptyList()
        inputStream.use { stream ->
            val reader = InputStreamReader(stream, StandardCharsets.UTF_8)
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(reader)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "run") {
                    val timestamp = parser.getAttributeValue(null, "timestamp")?.toLongOrNull()
                        ?: System.currentTimeMillis()
                    val durationMillis = parser.getAttributeValue(null, "durationMillis")?.toLongOrNull()
                        ?: 0L
                    val distanceMeters = parser.getAttributeValue(null, "distanceMeters")?.toDoubleOrNull()
                        ?: 0.0
                    val avgSpeedKmh = parser.getAttributeValue(null, "avgSpeedKmh")?.toDoubleOrNull()
                        ?: 0.0

                    result.add(
                        RunEntity(
                            timestamp = timestamp,
                            durationMillis = durationMillis,
                            distanceMeters = distanceMeters,
                            avgSpeedKmh = avgSpeedKmh
                        )
                    )
                }
                eventType = parser.next()
            }
        }

        return result
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
