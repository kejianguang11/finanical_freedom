package com.financial.freedom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financial.freedom.data.DefaultDataSeeder
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.ui.navigation.AppNavHost
import com.financial.freedom.ui.navigation.PagerPage
import com.financial.freedom.ui.navigation.Route

import com.financial.freedom.ui.navigation.bottomNavIndexFor
import com.financial.freedom.ui.navigation.bottomNavItems
import com.financial.freedom.ui.theme.FinancialColors
import com.financial.freedom.ui.theme.FinancialFreedomTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialFreedomApp(accountManager: AccountManager, defaultDataSeeder: DefaultDataSeeder) {
    FinancialFreedomTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        var showAddSheet by rememberSaveable { mutableStateOf(false) }
        var startDest by remember { mutableStateOf<Route?>(null) }
        var currentPage by remember { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        // Pager state hoisted here so bottom nav can control it
        val pagerState = rememberPagerState(pageCount = { 10 })

        LaunchedEffect(Unit) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                defaultDataSeeder.seedIfNeeded()
            }
            startDest = if (accountManager.hasAnyAccount()) Route.PinUnlock else Route.Welcome
        }

        val mainRoutes = listOf(
            Route.Main::class.qualifiedName!!,
            Route.Home::class.qualifiedName!!
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

        fun matchesAny(route: String?, candidates: List<String>): Boolean {
            if (route == null) return false
            return candidates.any { route.startsWith(it) }
        }

        val isMainOrSub = matchesAny(currentRoute, mainRoutes) || matchesAny(currentRoute, subRoutes)
        val isSubRoute = matchesAny(currentRoute, subRoutes)
        val isAuth = matchesAny(currentRoute, authRoutes)
        val showBottomBar = isMainOrSub
        val showFab = matchesAny(currentRoute, mainRoutes)

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEachIndexed { index, item ->
                            val selected = bottomNavIndexFor(currentPage) == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (isSubRoute) {
                                        navController.popBackStack(Route.Main, inclusive = false)
                                    }
                                    val anchor = PagerPage.anchorFor(item.label)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(anchor)
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
                    FloatingActionButton(
                        onClick = { showAddSheet = true },
                        containerColor = FinancialColors.gold
                    ) {
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
                    pagerState = pagerState,
                    onPageChanged = { page -> currentPage = page },
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
