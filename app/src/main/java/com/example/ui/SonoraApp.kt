package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.audio.SonoraAudioEngine
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.local.LocalAudioStorage
import com.example.data.local.SonoraPreferencesStore
import com.example.data.model.SonoraThemeMode
import com.example.data.model.UiState
import com.example.data.repository.SonoraRepository
import com.example.ui.components.FloatingMiniPlayer
import com.example.ui.components.GlassCard
import com.example.ui.components.SonoraBackdrop
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.ThemesAndSettingsScreen
import com.example.ui.theme.LocalSonoraDesign
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.SonoraTheme
import com.example.ui.viewmodel.SonoraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

private object SonoraRoutes {
    const val LIBRARY = "library"
    const val PLAYLISTS = "playlists"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
}

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    NavDestination(SonoraRoutes.LIBRARY, R.string.nav_library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    NavDestination(SonoraRoutes.PLAYLISTS, R.string.nav_playlists, Icons.Rounded.QueueMusic, Icons.Outlined.QueueMusic, "nav_playlists"),
    NavDestination(SonoraRoutes.EQUALIZER, R.string.nav_equalizer, Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq, "nav_equalizer"),
    NavDestination(SonoraRoutes.SETTINGS, R.string.nav_settings, Icons.Rounded.Palette, Icons.Outlined.Palette, "nav_settings")
)

@Composable
fun SonoraApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val prefsStore = remember { SonoraPreferencesStore(context.applicationContext) }
    val localPrefs by prefsStore.prefsFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.LocalSonoraPrefs()
    )

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        SonoraTheme(themeMode = localPrefs.themeMode) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedSonoraShell(
                currentUserId = uid,
                displayName = user.displayName,
                email = user.email,
                prefsStore = prefsStore,
                fallbackThemeMode = localPrefs.themeMode
            )
        }
    }
}

