package ru.ruvideohub.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.ruvideohub.app.ui.App
import ru.ruvideohub.app.util.SecurePrefs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val prefs = SecurePrefs.get(this)

        setContent {
            App(prefs, this)
        }
    }
}
