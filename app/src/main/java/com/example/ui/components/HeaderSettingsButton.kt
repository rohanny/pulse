package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.Backdrop

@Composable
internal fun HeaderSettingsButton(
    heroBackdrop: Backdrop,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .pressBounce(settingsInteraction, pressedScale = 0.90f)
            .minimumInteractiveComponentSize()
            .drawBackdrop(
                backdrop = heroBackdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(10f.dp.toPx(), 22f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.16f))
                }
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.52f),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = settingsInteraction,
                indication = null,
                onClick = onClick,
                role = androidx.compose.ui.semantics.Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
