package com.sowrov.germanarticlebangla.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sowrov.germanarticlebangla.BuildConfig
import com.sowrov.germanarticlebangla.data.ThemePref
import com.sowrov.germanarticlebangla.ui.admin.AdminScreen
import com.sowrov.germanarticlebangla.ui.explore.ExploreScreen
import com.sowrov.germanarticlebangla.ui.practice.PracticeScreen
import com.sowrov.germanarticlebangla.ui.premium.PremiumScreen
import com.sowrov.germanarticlebangla.ui.stats.StatsScreen
import com.sowrov.germanarticlebangla.ui.theme.WortArtTheme
import com.sowrov.germanarticlebangla.ui.theme.WortTheme

@Composable
fun WortArtRoot(vm: AppViewModel) {
    val themePref by vm.themePref.collectAsStateWithLifecycle()
    val dark = when (themePref) {
        ThemePref.DARK -> true
        ThemePref.LIGHT -> false
        ThemePref.SYSTEM -> isSystemInDarkTheme()
    }
    WortArtTheme(dark = dark) { RootContent(vm, dark) }
}

@Composable
private fun RootContent(vm: AppViewModel, dark: Boolean) {
    val c = WortTheme.colors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val onToggleReminders: (Boolean) -> Unit = { on ->
        vm.setReminders(on)
        if (on && Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = vm.showAdmin || vm.tab != Tab.Practice) {
        if (vm.showAdmin) vm.closeAdmin() else vm.selectTab(Tab.Practice)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (vm.showAdmin) {
            Box(Modifier.widthIn(max = 520.dp).fillMaxSize().statusBarsPadding()) {
                AdminScreen(onBack = { vm.closeAdmin() })
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (vm.tab) {
                        Tab.Practice -> PracticeScreen(vm, onToggleReminders)
                        Tab.Explore -> ExploreScreen(vm)
                        Tab.Stats -> StatsScreen(vm)
                        Tab.Premium -> PremiumScreen(vm)
                    }
                }
                BottomBar(vm)
            }
        }
    }

    val notice = vm.limitNotice
    if (notice != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissNotice() },
            containerColor = c.card,
            icon = { Icon(AppIcons.Crown, null, tint = c.primary) },
            title = { Text(notice.title, color = c.foreground) },
            text = {
                Text(
                    when {
                        BuildConfig.PLAY_BUILD && notice.resets -> "${notice.body} It resets tomorrow."
                        notice.resets -> "${notice.body} It resets tomorrow — or go Premium for unlimited access."
                        else -> notice.body
                    },
                    color = c.mutedForeground,
                )
            },
            confirmButton = {
                if (BuildConfig.PLAY_BUILD) {
                    TextButton(onClick = { vm.dismissNotice() }) { Text("OK", color = c.primary) }
                } else {
                    TextButton(onClick = { vm.dismissNotice(); vm.openPremiumTab() }) {
                        Text("Get Premium", color = c.primary)
                    }
                }
            },
            dismissButton = {
                if (!BuildConfig.PLAY_BUILD) {
                    TextButton(onClick = { vm.dismissNotice() }) { Text("Not now", color = c.foreground) }
                }
            },
        )
    }
}

private class NavItem(val tab: Tab, val label: String, val icon: ImageVector)

@Composable
private fun BottomBar(vm: AppViewModel) {
    val c = WortTheme.colors
    val items = listOf(
        NavItem(Tab.Practice, "Practice", AppIcons.Layers),
        NavItem(Tab.Explore, "Explore", AppIcons.Grid),
        NavItem(Tab.Stats, "Stats", AppIcons.Chart),
        if (BuildConfig.PLAY_BUILD) {
            NavItem(Tab.Premium, "Members", AppIcons.Person)
        } else {
            NavItem(Tab.Premium, "Premium", AppIcons.Crown)
        },
    )
    NavigationBar(containerColor = c.card, contentColor = c.foreground) {
        for (item in items) {
            NavigationBarItem(
                selected = vm.tab == item.tab,
                onClick = { vm.selectTab(item.tab) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.primaryForeground,
                    selectedTextColor = c.primary,
                    indicatorColor = c.primary,
                    unselectedIconColor = c.mutedForeground,
                    unselectedTextColor = c.mutedForeground,
                ),
            )
        }
    }
}
