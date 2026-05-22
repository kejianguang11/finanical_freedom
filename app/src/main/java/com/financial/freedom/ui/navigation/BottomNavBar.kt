package com.financial.freedom.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Route.Home, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Route.Holdings(), "持仓", Icons.Filled.ViewList, Icons.Outlined.ViewList),
    BottomNavItem(Route.Earnings, "收益", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(Route.Settings, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun BottomNavBar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentRoute::class == item.route::class
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(text = item.label, textAlign = TextAlign.Center, fontSize = 11.sp)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}
