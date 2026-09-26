package com.hinvr.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Height of the floating tab dock that tab screens scroll beneath. Tab screens
 * add it to the end of their scrolling content so the last item can clear the dock.
 */
val LocalDockClearance = compositionLocalOf<Dp> { 0.dp }
