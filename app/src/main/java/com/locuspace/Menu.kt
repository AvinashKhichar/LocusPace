package com.locuspace

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

class Menu : BaseActivity() {

    private lateinit var home : MaterialTextView
    private lateinit var statistic: MaterialTextView
    private lateinit var backup: MaterialTextView
    private lateinit var social: MaterialTextView
    private lateinit var settings : MaterialTextView
    private lateinit var account: MaterialTextView
    private lateinit var logout: MaterialTextView
    private lateinit var theme: MaterialSwitch
    private lateinit var toolbar: MaterialToolbar
    private lateinit var profile : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.nav)

        home = findViewById(R.id.nav_home)
        statistic = findViewById(R.id.nav_statistics)
        backup = findViewById(R.id.nav_backup)
        social = findViewById(R.id.nav_social)
        settings = findViewById(R.id.nav_setting)
        account = findViewById(R.id.tv_user_name)
        profile = findViewById(R.id.iv_user_avatar)
        logout = findViewById(R.id.tv_logout)
        theme = findViewById(R.id.switch_theme)

        toolbar = findViewById(R.id.toolbar_profile)
        setSupportActionBar(toolbar)

        profile.scaleType = ImageView.ScaleType.CENTER_CROP
        profile.setImageResource(R.drawable.tanjiro)

        clickListeners()
        enableImmersiveMode()

    }

    private fun clickListeners(){
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        home.setOnClickListener {
            val intent = Intent(this, TrackRun::class.java)
            startActivity(intent)
        }

        statistic.setOnClickListener {
            val intent = Intent(this, Statistic::class.java)
            startActivity(intent)
        }
        backup.setOnClickListener {
            val intent = Intent(this, Backup::class.java)
            startActivity(intent)
        }
        social.setOnClickListener {
            val intent = Intent(this, Social::class.java)
            startActivity(intent)
        }
        settings.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }
        logout.setOnClickListener {
            finishAffinity()
            System.exit(0)
        }

        account.setOnClickListener {
            Toast.makeText(this, "Not Implemented Yet... Will get back to it soon", Toast.LENGTH_SHORT).show()
        }
        theme.setOnClickListener {
            Toast.makeText(this, "Not Implemented Yet... Change from your device settings", Toast.LENGTH_SHORT).show()
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