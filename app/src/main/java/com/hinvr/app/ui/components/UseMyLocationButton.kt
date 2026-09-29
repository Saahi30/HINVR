package com.hinvr.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.hinvr.app.R
import com.hinvr.app.data.DeviceAddress
import com.hinvr.app.data.ResolvedPlace
import com.hinvr.app.i18n.userMessage
import kotlinx.coroutines.launch

@Composable
fun UseMyLocationButton(
    onPlace: (ResolvedPlace) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun lookup() {
        if (busy) return
        busy = true
        scope.launch {
            runCatching { DeviceAddress.resolve(context) }
                .onSuccess(onPlace)
                .onFailure { onError(context.userMessage(it.message, R.string.err_location)) }
            busy = false
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        val allowed = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (allowed) lookup() else onError(context.getString(R.string.err_location_off))
    }

    HinvrTextButton(
        text = if (busy) stringResource(R.string.finding_address) else stringResource(R.string.use_location),
        onClick = {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (fine || coarse) lookup()
            else {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        },
    )
}