@Composable
private fun AuthenticatedSonoraShell(
    currentUserId: String,
    displayName: String?,
    email: String?,
    prefsStore: SonoraPreferencesStore,
    fallbackThemeMode: SonoraThemeMode
) {
    val context = LocalContext.current
    val localAudioStorage = remember { LocalAudioStorage(context.applicationContext) }
    val audioEngine = remember { SonoraAudioEngine(context.applicationContext, localAudioStorage) }

    DisposableEffect(audioEngine) {
        onDispose {
            audioEngine.release()
        }
    }

    val viewModel: SonoraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repo = SonoraRepository(app)
                SonoraViewModel(
                    repository = repo,
                    prefsStore = prefsStore,
                    localAudioStorage = localAudioStorage,
                    audioEngine = audioEngine,
                    currentUserId = currentUserId,
                    currentDisplayName = displayName,
                    appContext = app
                )
            }
        }
    )

    // Graceful runtime request for playback notifications on Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val themeMode by viewModel.activeThemeMode.collectAsStateWithLifecycle(initialValue = fallbackThemeMode)
    val offlineModeEnabled by viewModel.offlineModeEnabled.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val tracksState by viewModel.tracksState.collectAsStateWithLifecycle()
    val playlistsState by viewModel.playlistsState.collectAsStateWithLifecycle()
    val eqPresetsState by viewModel.eqPresetsState.collectAsStateWithLifecycle()
    val liveEqPreset by viewModel.liveEqPreset.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val offlineCacheMb by viewModel.offlineCacheMb.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val credentialManager = remember { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()

    var showNowPlaying by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: SonoraRoutes.LIBRARY

    val userProfile = (userProfileState as? UiState.Success)?.data
    val allTracks = (tracksState as? UiState.Success)?.data.orEmpty()
    val allPlaylists = (playlistsState as? UiState.Success)?.data.orEmpty()
    val offlineTracksCount = allTracks.count { it.offlineAvailable }

    SonoraTheme(themeMode = themeMode) {
        val design = LocalSonoraDesign.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 700.dp

            SonoraBackdrop {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isWideScreen && !showNowPlaying) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                if (playbackState.currentTrack != null) {
                                    FloatingMiniPlayer(
                                        playbackState = playbackState,
                                        onOpenPlayer = { showNowPlaying = true },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.nextTrack() },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                NavigationBar(
                                    containerColor = design.navBarFill,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .border(1.dp, design.glassBorderBrush, RoundedCornerShape(26.dp))
                                ) {
                                    TopDestinations.forEach { dest ->
                                        val selected = currentRoute == dest.route ||
                                            (dest.route == SonoraRoutes.PLAYLISTS && currentRoute.startsWith("playlist_detail"))
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(dest.route) {
                                                    popUpTo(SonoraRoutes.LIBRARY) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(dest.labelRes),
                                                    maxLines = 1
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = design.accentGlow,
                                                selectedTextColor = design.accentGlow,
                                                indicatorColor = design.accentGlow.copy(alpha = 0.16f),
                                                unselectedIconColor = design.textSecondary,
                                                unselectedTextColor = design.textSecondary
                                            ),
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                containerColor = design.navBarFill,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .border(1.dp, design.glassBorderBrush, RoundedCornerShape(26.dp))
                            ) {
                                TopDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(dest.route) {
                                                popUpTo(SonoraRoutes.LIBRARY) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = stringResource(dest.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(dest.labelRes)) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = design.accentGlow,
                                            selectedTextColor = design.accentGlow,
                                            indicatorColor = design.accentGlow.copy(alpha = 0.16f),
                                            unselectedIconColor = design.textSecondary,
                                            unselectedTextColor = design.textSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            // Top Header with Cloud/Offline Pill
                            SonoraTopStatusHeader(
                                themeMode = themeMode,
                                offlineModeEnabled = offlineModeEnabled,
                                isDeviceOnline = isDeviceOnline,
                                onToggleOffline = { viewModel.toggleOfflineMode(!offlineModeEnabled) }
                            )

                            val extraBottom = if (isWideScreen && playbackState.currentTrack != null) 88.dp else 8.dp

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = SonoraRoutes.LIBRARY
                                ) {
                                    composable(SonoraRoutes.LIBRARY) {
                                        LibraryScreen(
                                            tracksState = tracksState,
                                            playlistsState = playlistsState,
                                            activeTrackId = playbackState.currentTrack?.id,
                                            offlineModeEnabled = offlineModeEnabled,
                                            isImporting = isImporting,
                                            onImportAudio = { uris -> viewModel.importAudioUris(uris) },
                                            onPlayTracks = { tracks, idx -> viewModel.playTracks(tracks, idx) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            onToggleOffline = { viewModel.toggleTrackOffline(it) },
                                            onDeleteTrack = { viewModel.deleteTrack(it) },
                                            onAddToPlaylist = { pl, tr -> viewModel.addTrackToPlaylist(pl, tr.id) },
                                            contentBottomPadding = extraBottom
                                        )
                                    }

                                    composable(SonoraRoutes.PLAYLISTS) {
                                        PlaylistsScreen(
                                            playlistsState = playlistsState,
                                            tracksState = tracksState,
                                            onOpenPlaylist = { playlistId ->
                                                navController.navigate("playlist_detail/$playlistId")
                                            },
                                            onPlayPlaylist = { _, tracks -> viewModel.playTracks(tracks, 0) },
                                            onCreatePlaylist = { n, d, c, off -> viewModel.createPlaylist(n, d, c, off) },
                                            onTogglePlaylistOffline = { viewModel.togglePlaylistOffline(it) },
                                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                            contentBottomPadding = extraBottom
                                        )
                                    }

                                    composable(
                                        route = SonoraRoutes.PLAYLIST_DETAIL,
                                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                                    ) { entry ->
                                        val plId = entry.arguments?.getString("playlistId").orEmpty()
                                        val playlist = allPlaylists.firstOrNull { it.id == plId }
                                        PlaylistDetailScreen(
                                            playlist = playlist,
                                            allTracks = allTracks,
                                            onBack = { navController.popBackStack() },
                                            onPlayTracks = { tracks, idx -> viewModel.playTracks(tracks, idx) },
                                            onMoveTrack = { pl, from, to -> viewModel.movePlaylistTrack(pl, from, to) },
                                            onRemoveTrack = { pl, idx -> viewModel.removeTrackFromPlaylist(pl, idx) },
                                            onAddTrack = { pl, tr -> viewModel.addTrackToPlaylist(pl, tr.id) },
                                            onToggleOffline = { viewModel.togglePlaylistOffline(it) },
                                            contentBottomPadding = extraBottom
                                        )
                                    }

                                    composable(SonoraRoutes.EQUALIZER) {
                                        EqualizerScreen(
                                            livePreset = liveEqPreset,
                                            presetsState = eqPresetsState,
                                            dspAttached = playbackState.dspAttached,
                                            hardwareBandCount = playbackState.hardwareBandCount,
                                            onSelectPreset = { viewModel.selectEqPreset(it) },
                                            onUpdateLiveEq = { viewModel.updateLiveEq(it) },
                                            onPersistPreset = { viewModel.persistCurrentEqPreset(it) },
                                            onDeletePreset = { viewModel.deleteEqPreset(it) },
                                            contentBottomPadding = extraBottom
                                        )
                                    }

                                    composable(SonoraRoutes.SETTINGS) {
                                        ThemesAndSettingsScreen(
                                            activeTheme = themeMode,
                                            offlineModeEnabled = offlineModeEnabled,
                                            isDeviceOnline = isDeviceOnline,
                                            userProfile = userProfile,
                                            userEmail = email,
                                            offlineTracksCount = offlineTracksCount,
                                            totalTracksCount = allTracks.size,
                                            offlineCacheMb = offlineCacheMb,
                                            isSyncing = isSyncing,
                                            onSelectTheme = { viewModel.selectTheme(it) },
                                            onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                                            onUpdateAudioPrefs = { g, hr -> viewModel.updateAudioEnginePrefs(g, hr) },
                                            onForceSync = { viewModel.forceCloudSync() },
                                            onSignOut = {
                                                signOut(credentialManager, scope)
                                            },
                                            contentBottomPadding = extraBottom
                                        )
                                    }
                                }

                                if (isWideScreen && playbackState.currentTrack != null && !showNowPlaying) {
                                    FloatingMiniPlayer(
                                        playbackState = playbackState,
                                        onOpenPlayer = { showNowPlaying = true },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.nextTrack() },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .navigationBarsPadding()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    playbackState = playbackState,
                    eqEnabled = liveEqPreset.enabled,
                    eqPresetName = liveEqPreset.name,
                    onClose = { showNowPlaying = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onPrevious = { viewModel.previousTrack() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onToggleOffline = { viewModel.toggleTrackOffline(it) },
                    onOpenEqualizer = {
                        showNowPlaying = false
                        navController.navigate(SonoraRoutes.EQUALIZER) {
                            launchSingleTop = true
                        }
                    },
                    onPlayQueueItem = { idx -> viewModel.playTracks(playbackState.queue, idx) }
                )
            }
        }
    }
}

@Composable
private fun SonoraTopStatusHeader(
    themeMode: SonoraThemeMode,
    offlineModeEnabled: Boolean,
    isDeviceOnline: Boolean,
    onToggleOffline: () -> Unit
) {
    val design = LocalSonoraDesign.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = design.textPrimary
            )
            Text(
                text = "HI-RES • ${themeMode.titleEs.uppercase()}",
                style = MonoMetricStyle,
                color = design.accentGlow
            )
        }

        GlassCard(
            cornerRadius = 50.dp,
            onClick = onToggleOffline,
            modifier = Modifier.testTag("header_offline_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val (icon, label, tint) = when {
                    offlineModeEnabled -> Triple(Icons.Rounded.CloudOff, "Modo Offline", design.offlineBadgeColor)
                    !isDeviceOnline -> Triple(Icons.Rounded.WifiOff, "Sin Red (Caché)", design.offlineBadgeColor)
                    else -> Triple(Icons.Rounded.CloudDone, "Nube Activa", design.cloudBadgeColor)
                }
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}
