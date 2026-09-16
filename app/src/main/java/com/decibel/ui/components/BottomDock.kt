package com.decibel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.Bell
import com.mariesta.menzies.washui.icons.lucide.List
import com.mariesta.menzies.washui.icons.lucide.Radio
import com.mariesta.menzies.washui.icons.lucide.Search
import com.mariesta.menzies.washui.icons.lucide.Settings
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme

enum class DockTab {
    Browse,
    Library,
    NowPlaying,
    Notifications,
    Settings,
}

@Composable
fun BottomDock(
    selected: DockTab,
    onSelect: (DockTab) -> Unit,
    notificationBadge: Int = 0,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = WashTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.base_100.copy(alpha = 0.94f))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DockItem(
            label = "Browse",
            selected = selected == DockTab.Browse,
            enabled = enabled,
            onClick = { onSelect(DockTab.Browse) },
            icon = LucideIcons.Search,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            label = "Library",
            selected = selected == DockTab.Library,
            enabled = enabled,
            onClick = { onSelect(DockTab.Library) },
            icon = LucideIcons.List,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            label = "Playing",
            selected = selected == DockTab.NowPlaying,
            enabled = enabled,
            onClick = { onSelect(DockTab.NowPlaying) },
            icon = LucideIcons.Radio,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            label = "Alerts",
            selected = selected == DockTab.Notifications,
            enabled = enabled,
            onClick = { onSelect(DockTab.Notifications) },
            icon = LucideIcons.Bell,
            badgeCount = notificationBadge,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            label = "Settings",
            selected = selected == DockTab.Settings,
            enabled = enabled,
            onClick = { onSelect(DockTab.Settings) },
            icon = LucideIcons.Settings,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DockItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(colors.radiusField)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.wash_a.copy(alpha = 0.7f) else colors.base_200.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            WashIcon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) colors.primary else colors.ink_muted,
                size = 20.dp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(if (badgeCount > 9) 16.dp else 14.dp)
                        .clip(CircleShape)
                        .background(colors.error),
                    contentAlignment = Alignment.Center,
                ) {
                    WashText(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = colors.base_100,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        WashText(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.primary else colors.ink_muted,
        )
    }
}
