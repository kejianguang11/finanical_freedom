package com.financial.freedom.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var exportType by rememberSaveable { mutableStateOf("") }
    var importUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    // 保存文件 launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            when (exportType) {
                "deposits" -> viewModel.exportDeposits(it)
                "holdings" -> viewModel.exportHoldings(it)
            }
            Toast.makeText(context, "导出完成", Toast.LENGTH_SHORT).show()
        }
    }

    // 打开文件 launcher
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importUri = it
            viewModel.previewImport(it)
        }
    }

    // 清空确认对话框
    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearConfirm() },
            title = { Text("清空所有数据") },
            text = { Text("确定要删除全部本地数据吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData() }) {
                    Text("清空", color = FinancialColors.up)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearConfirm() }) { Text("取消") }
            }
        )
    }

    // 导入预览
    state.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = { Text("导入预览 — ${preview.fileName}") },
            text = {
                Column {
                    Text("共 ${preview.totalRows} 条数据")
                    Spacer(Modifier.height(8.dp))
                    preview.sampleRows.take(3).forEach { row ->
                        Text(row, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = importUri
                    if (uri != null) {
                        val fileName = preview.fileName.lowercase()
                        when {
                            fileName.contains("deposit") -> viewModel.confirmImportDeposits(uri)
                            fileName.contains("holding") -> viewModel.confirmImportHoldings(uri)
                            else -> {
                                val firstRow = preview.sampleRows.firstOrNull()?.lowercase() ?: ""
                                if (firstRow.contains("principal") || firstRow.contains("interest_rate"))
                                    viewModel.confirmImportDeposits(uri)
                                else
                                    viewModel.confirmImportHoldings(uri)
                            }
                        }
                        Toast.makeText(context, "导入完成", Toast.LENGTH_SHORT).show()
                        importUri = null
                    }
                }) { Text("确认导入") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPreview() }) { Text("取消") }
            }
        )
    }

    // 账号列表对话框
    if (state.showAccountDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccountDialog() },
            title = { Text("切换账号") },
            text = {
                LazyColumn {
                    items(state.accounts) { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.switchAccount(account) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Text(account.nickname, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAccountDialog() }) { Text("取消") }
            }
        )
    }

    // 修改 PIN 对话框
    if (state.showPinChangeDialog) {
        var oldPin by rememberSaveable { mutableStateOf("") }
        var newPin by rememberSaveable { mutableStateOf("") }
        var confirmPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissPinChangeDialog() },
            title = { Text("修改 PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
                        label = { Text("当前 4 位 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("新 4 位 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("确认新 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPin.length != 4) {
                            Toast.makeText(context, "请输入当前 PIN", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPin.length != 4 || newPin != confirmPin) {
                            Toast.makeText(context, "新 PIN 不匹配或不足 4 位", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.changePin(oldPin, newPin) { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = oldPin.length == 4 && newPin.length == 4 && confirmPin.length == 4
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPinChangeDialog() }) { Text("取消") }
            }
        )
    }

    // 删除账号确认
    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text("删除当前账号") },
            text = { Text("确定要删除「${state.currentAccountNickname}」吗？该账号所有数据将被删除，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCurrentAccount() }) {
                    Text("删除", color = FinancialColors.up)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) { Text("取消") }
            }
        )
    }

    // 创建新账号对话框
    if (state.showCreateAccountDialog) {
        var newNickname by rememberSaveable { mutableStateOf("") }
        var newPin by rememberSaveable { mutableStateOf("") }
        var confirmPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreateAccountDialog() },
            title = { Text("创建新账号") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNickname,
                        onValueChange = { newNickname = it },
                        label = { Text("昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("设置 4 位 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("确认 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNickname.isBlank()) {
                            Toast.makeText(context, "请输入昵称", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPin.length != 4 || newPin != confirmPin) {
                            Toast.makeText(context, "PIN 不匹配或不足 4 位", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.createAccount(newNickname.trim(), newPin)
                        Toast.makeText(context, "账号已创建", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreateAccountDialog() }) { Text("取消") }
            }
        )
    }

    // 测试数据生成完成提示
    if (state.testDataDone) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "测试数据生成完成", Toast.LENGTH_SHORT).show()
            viewModel.dismissTestDataDone()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        // ---- 账号管理 ----
        Text("账号", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("当前账号：${state.currentAccountNickname}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                Row {
                    TextButton(onClick = { viewModel.showAccountDialog() }) {
                        Text("切换账号")
                    }
                    TextButton(onClick = { viewModel.showCreateAccountDialog() }) {
                        Text("新建账号")
                    }
                }

                Row {
                    TextButton(onClick = { viewModel.showPinChangeDialog() }) {
                        Text("修改 PIN")
                    }
                    TextButton(onClick = { viewModel.showDeleteConfirm() }) {
                        Text("删除账号", color = FinancialColors.up)
                    }
                }
            }
        }

        // ---- 测试数据 ----
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                Modifier.padding(16.dp).clickable { viewModel.generateTestData() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("一键生成测试数据", style = MaterialTheme.typography.bodyLarge)
                    Text("为当前账号填充 90 天模拟数据", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 数据导出
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("数据导出", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))

                Text("存款", style = MaterialTheme.typography.labelMedium)
                Row {
                    TextButton(onClick = {
                        exportType = "deposits"
                        saveLauncher.launch("deposits.csv")
                    }) { Text("保存文件") }
                    TextButton(onClick = { viewModel.shareDeposits() }) { Text("分享") }
                }

                Text("持仓", style = MaterialTheme.typography.labelMedium)
                Row {
                    TextButton(onClick = {
                        exportType = "holdings"
                        saveLauncher.launch("holdings.csv")
                    }) { Text("保存文件") }
                    TextButton(onClick = { viewModel.shareHoldings() }) { Text("分享") }
                }
            }
        }

        // 分享 intent 处理
        state.shareIntent?.let { intent ->
            LaunchedEffect(intent) {
                context.startActivity(Intent.createChooser(intent, "分享 CSV"))
                viewModel.clearShareIntent()
            }
        }

        // 数据导入
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { openLauncher.launch(arrayOf("text/*", "*/*")) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("数据导入", style = MaterialTheme.typography.bodyLarge)
                    Text("从 CSV 文件导入", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 汇率
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.refreshRates() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("汇率基准", style = MaterialTheme.typography.bodyLarge)
                    Text(state.rates, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 清空数据
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.showClearConfirm() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("清空全部数据", style = MaterialTheme.typography.bodyLarge, color = FinancialColors.up)
            }
        }

        // 关于
        Card(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.bodyLarge)
                Text("财富自由 v1.0.0", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("数据完全本地存储，安全私密", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
