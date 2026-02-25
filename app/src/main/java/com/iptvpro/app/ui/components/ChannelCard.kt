package com.iptvpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.NowNextInfo
import com.iptvpro.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    nowNext: NowNextInfo?,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .scale(if (isFocused) 1.08f else 1f)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) CardFocused else CardBackground
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        )
    ) {
        Column {
            // Logo del canal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    error = null,    // muestra placeholder de Coil si falla
                    placeholder = null
                )
                if (channel.logoUrl == null) {
                    PlaceholderChannelIcon(channelName = channel.name)
                }

                // Botón favorito superpuesto (solo visible al tener foco)
                if (isFocused) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Quitar favorito" else "Añadir favorito",
                            tint = if (isFavorite) Accent else OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Nombre del canal
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // EPG Now
                nowNext?.now?.let { now ->
                    Spacer(Modifier.height(4.dp))
                    NowNextBadge(
                        label = "Ahora",
                        text = now.title,
                        color = EpgNow
                    )
                }

                // EPG Next
                nowNext?.next?.let { next ->
                    Spacer(Modifier.height(2.dp))
                    NowNextBadge(
                        label = "Después",
                        text = next.title,
                        color = EpgNext
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderChannelIcon(channelName: String) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Primary.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = channelName.take(2).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            color = Primary
        )
    }
}
