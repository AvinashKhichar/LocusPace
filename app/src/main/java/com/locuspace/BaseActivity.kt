package com.locuspace

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preventScreenshot = PreferenceManager.isScreenshotPrevented(this)
        Log.d("BaseActivity", "preventScreenshot = $preventScreenshot")

        if (preventScreenshot) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        val hasFlagSecure =
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        Log.d("BaseActivity", "FLAG_SECURE currently set: $hasFlagSecure")
    }
}
