package com.sarthak.lexilock.wallpaper

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

object WallpaperHelper {

    private const val TAG = "LexiWallpaperHelper"

    fun isWallpaperActive(context: Context): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val info: WallpaperInfo? = wm.wallpaperInfo
            val active = info?.packageName == context.packageName
            Log.d(TAG, "isWallpaperActive: $active (current component: ${info?.component?.flattenToShortString()})")
            active
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if wallpaper is active", e)
            false
        }
    }

    fun openWallpaperPicker(context: Context) {
        val component = ComponentName(context, LexiLockWallpaperService::class.java)
        Log.i(TAG, "openWallpaperPicker called for component: ${component.flattenToShortString()}")
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Successfully started ACTION_CHANGE_LIVE_WALLPAPER")
        } catch (e: Exception) {
            Log.w(TAG, "ACTION_CHANGE_LIVE_WALLPAPER failed, attempting ACTION_LIVE_WALLPAPER_CHOOSER fallback", e)
            try {
                val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                    putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", component)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                Log.i(TAG, "Successfully started ACTION_LIVE_WALLPAPER_CHOOSER fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to start wallpaper chooser", e2)
                Toast.makeText(
                    context,
                    "Please choose LexiLock from your device Live Wallpaper settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
