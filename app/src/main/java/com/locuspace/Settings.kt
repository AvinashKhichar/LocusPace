package com.locuspace

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textview.MaterialTextView

class Settings : BaseActivity() {

    private lateinit var screenshotSwitch: MaterialSwitch
    private lateinit var toolbar : MaterialToolbar
    private lateinit var appRepository : MaterialTextView

    private lateinit var processData : MaterialTextView

    private lateinit var tutorial : MaterialTextView

    private lateinit var version : MaterialTextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.settings)

        screenshotSwitch = findViewById(R.id.switch_screenshot)
        screenshotSwitch.isChecked = PreferenceManager.isScreenshotPrevented(this)

        screenshotSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Settings", "Prevent screenshots = $isChecked")
            PreferenceManager.setPreventScreenshot(this, isChecked)

            if (isChecked) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            val hasFlagSecure =
                (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            Log.d("Settings", "FLAG_SECURE after toggle: $hasFlagSecure")
        }

        toolbar = findViewById(R.id.toolbar_settings)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        appRepository = findViewById(R.id.btn_app_repo)
        processData = findViewById(R.id.btn_data_processing)
        tutorial = findViewById(R.id.btn_app_tutorial)
        version = findViewById(R.id.tv_app_version)

        val appVerison = "1.0.0"
        version.text = appVerison

        enableImmersiveMode()
        onClickListeners()


    }


    private fun onClickListeners(){
        appRepository.setOnClickListener {
            openGithub()
        }
        processData.setOnClickListener {
            openDataprocessing()
        }
        tutorial.setOnClickListener {
            openTutorial()
        }
    }

    private fun openDataprocessing(){
        val intent = Intent(this, DataProcessing::class.java)
        startActivity(intent)
    }

    private fun openTutorial(){
        val intent = Intent(this, Tutorial::class.java)
        startActivity(intent)
        Log.d("Tutorial", "Tutorial button clicked")
    }


    private fun openGithub() {
        val repoLink = "https://github.com/AvinashKhichar/LocusPace"

        val gitIntent = Intent(Intent.ACTION_VIEW , Uri.parse(repoLink)).apply{
            `package` = "com.github.android"
        }

        if(gitIntent.resolveActivity(packageManager)!=null){
            startActivity(gitIntent)
        }else{
            val webIntent = Intent(
                Intent.ACTION_VIEW , Uri.parse(repoLink)
            )
            startActivity(webIntent)
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