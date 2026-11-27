package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.HydrationRepository
import com.example.data.HydroPulseDatabase
import com.example.model.AppThemeMode
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.WaterLog
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.components.FloatingPillNavigationBar
import com.example.ui.components.HydroToastHost
import com.example.ui.components.NavDestinationItem
import com.example.ui.theme.HydroPulseTheme
import com.example.viewmodel.AndroidReminderCoordinator
import com.example.viewmodel.HydrationViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Today", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop, "nav_today"),
    REMINDERS("Alerts", Icons.Filled.NotificationsActive, Icons.Outlined.Notifications, "nav_reminders"),
    HISTORY("History", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_history")
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val viewModel: HydrationViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dao = HydroPulseDatabase.getDatabase(app.applicationContext).hydrationDao()
                HydrationViewModel(
                    repository = HydrationRepository(dao),
                    reminderCoordinator = AndroidReminderCoordinator(app.applicationContext)
                )
            }
        }
    )

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val logsState by viewModel.logsState.collectAsStateWithLifecycle()
    val nextReminderEpochMs by viewModel.nextReminderEpochMs.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val loadedSettings = (settingsState as? UiState.Success)?.data

    var hasNotificationPermission by remember {
        mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            ReminderNotificationHelper.ensureChannel(context)
        }
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
        }
    }

    var selectedTab by remember { mutableStateOf(MainTab.TODAY) }
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings || selectedTab != MainTab.TODAY) {
        if (showSettings) {
            showSettings = false
        } else {
            selectedTab = MainTab.TODAY
        }
    }

    val navItems = remember {
        MainTab.entries.map { tab ->
            NavDestinationItem(
                id = tab.name,
                label = tab.label,
                selectedIcon = tab.selectedIcon,
                unselectedIcon = tab.unselectedIcon,
                testTag = tab.testTag
            )
        }
    }

    HydroPulseTheme(
        textScale = loadedSettings?.textScale ?: 0.90f,
        themeMode = loadedSettings?.themeMode ?: AppThemeMode.SYSTEM.name
    ) {
        var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
        val dark = com.example.ui.theme.LocalIsDarkTheme.current
        val window = androidx.activity.compose.LocalActivity.current?.window
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect {
            window?.let {
                val bars = androidx.core.view.WindowCompat.getInsetsController(it, view)
                bars.isAppearanceLightStatusBars = !dark
                bars.isAppearanceLightNavigationBars = !dark
            }
        }
        val bgColor = MaterialTheme.colorScheme.background
        val currentBgColor by rememberUpdatedState(bgColor)
        val rootBackdrop = rememberLayerBackdrop {
            drawRect(currentBgColor)
            drawContent()
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            val useRail = maxWidth >= 600.dp
            val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomContentPadding = if (!useRail && !showSettings) {
                100.dp + navBarBottomInset
            } else {
                24.dp + navBarBottomInset
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(rootBackdrop)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                            )
                        )
                ) {
                    if (useRail && !showSettings) {
                        NavigationRail(
                            containerColor = Color.Transparent,
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val sState = settingsState
                    val lState = logsState
                    when {
                        sState is UiState.Loading || lState is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        sState is UiState.Error -> {
                            ErrorStateBox(message = sState.message)
                        }

                        lState is UiState.Error -> {
                            ErrorStateBox(message = lState.message)
                        }

                        sState is UiState.Success && lState is UiState.Success -> {
                            val settings: UserSettings = sState.data
                            val logs: List<WaterLog> = lState.data

                            AnimatedContent(
                                targetState = showSettings,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            initialOffsetX = { it / 3 }
                                        ) + scaleIn(initialScale = 0.96f, animationSpec = tween(240)) + fadeIn(tween(240))) togetherWith
                                            (slideOutHorizontally(
                                                targetOffsetX = { -it / 5 }
                                            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)) + fadeOut(tween(170)))
                                    } else {
                                        (slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            initialOffsetX = { -it / 3 }
                                        ) + scaleIn(initialScale = 0.96f, animationSpec = tween(240)) + fadeIn(tween(240))) togetherWith
                                            (slideOutHorizontally(
                                                targetOffsetX = { it / 3 }
                                            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)) + fadeOut(tween(170)))
                                    }
                                },
                                label = "settings_subscreen_transition"
                            ) { inSettings ->
                                if (inSettings) {
                                    SettingsScreen(
                                        settings = settings,
                                        bottomContentPadding = bottomContentPadding,
                                        onBack = { showSettings = false },
                                        onUpdateSettings = { transform, msg ->
                                            viewModel.updateSettings(transform, msg)
                                        }
                                    )
                                } else {
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            val forward = targetState.ordinal > initialState.ordinal
                                            (slideInHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = 0.84f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                ),
                                                initialOffsetX = { width -> if (forward) width / 4 else -width / 4 }
                                            ) + scaleIn(initialScale = 0.97f, animationSpec = tween(220)) + fadeIn(tween(220))) togetherWith
                                                (slideOutHorizontally(
                                                    animationSpec = spring(
                                                        dampingRatio = 0.9f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    ),
                                                    targetOffsetX = { width -> if (forward) -width / 5 else width / 5 }
                                                ) + scaleOut(targetScale = 0.97f, animationSpec = tween(160)) + fadeOut(tween(160)))
                                        },
                                        label = "tab_slide_transition"
                                    ) { tab ->
                                        when (tab) {
                                            MainTab.TODAY -> TodayScreen(
                                                settings = settings,
                                                logs = logs,
                                                nextReminderEpochMs = nextReminderEpochMs,
                                                hasNotificationPermission = hasNotificationPermission,
                                                bottomContentPadding = bottomContentPadding,
                                                onLogWater = { amountMl, drinkType ->
                                                    viewModel.logWater(amountMl, drinkType)
                                                },
                                                onDeleteLog = { logId -> viewModel.deleteLog(logId) },
                                                onOpenRemindersTab = { selectedTab = MainTab.REMINDERS }
                                            )

                                            MainTab.REMINDERS -> RemindersScreen(
                                                settings = settings,
                                                nextReminderEpochMs = nextReminderEpochMs,
                                                hasNotificationPermission = hasNotificationPermission,
                                                bottomContentPadding = bottomContentPadding,
                                                onRequestNotificationPermission = requestNotificationPermission,
                                                onUpdateSettings = { transform, msg ->
                                                    viewModel.updateSettings(transform, msg)
                                                },
                                                onScheduleChanged = viewModel::refreshNextReminder,
                                                onSendTestNotification = {
                                                    viewModel.sendTestNotification()
                                                }
                                            )

                                            MainTab.HISTORY -> HistoryScreen(
                                                settings = settings,
                                                logs = logs,
                                                bottomContentPadding = bottomContentPadding,
                                                onOpenSettings = { showSettings = true },
                                                onUpdateGoal = { goalMl ->
                                                    viewModel.updateSettings(
                                                        transform = { it.copy(dailyGoalMl = goalMl) },
                                                        confirmationMessage = "Daily goal updated to $goalMl ml"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }

            AnimatedVisibility(
                visible = !useRail && !showSettings,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingPillNavigationBar(
                    backdrop = rootBackdrop,
                    items = navItems,
                    selectedIndex = selectedTab.ordinal,
                    onSelectIndex = { index ->
                        MainTab.entries.getOrNull(index)?.let { selectedTab = it }
                    }
                )
            }

            HydroToastHost(
                backdrop = rootBackdrop,
                message = feedbackMessage,
                onDismiss = { viewModel.clearFeedbackMessage() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            com.example.ui.components.SplashOverlay(visible = showSplash, onFinished = { showSplash = false })
        }
    }
}

@Composable
private fun ErrorStateBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Could not load hydration data",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
