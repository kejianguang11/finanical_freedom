package com.financial.freedom.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.ui.auth.AccountListScreen
import com.financial.freedom.ui.auth.PinUnlockScreen
import com.financial.freedom.ui.auth.WelcomeScreen
import com.financial.freedom.ui.earnings.EarningsScreen
import com.financial.freedom.ui.holdings.AddHoldingScreen
import com.financial.freedom.ui.holdings.EditHoldingScreen
import com.financial.freedom.ui.holdings.HoldingDetailScreen
import com.financial.freedom.ui.holdings.HoldingsScreen
import com.financial.freedom.ui.holdings.deposit.AddDepositScreen
import com.financial.freedom.ui.holdings.deposit.EditDepositScreen
import com.financial.freedom.ui.home.HomeScreen
import com.financial.freedom.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    accountManager: AccountManager,
    startDestination: Route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Route.Welcome> {
            WelcomeScreen(
                accountManager = accountManager,
                onAccountCreated = {
                    navController.navigate(Route.Home) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            )
        }

        composable<Route.PinUnlock> {
            PinUnlockScreen(
                accountManager = accountManager,
                onUnlocked = {
                    navController.navigate(Route.Home) {
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
                    navController.navigate(Route.Home) {
                        popUpTo(Route.AccountList) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Home> {
            HomeScreen(onNavigateToHoldings = { tab -> navController.navigate(Route.Holdings(tab)) })
        }

        composable<Route.Holdings> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Holdings>()
            HoldingsScreen(
                initialTab = route.tab,
                onHoldingClick = { id -> navController.navigate(Route.HoldingDetail(id)) },
                onAddDeposit = { navController.navigate(Route.AddDeposit) },
                onAddHolding = { type -> navController.navigate(Route.AddHolding(type)) }
            )
        }

        composable<Route.Earnings> {
            EarningsScreen()
        }

        composable<Route.Settings> {
            SettingsScreen()
        }

        composable<Route.HoldingDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.HoldingDetail>()
            HoldingDetailScreen(
                holdingId = route.holdingId,
                onEdit = { navController.navigate(Route.EditHolding(route.holdingId)) },
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
