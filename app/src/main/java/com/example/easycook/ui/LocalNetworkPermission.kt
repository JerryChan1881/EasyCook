package com.example.easycook.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Autorisation d'accès au réseau local, exigée à partir d'Android 17 pour les applications qui le
 * ciblent (targetSdk 37) : sans elle, les connexions entre téléphones échouent sur un délai d'attente.
 *
 * Elle fait partie du groupe « Appareils à proximité » ; avant Android 17, elle n'existe pas et
 * l'accès au réseau local est couvert par l'autorisation Internet.
 */
object LocalNetworkPermission {

    const val NAME = "android.permission.ACCESS_LOCAL_NETWORK"

    /** Android 17 (API 37), première version où l'autorisation existe. */
    private const val FIRST_SDK = 37

    val isRequired: Boolean
        get() = Build.VERSION.SDK_INT >= FIRST_SDK

    fun isGranted(context: Context): Boolean =
        !isRequired || ContextCompat.checkSelfPermission(context, NAME) == PackageManager.PERMISSION_GRANTED
}

/**
 * Renvoie une fonction à appeler avant tout transfert : elle lance l'action tout de suite si
 * l'accès au réseau local est déjà accordé, sinon demande l'autorisation, puis lance l'action
 * si l'utilisateur accepte ou appelle [onDenied] s'il refuse.
 */
@Composable
fun rememberLocalNetworkAccess(onDenied: () -> Unit): (onGranted: () -> Unit) -> Unit {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAction
        pendingAction = null
        if (granted) action?.invoke() else onDenied()
    }

    return { onGranted ->
        if (LocalNetworkPermission.isGranted(context)) {
            onGranted()
        } else {
            pendingAction = onGranted
            launcher.launch(LocalNetworkPermission.NAME)
        }
    }
}
