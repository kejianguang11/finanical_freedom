package com.financial.freedom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.financial.freedom.data.DefaultDataSeeder
import com.financial.freedom.domain.account.AccountManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accountManager: AccountManager
    @Inject lateinit var defaultDataSeeder: DefaultDataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val am = accountManager
        val seeder = defaultDataSeeder
        setContent {
            FinancialFreedomApp(accountManager = am, defaultDataSeeder = seeder)
        }
    }
}
