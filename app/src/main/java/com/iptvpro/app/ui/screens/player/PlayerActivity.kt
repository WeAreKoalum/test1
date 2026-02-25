package com.iptvpro.app.ui.screens.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.iptvpro.app.domain.model.Channel
import com.iptvpro.app.domain.model.NowNextInfo
import com.iptvpro.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val EXTRA_CHANNEL_ID = "channel_id"
        private const val EXTRA_CHANNEL_NAME = "channel_name"
        private const val EXTRA_CHANNEL_URL = "channel_url"
        private const val EXTRA_CHANNEL_LOGO = "channel_logo"
        private const val EXTRA_CHANNEL_TVG_ID = "channel_tvg_id"
        private const val EXTRA_PLAYLIST_ID = "playlist_id"
        private const val EXTRA_GROUP_ID = "group_id"

        fun launch(context: Context, channel: Channel, playlistId: Long) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(EXTRA_CHANNEL_ID, channel.id)
                    putExtra(EXTRA_CHANNEL_NAME, channel.name)
                    putExtra(EXTRA_CHANNEL_URL, channel.url)
                    putExtra(EXTRA_CHANNEL_LOGO, channel.logoUrl)
                    putExtra(EXTRA_CHANNEL_TVG_ID, channel.tvgId)
                    putExtra(EXTRA_PLAYLIST_ID, playlistId)
                    putExtra(EXTRA_GROUP_ID, channel.groupId)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val channel = Channel(
            id = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L),
            name = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "",
            url = intent.getStringExtra(EXTRA_CHANNEL_URL) ?: "",
            logoUrl = intent.getStringExtra(EXTRA_CHANNEL_LOGO),
            tvgId = intent.getStringExtra(EXTRA_CHANNEL_TVG_ID),
            tvgName = null,
            playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, 0L),
            groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0L)
        )

        requestAudioFocus()
        viewModel.init(channel, channel.playlistId)

        setContent {
            IPTVProTheme {
                PlayerScreenContent(
                    channel = channel,
                    viewModel = viewModel,
                    onBack = ::finish
                )
            }
        }
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener {}
                .build()
            audioFocusRequest = req
            audioManager?.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus({}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        super.onDestroy()
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerScreenContent(
    channel: Channel,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }

    // Auto-ocultar overlay
    LaunchedEffect(uiState.isOverlayVisible) {
        if (uiState.isOverlayVisible) {
            delay(5000)
            viewModel.hideOverlay()
        }
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = "Error al reproducir: ${error.message}"
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) playerError = null
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Superficie de vídeo
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Área de tap para mostrar overlay
        val overlayInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = overlayInteraction,
                    indication = null,
                    onClick = { viewModel.toggleOverlay() }
                )
        )

        // Buffering
        if (isBuffering && playerError == null) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // Pantalla de error
        if (playerError != null) {
            PlayerErrorOverlay(
                error = playerError!!,
                onRetry = {
                    playerError = null
                    player.prepare()
                    player.play()
                },
                onBack = onBack
            )
        }

        // Overlay de información
        AnimatedVisibility(
            visible = uiState.isOverlayVisible && playerError == null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            PlayerTopOverlay(
                channel = channel,
                isFavorite = uiState.isFavorite,
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite() }
            )
        }

        AnimatedVisibility(
            visible = uiState.isOverlayVisible && playerError == null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            PlayerBottomOverlay(nowNext = uiState.nowNext)
        }
    }
}

@Composable
private fun PlayerTopOverlay(
    channel: Channel,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(8.dp))
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = channel.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Quitar favorito" else "Añadir favorito",
                    tint = if (isFavorite) Accent else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomOverlay(nowNext: NowNextInfo?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            nowNext?.now?.let { program ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("▶ AHORA", style = MaterialTheme.typography.labelLarge, color = EpgNow)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        program.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${formatTime(program.startTime)} – ${formatTime(program.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { program.progress(System.currentTimeMillis()) },
                    modifier = Modifier.fillMaxWidth(),
                    color = EpgNow,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
            nowNext?.next?.let { program ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A CONTINUACIÓN", style = MaterialTheme.typography.labelSmall, color = EpgNext)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${program.title} · ${formatTime(program.startTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerErrorOverlay(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Error, null, tint = ErrorColor, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(error, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                Text("Reintentar")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack) { Text("Volver", color = TextSecondary) }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
