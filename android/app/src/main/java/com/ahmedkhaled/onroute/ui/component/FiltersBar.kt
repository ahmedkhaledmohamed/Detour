package com.ahmedkhaled.onroute.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FiltersBar(
    maxDetourMinutes: Float,
    onMaxDetourChange: (Float) -> Unit,
    openNowOnly: Boolean,
    onOpenNowChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Max detour: ${maxDetourMinutes.toInt()} min",
                fontSize = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Open now", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = openNowOnly,
                    onCheckedChange = onOpenNowChange,
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        Slider(
            value = maxDetourMinutes,
            onValueChange = onMaxDetourChange,
            valueRange = 1f..30f,
            steps = 28
        )
    }
}
