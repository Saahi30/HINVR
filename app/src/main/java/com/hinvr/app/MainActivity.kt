package com.hinvr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.hinvr.app.navigation.HinvrNavHost
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.theme.HinvrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HinvrApplication
        setContent {
            CompositionLocalProvider(
                LocalSessionRepository provides app.sessionRepository,
                LocalCatalogRepository provides app.catalogRepository,
            ) {
                HinvrTheme {
                    HinvrNavHost()
                }
            }
        }
    }
}
