package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DailyHydrationSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaterWaveGauge(
    currentMl: Long,
    goalMl: Long,
    modifier: Modifier = Modifier
) {
    val rawProgress = if (goalMl > 0L) (currentMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 110f),
        label = "hydration_progress"
    )
    val animatedDisplayMl by animateIntAsState(
        targetValue = currentMl.toInt().coerceAtLeast(0),
        animationSpec = tween(durationMillis = 540),
        label = "hydration_counter"
    )

    // Pulse ripple and bounce whenever water is logged
    var lastObservedMl by remember { mutableLongStateOf(currentMl) }
    val pulseRadiusFactor = remember { Animatable(0f) }
    val pulseAlpha = remember { Animatable(0f) }
    val gaugeBounce = remember { Animatable(1f) }

    LaunchedEffect(currentMl) {
        if (currentMl > lastObservedMl) {
            launch {
                gaugeBounce.snapTo(1.045f)
                gaugeBounce.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                pulseRadiusFactor.snapTo(0f)
                pulseAlpha.snapTo(0.45f)
                launch {
                    pulseRadiusFactor.animateTo(1f, animationSpec = tween(680))
                }
                pulseAlpha.animateTo(0f, animationSpec = tween(680))
            }
        }
        lastObservedMl = currentMl
    }

    val goalColor = MaterialTheme.colorScheme.tertiary
    val goalMet = goalMl > 0L && currentMl >= goalMl
    val ringAccent by animateColorAsState(
        targetValue = if (goalMet) goalColor else MaterialTheme.colorScheme.outline,
        animationSpec = tween(420),
        label = "gauge_ring_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "wave_motion")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = PI.toFloat(),
        targetValue = (3f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "back_wave_phase"
    )
    val bubbleDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_drift"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val waterFill = if (com.example.ui.theme.LocalIsDarkTheme.current) com.example.ui.theme.DarkWaterBlue else primaryColor
    val surfaceColor = MaterialTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val gaugeSize = minOf(maxWidth, 224.dp)
        Box(
            modifier = Modifier
                .size(gaugeSize)
                .graphicsLayer {
                    scaleX = gaugeBounce.value
                    scaleY = gaugeBounce.value
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f - 8.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                if (pulseAlpha.value > 0.01f) {
                    val rippleRadius = radius + pulseRadiusFactor.value * 12.dp.toPx()
                    drawCircle(
                        color = (if (goalMet) goalColor else primaryColor).copy(alpha = pulseAlpha.value),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                drawCircle(
                    color = surfaceColor,
                    radius = radius,
                    center = center
                )

                val circleClip = Path().apply {
                    addOval(
                        Rect(
                            left = center.x - radius,
                            top = center.y - radius,
                            right = center.x + radius,
                            bottom = center.y + radius
                        )
                    )
                }

                clipPath(circleClip) {
                    val fillHeight = radius * 2f * animatedProgress
                    val baseWaterTop = (center.y + radius) - fillHeight

                    if (animatedProgress > 0.005f) {
                        val backAmp = if (animatedProgress >= 0.99f) 1.8.dp.toPx() else 4.5.dp.toPx()
                        val backWavePath = Path().apply {
                            moveTo(center.x - radius, center.y + radius)
                            var x = center.x - radius
                            while (x <= center.x + radius + 4f) {
                                val norm = (x - (center.x - radius)) / (radius * 2f)
                                val y = baseWaterTop - 2.dp.toPx() + sin(norm * 2f * PI + backWavePhase).toFloat() * backAmp
                                lineTo(x, y)
                                x += 4f
                            }
                            lineTo(center.x + radius, center.y + radius)
                            close()
                        }
                        drawPath(
                            path = backWavePath,
                            color = primaryColor.copy(alpha = 0.34f)
                        )

                        val waveAmp = if (animatedProgress >= 0.99f) 2.dp.toPx() else 5.dp.toPx()
                        val wavePath = Path().apply {
                            moveTo(center.x - radius, center.y + radius)
                            var x = center.x - radius
                            while (x <= center.x + radius + 4f) {
                                val norm = (x - (center.x - radius)) / (radius * 2f)
                                val y = baseWaterTop + sin(norm * 2f * PI + wavePhase).toFloat() * waveAmp
                                lineTo(x, y)
                                x += 4f
                            }
                            lineTo(center.x + radius, center.y + radius)
                            close()
                        }
                        drawPath(
                            path = wavePath,
                            color = waterFill.copy(alpha = 0.86f)
                        )

                        // Subtle rising micro-bubbles inside filled water
                        if (animatedProgress > 0.14f) {
                            val bottomY = center.y + radius
                            val travel = (fillHeight - 10.dp.toPx()).coerceAtLeast(12f)
                            val bubbleSeeds = listOf(-0.42f to 0.0f, 0.18f to 0.35f, 0.48f to 0.72f)
                            bubbleSeeds.forEachIndexed { idx, (xRatio, phaseOffset) ->
                                val t = (bubbleDrift + phaseOffset) % 1f
                                val bx = center.x + xRatio * radius + sin(t * 2f * PI + idx).toFloat() * 4.dp.toPx()
                                val by = bottomY - (t * travel)
                                if (by > baseWaterTop + 6.dp.toPx()) {
                                    drawCircle(
                                        color = Color.White.copy(alpha = (1f - t) * 0.28f),
                                        radius = (2.2f + (idx % 2) * 1.1f).dp.toPx(),
                                        center = Offset(bx, by)
                                    )
                                }
                            }
                        }
                    }
                }

                // Hairline glass border with goal-met mint glow
                drawCircle(
                    color = ringAccent,
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (goalMet) 2.dp.toPx() else 1.25.dp.toPx())
                )
            }

            val highFill = animatedProgress > 0.56f
            val mainTextColor by animateColorAsState(
                targetValue = if (highFill) Color.White else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(260),
                label = "gauge_main_text"
            )
            val subTextColor by animateColorAsState(
                targetValue = if (highFill) Color.White.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(260),
                label = "gauge_sub_text"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "$animatedDisplayMl",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor
                )
                Text(
                    text = "/ $goalMl ml",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTextColor
                )
            }
        }
    }
}

