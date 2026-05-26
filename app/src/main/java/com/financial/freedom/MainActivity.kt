package com.financial.freedom

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.financial.freedom.data.DefaultDataSeeder
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.account.LockManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accountManager: AccountManager
    @Inject lateinit var defaultDataSeeder: DefaultDataSeeder

    private val shieldDrawable by lazy {
        ColorDrawable(0xFFF5F0E8.toInt())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        val am = accountManager
        val seeder = defaultDataSeeder
        setContent {
            FinancialFreedomApp(accountManager = am, defaultDataSeeder = seeder)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val decor = window.decorView
        if (!hasFocus) {
            // Called BEFORE onPause — set shield before system captures the frame
            LockManager.lock()
            shieldDrawable.setBounds(0, 0, decor.width, decor.height)
            decor.overlay.add(shieldDrawable)
        } else {
            // Called when window regains focus — remove shield after first frame
            decor.post {
                decor.overlay.remove(shieldDrawable)
            }
        }
    }
}
