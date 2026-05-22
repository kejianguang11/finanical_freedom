package com.financial.freedom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.financial.freedom.data.local.entity.Account
import com.financial.freedom.domain.account.AccountManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    accountManager: AccountManager,
    onBack: () -> Unit,
    onUnlocked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var showPinDialog by remember { mutableStateOf<Account?>(null) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        accounts = accountManager.getAllAccounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择账号") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showPinDialog = account
                            pin = ""
                            pinError = false
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            account.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无账号",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    showPinDialog?.let { account ->
        AlertDialog(
            onDismissRequest = { showPinDialog = null },
            title = { Text("输入 PIN - ${account.nickname}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pin = it
                                pinError = false
                            }
                        },
                        label = { Text("4 位 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("PIN 错误", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountManager.verifyPin(account, pin)) {
                            scope.launch {
                                accountManager.switchTo(account)
                                showPinDialog = null
                                onUnlocked()
                            }
                        } else {
                            pinError = true
                            pin = ""
                        }
                    },
                    enabled = pin.length == 4
                ) {
                    Text("解锁")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}