@Composable
fun WeeklyHydrationBarChart(
    summaries: List<DailyHydrationSummary>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val mutedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val goalLineColor = MaterialTheme.colorScheme.outline

    val maxScale = maxOf(
        summaries.maxOfOrNull { maxOf(it.totalMl, it.goalMl) } ?: 2500L,
        1000L
    ).toFloat()

    // Staggered bar animation per day column
    val barMultipliers = List(summaries.size) { index ->
        var started by remember(summaries.size, index) { mutableStateOf(false) }
        LaunchedEffect(summaries.size) {
            delay(index * 55L)
            started = true
        }
        val anim by animateFloatAsState(
            targetValue = if (started) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 170f),
            label = "chart_bar_$index"
        )
        anim
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            if (summaries.isEmpty()) return@Canvas
            val count = summaries.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.34f).coerceAtMost(16.dp.toPx())
            val chartHeight = size.height - 8.dp.toPx()

            val goalRatio = (summaries.last().goalMl.toFloat() / maxScale).coerceIn(0f, 1f)
            val goalY = size.height - (chartHeight * goalRatio)
            drawLine(
                color = goalLineColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            summaries.forEachIndexed { index, summary ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                val fillRatio = (summary.totalMl.toFloat() / maxScale).coerceIn(0f, 1f) * barMultipliers[index]
                val barHeight = (chartHeight * fillRatio).coerceAtLeast(if (summary.totalMl > 0) 4.dp.toPx() else 2.dp.toPx())
                val metGoal = summary.totalMl >= summary.goalMl
                val barColor = when {
                    summary.totalMl == 0L -> goalLineColor.copy(alpha = 0.55f)
                    metGoal -> goalColor
                    index == summaries.lastIndex -> primaryColor
                    else -> mutedBarColor
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            summaries.forEach { summary ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = summary.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
