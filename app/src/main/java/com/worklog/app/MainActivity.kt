package com.worklog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.worklog.app.ui.navigation.AppNavigation
import com.worklog.app.ui.theme.WorkLogTheme
import com.worklog.app.ui.theme.GrayBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GrayBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
