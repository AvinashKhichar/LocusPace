package com.locuspace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.locuspace.Database.AppDatabase
import com.locuspace.Database.RunDao
import com.locuspace.Database.RunEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class Statistic : AppCompatActivity() {


    private lateinit var toolbar : MaterialToolbar
    private lateinit var rvTrackList : RecyclerView

    private lateinit var tvTotalRuns : TextView
    private lateinit var tvTotalDistance : TextView
    private lateinit var tvTotalDuration : TextView
    private lateinit var tvAvgSpeed : TextView


    private lateinit var adapter : RunHistoryAdapter

    private lateinit var runDao : RunDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stat)

        toolbar = findViewById(R.id.toolbar_history)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvTotalRuns = findViewById(R.id.tv_total_runs)
        tvTotalDistance = findViewById(R.id.tv_total_distance)
        tvTotalDuration = findViewById(R.id.tv_total_duration)
        tvAvgSpeed = findViewById(R.id.tv_avg_speed)

        rvTrackList = findViewById(R.id.rv_track_list)
        adapter = RunHistoryAdapter { run ->
            val intent = Intent(this, RunDetailActivity::class.java).apply {
                putExtra("runId", run.id)
                putExtra("timestamp", run.timestamp)
                putExtra("distanceMeters", run.distanceMeters)
                putExtra("durationMillis", run.durationMillis)
                putExtra("avgSpeedKmh", run.avgSpeedKmh)
            }
            startActivity(intent)
        }
        rvTrackList.adapter = adapter

        val db = AppDatabase.getInstance(this)
        runDao = db.runDao()

        observeRuns()

    }

    private fun observeRuns(){
        lifecycleScope.launch {
            runDao.getAllRuns().collectLatest { runs ->
                adapter.submitList(runs)
                updateSummary(runs)
            }
        }
    }

    private fun updateSummary(runs : List<RunEntity>){
        val totalRuns = runs.size
        val totalDistanceMeters = runs.sumOf { it.distanceMeters }
        val totalDurationMillis = runs.sumOf { it.durationMillis }

        val totalDistanceKm = totalDistanceMeters / 1000.0
        val avgSpeedKmh = if (totalDurationMillis > 0L && totalDistanceMeters > 0.0) {
            // durationMillis -> hours
            val hours = totalDurationMillis / 3600000.0
            totalDistanceKm / hours
        } else {
            0.0
        }

        tvTotalRuns.text = "Total runs: $totalRuns"
        tvTotalDistance.text = String.format(
            java.util.Locale.getDefault(),
            "Total distance: %.2f km",
            totalDistanceKm
        )
        tvTotalDuration.text = "Total duration: ${formatDuration(totalDurationMillis)}"
        tvAvgSpeed.text = String.format(
            java.util.Locale.getDefault(),
            "Avg speed: %.1f km/h",
            if (avgSpeedKmh.isFinite()) avgSpeedKmh else 0.0
        )

    }

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}