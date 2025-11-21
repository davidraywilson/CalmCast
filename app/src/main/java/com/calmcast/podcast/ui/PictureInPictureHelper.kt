package com.calmcast.podcast.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity

object PictureInPictureHelper {
    
    fun enterPictureInPictureMode(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(1, 1))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }
    
    fun isPictureInPictureSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.hasSystemFeature("android.software.picture_in_picture")
        } else {
            false
        }
    }
}