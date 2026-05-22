package com.financial.freedom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.financial.freedom.data.local.entity.Account
import com.financial.freedom.domain.account.AccountManager
import kotlinx.coroutines.launch

@Composable
fun PinUnlockScreen(
    accountManager: AccountManager,
    onUnlocked: () -> Unit,
    onSwitchAccount: () -> Unit,
    onCreateAccount: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var account: Account? by remember { mutableStateOf(null) }
    var pin by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val restored = accountManager.restoreLastAccount()
        if (!restored) {
            onCreateAccount()
        } else {
            account = accountManager.currentAccount.value
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "欢迎回来",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            account?.nickname ?: "",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                    pin = it
                    errorMessage = null
                }
            },
            label = { Text("请输入 4 位 PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage != null,
            supportingText = errorMessage?.let { msg ->
                { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val currentAccount = account ?: return@Button
                if (pin.length != 4) {
                    errorMessage = "请输入 4 位 PIN"
                    return@Button
                }
                if (accountManager.verifyPin(currentAccount, pin)) {
                    scope.launch {
                        accountManager.switchTo(currentAccount)
                        onUnlocked()
                    }
                } else {
                    attempts++
                    pin = ""
                    if (attempts >= 5) {
                        Toast.makeText(context, "PIN 错误次数过多，请切换账号", Toast.LENGTH_LONG).show()
                        onSwitchAccount()
                    } else {
                        errorMessage = "PIN 错误，还剩 ${5 - attempts} 次机会"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = pin.length == 4
        ) {
            Text("解锁")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onSwitchAccount) {
            Text("切换账号")
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onCreateAccount) {
            Text("创建新账号")
        }
    }
}
