package com.financial.freedom.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.ui.auth.AccountListScreen
import com.financial.freedom.ui.auth.PinUnlockScreen
import com.financial.freedom.ui.auth.WelcomeScreen
import com.financial.freedom.ui.holdings.AddHoldingScreen
import com.financial.freedom.ui.holdings.EditHoldingScreen
import com.financial.freedom.ui.holdings.GoldDetailScreen
import com.financial.freedom.ui.holdings.HoldingDetailScreen
import com.financial.freedom.ui.holdings.deposit.AddDepositScreen
import com.financial.freedom.ui.holdings.deposit.BankDepositsScreen
import com.financial.freedom.ui.holdings.deposit.EditDepositScreen
import com.financial.freedom.ui.holdings.deposit.MaturedDepositsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    accountManager: AccountManager,
    startDestination: Route,
    pagerState: PagerState,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth
        composable<Route.Welcome> {
            WelcomeScreen(
                accountManager = accountManager,
                onAccountCreated = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.PinUnlock> {
            PinUnlockScreen(
                accountManager = accountManager,
                onUnlocked = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.PinUnlock) { inclusive = true }
                    }
                },
                onSwitchAccount = { navController.navigate(Route.AccountList) },
                onCreateAccount = { navController.navigate(Route.Welcome) }
            )
        }

        composable<Route.AccountList> {
            AccountListScreen(
                accountManager = accountManager,
                onBack = { navController.popBackStack() },
                onUnlocked = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.AccountList) { inclusive = true }
                    }
                }
            )
        }

        // Main pager — all primary screens in one HorizontalPager
        composable<Route.Main> {
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(pagerState.currentPage) {
                onPageChanged(pagerState.currentPage)
            }

            MainPagerScreen(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                onHoldingClick = { id -> navController.navigate(Route.HoldingDetail(id)) },
                goldClick = { id -> navController.navigate(Route.GoldDetail(id)) },
                onBankClick = { bank, status -> navController.navigate(Route.BankDeposits(bank, status)) },
                onAddDeposit = { navController.navigate(Route.AddDeposit) },
                onAddHolding = { type -> navController.navigate(Route.AddHolding(type)) },
                onMaturedDepositsClick = { navController.navigate(Route.MaturedDeposits) }
            )
        }

        // Backward compat
        composable<Route.Home> {
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(pagerState.currentPage) {
                onPageChanged(pagerState.currentPage)
            }
            MainPagerScreen(
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                onHoldingClick = { id -> navController.navigate(Route.HoldingDetail(id)) },
                goldClick = { id -> navController.navigate(Route.GoldDetail(id)) },
                onBankClick = { bank, status -> navController.navigate(Route.BankDeposits(bank, status)) },
                onAddDeposit = { navController.navigate(Route.AddDeposit) },
                onAddHolding = { type -> navController.navigate(Route.AddHolding(type)) },
                onMaturedDepositsClick = { navController.navigate(Route.MaturedDeposits) }
            )
        }

        // Detail screens (overlay)
        composable<Route.HoldingDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.HoldingDetail>()
            HoldingDetailScreen(
                holdingId = route.holdingId,
                onEdit = { navController.navigate(Route.EditHolding(route.holdingId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.GoldDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.GoldDetail>()
            GoldDetailScreen(
                holdingId = route.holdingId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.AddDeposit> {
            AddDepositScreen(onSaved = { navController.popBackStack() })
        }

        composable<Route.EditDeposit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.EditDeposit>()
            EditDepositScreen(
                depositId = route.depositId,
                onSaved = { navController.popBackStack() }
            )
        }

        composable<Route.BankDeposits> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.BankDeposits>()
            BankDepositsScreen(
                bankName = route.bankName,
                status = route.status,
                onBack = { navController.popBackStack() },
                onEditDeposit = { id -> navController.navigate(Route.EditDeposit(id)) },
                onAddDeposit = { navController.navigate(Route.AddDeposit) }
            )
        }

        composable<Route.MaturedDeposits> {
            MaturedDepositsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Route.AddHolding> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.AddHolding>()
            AddHoldingScreen(
                type = route.type,
                onSaved = { navController.popBackStack() }
            )
        }

        composable<Route.EditHolding> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.EditHolding>()
            EditHoldingScreen(
                holdingId = route.holdingId,
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
