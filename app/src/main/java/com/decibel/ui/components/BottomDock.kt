package com.decibel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class DockTab {
    Browse,
    Library,
    NowPlaying,
    Notifications,
    Settings,
}

private data class DockDestination(
    val tab: DockTab,
    val label: String,
    val icon: ImageVector,
)

private val DockDestinations = listOf(
    DockDestination(DockTab.Browse, "Browse", Icons.Outlined.Search),
    DockDestination(DockTab.Library, "Library", Icons.AutoMirrored.Outlined.LibraryBooks),
    DockDestination(DockTab.NowPlaying, "Playing", Icons.Outlined.PlayCircle),
    DockDestination(DockTab.Notifications, "Alerts", Icons.Outlined.Notifications),
    DockDestination(DockTab.Settings, "Settings", Icons.Outlined.Settings),
)

@Composable
fun BottomDock(
    selected: DockTab,
    onSelect: (DockTab) -> Unit,
    notificationBadge: Int = 0,
    enabled: Boolean = true,
    landscape: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (landscape) {
        NavigationRail(
            modifier = modifier.fillMaxHeight(),
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DockDestinations.forEach { dest ->
                    RailDockItem(
                        label = dest.label,
                        selected = selected == dest.tab,
                        enabled = enabled,
                        onClick = { onSelect(dest.tab) },
                        icon = dest.icon,
                        badgeCount = if (dest.tab == DockTab.Notifications) {
                            notificationBadge
                        } else {
                            0
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    } else {
        NavigationBar(
            modifier = modifier,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            DockDestinations.forEach { dest ->
                BarDockItem(
                    label = dest.label,
                    selected = selected == dest.tab,
                    enabled = enabled,
                    onClick = { onSelect(dest.tab) },
                    icon = dest.icon,
                    badgeCount = if (dest.tab == DockTab.Notifications) {
                        notificationBadge
                    } else {
                        0
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.BarDockItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    badgeCount: Int = 0,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        icon = { DockIcon(icon = icon, label = label, badgeCount = badgeCount) },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun ColumnScope.RailDockItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    badgeCount: Int = 0,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        icon = { DockIcon(icon = icon, label = label, badgeCount = badgeCount) },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun DockIcon(
    icon: ImageVector,
    label: String,
    badgeCount: Int,
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
