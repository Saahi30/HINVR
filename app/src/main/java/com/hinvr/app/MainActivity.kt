package com.hinvr.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.i18n.ProvideAppLanguage
import com.hinvr.app.navigation.HinvrNavHost
import com.hinvr.app.navigation.LocalCatalogRepository
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.push.EXTRA_MANDIR
import com.hinvr.app.push.EXTRA_VIEW_IN_VR
import com.hinvr.app.push.PUSH_OPEN_AARTI
import com.hinvr.app.push.PUSH_OPEN_NOTIFICATIONS
import com.hinvr.app.ui.theme.HinvrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HinvrApplication
        deliverAuthIntent(intent, app)
        consumePushIntent(intent, app)
        setContent {
            val language by app.sessionRepository.storedLanguage.collectAsStateWithLifecycle()
            CompositionLocalProvider(
                LocalSessionRepository provides app.sessionRepository,
                LocalCatalogRepository provides app.catalogRepository,
            ) {
                ProvideAppLanguage(language) {
                    HinvrTheme {
                        HinvrNavHost()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as HinvrApplication
        deliverAuthIntent(intent, app)
        consumePushIntent(intent, app)
    }

    private fun consumePushIntent(intent: Intent?, app: HinvrApplication) {
        when (intent?.getStringExtra("open")) {
            PUSH_OPEN_NOTIFICATIONS -> app.requestOpenNotifications()
            PUSH_OPEN_AARTI -> intent.getStringExtra(EXTRA_MANDIR)?.let { id ->
                app.requestOpenAarti(id, intent.getBooleanExtra(EXTRA_VIEW_IN_VR, false))
            }
            else -> return
        }
        intent.removeExtra("open")
    }

    private fun deliverAuthIntent(intent: Intent?, app: HinvrApplication) {
        if (intent?.data == null) return
        app.sessionRepository.consumeAuthIntent(intent)
        intent.data = null
    }
}
