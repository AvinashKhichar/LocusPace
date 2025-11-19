package com.locuspace

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.locuspace.Database.RunEntity
import java.io.File

class RunHistoryAdapter(
    private val onItemClick: (RunEntity) -> Unit = {}
) : ListAdapter<RunEntity, RunHistoryAdapter.RunViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_history, parent, false)
        return RunViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RunViewHolder(
        itemView: View,
        private val onItemClick: (RunEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivTrackIcon: ImageView = itemView.findViewById(R.id.iv_track_icon)
        private val tvTrackTitle: TextView = itemView.findViewById(R.id.tv_track_title)
        private val tvTrackDate: TextView = itemView.findViewById(R.id.tv_track_date)
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_duration)

        fun bind(run: RunEntity) {
            // Title: time of day
            val date = java.util.Date(run.timestamp)
            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

            tvTrackTitle.text = "Run at ${timeFormat.format(date)}"
            tvTrackDate.text = dateFormat.format(date)

            // Distance in km to 2 decimal places
            val km = run.distanceMeters / 1000.0
            tvDistance.text = String.format(java.util.Locale.getDefault(), "%.2f km", km)

            // Duration as hh:mm:ss
            tvDuration.text = formatDuration(run.durationMillis)

            val snapshotFile = getSnapshotFile(itemView.context, run.timestamp)
            if (snapshotFile.exists()) {
                Glide.with(itemView)
                    .load(snapshotFile)
                    .into(ivTrackIcon)
            } else {
                ivTrackIcon.setImageResource(R.drawable.stat) // fallback icon
            }
            itemView.setOnClickListener { onItemClick(run) }
        }

        private fun getSnapshotFile(context: Context, timestamp: Long): File {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            return File(dir, "run_${timestamp}.png")
        }

        private fun formatDuration(durationMillis: Long): String {
            val totalSeconds = durationMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }



    private object DiffCallback : DiffUtil.ItemCallback<RunEntity>() {
        override fun areItemsTheSame(oldItem: RunEntity, newItem: RunEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RunEntity, newItem: RunEntity): Boolean =
            oldItem == newItem
    }
}
