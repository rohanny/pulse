package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.AquaGlow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SplashOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tideLevel = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(18f) }
    val cardAlpha = remember { Animatable(0f) }
    val progressBar = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                tideLevel.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                progressBar.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1480, easing = FastOutSlowInEasing)
                )
            }
            delay(120L)
            launch {
                cardAlpha.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }
            launch {
                titleOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
            }
            delay(1420L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(420)) + slideOutVertically(
            animationSpec = tween(420, easing = FastOutSlowInEasing),
            targetOffsetY = { -it / 10 }
        ),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "splash_wave_fx")
        val wavePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_wave_phase"
        )
        val backPhase by infiniteTransition.animateFloat(
            initialValue = PI.toFloat(),
            targetValue = (3f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_back_wave_phase"
        )
        val particleDrift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_particle_drift"
        )

        val accent = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.primary else AquaGlow
        val splashInk = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.onSurface else Color(0xFFEAF2F7)
        val goalColor = MaterialTheme.colorScheme.tertiary
        val splashBg = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.background else colorResource(id = R.color.splash_bg)
        val backdrop = rememberLayerBackdrop {
            drawRect(splashBg)
            drawContent()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(splashBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished
                )
                .testTag("splash_overlay")
        ) {
            // Pure procedural rising dual-wave tide + drifting particles captured by Kyant Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Subtle geometric constellation dots in upper sky region
                    val dotSpacing = 36.dp.toPx()
                    val cols = (w / dotSpacing).toInt()
                    val rows = ((h * 0.56f) / dotSpacing).toInt()
                    for (r in 1..rows) {
                        for (c in 1..cols) {
                            val px = c * dotSpacing
                            val py = r * dotSpacing
                            val pulse = 0.5f + 0.5f * sin(wavePhase + (c + r) * 0.6f).toFloat()
                            drawCircle(
                                color = Color.White.copy(alpha = 0.045f + pulse * 0.055f),
                                radius = 1.4.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Rising dual-wave liquid tide straddling the center-lower glass card
                    val targetWaterlineY = h * 0.50f
                    val baseLineY = h + 24.dp.toPx() - (h + 24.dp.toPx() - targetWaterlineY) * tideLevel.value

                    // Back wave crest
                    val backAmp = 14.dp.toPx()
                    val backPath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLineY)
                        var x = 0f
                        while (x <= w + 12f) {
                            val norm = x / w
                            val y = baseLineY - 12.dp.toPx() + sin(norm * 2.2f * PI + backPhase).toFloat() * backAmp
                            lineTo(x, y)
                            x += 10f
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = backPath,
                        color = goalColor.copy(alpha = 0.24f)
                    )

                    // Front wave crest
                    val frontAmp = 18.dp.toPx()
                    val frontPath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, baseLineY)
                        var x = 0f
                        while (x <= w + 12f) {
                            val norm = x / w
                            val y = baseLineY + sin(norm * 2.6f * PI + wavePhase).toFloat() * frontAmp
                            lineTo(x, y)
                            x += 10f
                        }
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = frontPath,
                        color = accent.copy(alpha = 0.34f)
                    )

                    // Luminous crest hairline along front wave
                    val crestStroke = Path().apply {
                        var x = 0f
                        val firstY = baseLineY + sin(wavePhase).toFloat() * frontAmp
                        moveTo(0f, firstY)
                        while (x <= w + 12f) {
                            val norm = x / w
                            val y = baseLineY + sin(norm * 2.6f * PI + wavePhase).toFloat() * frontAmp
                            lineTo(x, y)
                            x += 10f
                        }
                    }
                    drawPath(
                        path = crestStroke,
                        color = accent.copy(alpha = 0.72f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Rising glowing micro-bubbles within the liquid tide
                    val bubbleSpecs = listOf(
                        Triple(0.14f, 0.05f, 3.2f),
                        Triple(0.29f, 0.38f, 4.5f),
                        Triple(0.46f, 0.72f, 2.6f),
                        Triple(0.64f, 0.21f, 3.8f),
                        Triple(0.79f, 0.56f, 4.2f),
                        Triple(0.91f, 0.84f, 2.8f)
                    )
                    val travel = (h - baseLineY).coerceAtLeast(40f)
                    bubbleSpecs.forEachIndexed { idx, (xRatio, phaseOffset, rDp) ->
                        val t = (particleDrift + phaseOffset) % 1f
                        val bx = w * xRatio + sin(t * 2f * PI + idx).toFloat() * 10.dp.toPx()
                        val by = h - t * travel
                        if (by > baseLineY + 12.dp.toPx()) {
                            drawCircle(
                                color = Color.White.copy(alpha = (1f - t) * 0.42f),
                                radius = rDp.dp.toPx(),
                                center = Offset(bx, by)
                            )
                        }
                    }
                }
            }

            // Frosted Kyant Backdrop glass card straddling the animated wave horizon
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .graphicsLayer {
                            alpha = cardAlpha.value
                            translationY = titleOffsetY.value
                        }
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HYDROPULSE",
                        style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 3.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .width(34.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.85f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Calm hydration, paced to your day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


