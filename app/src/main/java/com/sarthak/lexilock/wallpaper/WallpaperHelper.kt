package com.sarthak.lexilock.wallpaper

import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

object WallpaperHelper {

    fun isWallpaperActive(context: Context): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val info: WallpaperInfo? = wm.wallpaperInfo
            if (info == null) return false
            info.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    fun openWallpaperPicker(context: Context) {
        val component = ComponentName(context, "com.sarthak.lexilock.LexiWallpaperService")
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    "Please choose LexiLock from your device Live Wallpaper settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
