package com.ahmedkhaled.onroute.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.ui.graphics.vector.ImageVector

enum class TravelMode(val apiValue: String, val label: String, val icon: ImageVector) {
    DRIVE("DRIVE", "Drive", Icons.Default.DirectionsCar),
    WALK("WALK", "Walk", Icons.Default.DirectionsWalk),
    BIKE("BICYCLE", "Bike", Icons.Default.DirectionsBike)
}
