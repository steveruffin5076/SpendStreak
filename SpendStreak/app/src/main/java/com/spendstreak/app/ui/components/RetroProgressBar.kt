package com.spendstreak.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// A blocky, segmented bar in the style of a retro RPG HP/XP meter.
@Composable
fun RetroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 10,
    filledColor: Color = MaterialTheme.colorScheme.primary,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600)
    )
    val filledSegments = if (animatedProgress.isNaN()) 0 else (animatedProgress * segments).roundToInt()
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { index ->
            val isFilled = index < filledSegments
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(if (isFilled) filledColor else emptyColor)
                    .border(1.dp, borderColor)
            )
        }
    }
}
