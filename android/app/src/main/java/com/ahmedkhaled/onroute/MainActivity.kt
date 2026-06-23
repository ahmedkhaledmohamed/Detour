package com.ahmedkhaled.onroute

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ahmedkhaled.onroute.ui.screen.OnboardingScreen
import com.ahmedkhaled.onroute.ui.theme.OnRouteTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.content.Context

private val Context.dataStore by preferencesDataStore(name = "settings")
private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnRouteTheme {
                OnRouteRoot()
            }
        }
    }
}

@Composable
fun OnRouteRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasSeenOnboarding by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        hasSeenOnboarding = context.dataStore.data
            .map { it[HAS_SEEN_ONBOARDING] ?: false }
            .first()
    }

    when (hasSeenOnboarding) {
        null -> {}
        false -> OnboardingScreen(
            onGetStarted = {
                scope.launch {
                    context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true }
                    hasSeenOnboarding = true
                }
            }
        )
        true -> MainMap()
    }
}

@Composable
fun MainMap() {
    val toronto = LatLng(43.6532, -79.3832)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(toronto, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    )
}
