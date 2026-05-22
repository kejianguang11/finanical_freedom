package com.financial.freedom.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.financial.freedom.domain.account.AccountManager
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    accountManager: AccountManager,
    onAccountCreated: () -> Unit
) {
    var nickname by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var pinConfirm by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "欢迎使用财富自由",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "创建你的第一个账号，开启财富管理之旅",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("昵称") },
            placeholder = { Text("给自己起个名字") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("设置 4 位 PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = pinConfirm,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirm = it },
            label = { Text("确认 PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (nickname.isBlank()) {
                    Toast.makeText(context, "请输入昵称", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (pin.length != 4) {
                    Toast.makeText(context, "PIN 必须为 4 位数字", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (pin != pinConfirm) {
                    Toast.makeText(context, "两次 PIN 不一致", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                loading = true
                scope.launch {
                    accountManager.createAccount(nickname.trim(), pin)
                    onAccountCreated()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loading
        ) {
            Text(if (loading) "创建中..." else "创建账号")
        }
    }
}
