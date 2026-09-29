package com.hinvr.app.push

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.data.SessionSnapshot
import com.hinvr.app.navigation.LocalSessionRepository

@Composable
fun PushRegistrationEffect() {
    val session = LocalSessionRepository.current
    val snap by session.snapshot.collectAsStateWithLifecycle(initialValue = SessionSnapshot())
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(snap.isLoggedIn) {
        if (!snap.isLoggedIn) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        session.registerPush()
    }
}
