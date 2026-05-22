package com.financial.freedom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.ui.navigation.AppNavHost
import com.financial.freedom.ui.navigation.Route
import com.financial.freedom.ui.navigation.bottomNavItems
import com.financial.freedom.ui.theme.FinancialFreedomTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialFreedomApp(accountManager: AccountManager) {
    FinancialFreedomTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        var showAddSheet by rememberSaveable { mutableStateOf(false) }
        var startDest by remember { mutableStateOf<Route?>(null) }

        LaunchedEffect(Unit) {
            startDest = if (accountManager.hasAnyAccount()) Route.PinUnlock else Route.Welcome
        }

        val tabRoutes = listOf(
            Route.Home::class.qualifiedName!!,
            Route.Holdings::class.qualifiedName!!,
            Route.Earnings::class.qualifiedName!!,
            Route.Settings::class.qualifiedName!!
        )
        val subRoutes = listOf(
            Route.HoldingDetail::class.qualifiedName!!,
            Route.AddDeposit::class.qualifiedName!!,
            Route.EditDeposit::class.qualifiedName!!,
            Route.AddHolding::class.qualifiedName!!,
            Route.EditHolding::class.qualifiedName!!
        )
        val authRoutes = listOf(
            Route.Welcome::class.qualifiedName!!,
            Route.PinUnlock::class.qualifiedName!!,
            Route.AccountList::class.qualifiedName!!
        )
        // 使用 startsWith 而非精确匹配，因为带参数的 Route（如 Holdings?tab={tab}）的
        // NavDestination.route 会包含参数占位符，与 qualifiedName 不完全相同
        fun matchesAny(route: String?, candidates: List<String>): Boolean {
            if (route == null) return false
            return candidates.any { route.startsWith(it) }
        }
        val showBottomBar = matchesAny(currentRoute, tabRoutes) || matchesAny(currentRoute, subRoutes)
        val showFab = matchesAny(currentRoute, tabRoutes)

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute != null && currentRoute!!.startsWith(item.route::class.qualifiedName!!)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新增资产")
                    }
                }
            }
        ) { innerPadding ->
            startDest?.let { dest ->
                AppNavHost(
                    navController = navController,
                    accountManager = accountManager,
                    startDestination = dest,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        if (showAddSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("新增资产", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    AddSheetItem("存款") {
                        showAddSheet = false
                        navController.navigate(Route.AddDeposit)
                    }
                    AddSheetItem("股票") {
                        showAddSheet = false
                        navController.navigate(Route.AddHolding("STOCK"))
                    }
                    AddSheetItem("基金") {
                        showAddSheet = false
                        navController.navigate(Route.AddHolding("FUND"))
                    }
                    AddSheetItem("黄金") {
                        showAddSheet = false
                        navController.navigate(Route.AddHolding("GOLD"))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSheetItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
