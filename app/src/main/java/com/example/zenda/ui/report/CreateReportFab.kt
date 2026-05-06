package com.example.zenda.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CreateReportFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Nuevo reporte ciudadano"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 400f),
        label = "fab_scale"
    )
    val primary = MaterialTheme.colorScheme.primary
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = FloatingActionButtonDefaults.shape,
                spotColor = primary.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.12f)
            )
            .scale(scale),
        containerColor = primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 8.dp
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = contentDescription
        )
    }
}
