package com.locuspace

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {

    private const val PREF_NAME = "locuspace_prefs"
    private const val KEY_PREVENT_SCREENSHOT = "prevent_screenshot"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setPreventScreenshot(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREVENT_SCREENSHOT, enabled).apply()
    }

    fun isScreenshotPrevented(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PREVENT_SCREENSHOT, false)
    }
}
