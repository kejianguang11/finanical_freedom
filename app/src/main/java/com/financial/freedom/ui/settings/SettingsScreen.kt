package com.financial.freedom.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var importUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }

    // 保存文件 launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.showExportPinDialog(it) }
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

    // 备份保存 launcher
    val backupSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.backup(it) }
    }

    // 恢复文件选择 launcher
    val restoreOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.previewRestore(it) }
    }

    // 备份完成 toast
    if (state.backupDone) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "备份完成", Toast.LENGTH_SHORT).show()
            viewModel.dismissBackupDone()
        }
    }

    // 恢复完成 toast
    if (state.restoreDone) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "恢复完成，数据已刷新", Toast.LENGTH_SHORT).show()
            viewModel.dismissRestoreDone()
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

    // 重算收益确认对话框
    if (state.showRecalcConfirm) {
        AlertDialog(
            onDismissRequest = { if (!state.isRecalculating) viewModel.dismissRecalcConfirm() },
            title = { Text(if (state.isRecalculating) "正在重算…" else "确认重算收益") },
            text = {
                if (state.isRecalculating) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = FinancialColors.up)
                        Text("正在从最早资产日期重新计算所有每日收益…", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("将删除所有每日收益汇总数据，并从最早资产日期重新计算。\n\n此操作不会影响存款、持仓等原始数据，但无法撤销。")
                }
            },
            confirmButton = {
                if (!state.isRecalculating) {
                    TextButton(onClick = { viewModel.recalculateReturns() }) {
                        Text("确认重算", color = FinancialColors.up)
                    }
                }
            },
            dismissButton = {
                if (!state.isRecalculating) {
                    TextButton(onClick = { viewModel.dismissRecalcConfirm() }) { Text("取消") }
                }
            }
        )
    }

    // Toast 提示
    if (state.recalcDone) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "收益重算完成", Toast.LENGTH_SHORT).show()
            viewModel.dismissRecalcDone()
        }
    }
    if (state.testDataDone) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "测试数据已生成", Toast.LENGTH_SHORT).show()
            viewModel.dismissTestDataDone()
        }
    }

    // 恢复确认对话框
    if (state.showRestoreConfirm && state.restorePreview != null) {
        val preview = state.restorePreview!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreConfirm() },
            title = { Text("确认恢复数据") },
            text = {
                Column {
                    Text("将用备份文件覆盖当前账号所有数据，此操作不可撤销。")
                    Spacer(Modifier.height(12.dp))
                    Text("备份日期：${preview.exportedAt.take(16)}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("存款 ${preview.deposits.size} 笔 · 持仓 ${preview.holdings.size} 只",
                        style = MaterialTheme.typography.bodySmall)
                    Text("价格快照 ${preview.priceSnapshots.size} 条 · 交易 ${preview.transactions.size} 笔",
                        style = MaterialTheme.typography.bodySmall)
                    Text("日汇总 ${preview.dailySummaries.size} 天 · 现金流水 ${preview.cashTransactions.size} 条",
                        style = MaterialTheme.typography.bodySmall)
                    Text("汇率 ${preview.exchangeRates.size} 条",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) {
                    Text("确认恢复", color = FinancialColors.up)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreConfirm() }) { Text("取消") }
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
                        if (preview.isEncrypted) {
                            viewModel.showImportPinDialog(uri)
                        } else {
                            viewModel.confirmImportAll(uri)
                            Toast.makeText(context, "导入完成", Toast.LENGTH_SHORT).show()
                        }
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

        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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

        // 重算收益
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.showRecalcConfirm() },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("一键重算收益", style = MaterialTheme.typography.bodyLarge)
                    Text("删除所有历史收益汇总并重新计算", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 一键备份与恢复 ──
        Text("数据备份", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedCard(
                modifier = Modifier.weight(1f).clickable {
                    backupSaveLauncher.launch("furaoge_backup_${state.currentAccountNickname}.json")
                }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("一键备份",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("导出完整数据快照",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedCard(
                modifier = Modifier.weight(1f).clickable {
                    restoreOpenLauncher.launch(arrayOf("application/json", "*/*"))
                }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("一键恢复",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("从备份文件恢复所有数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 数据导出
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("数据导出", style = MaterialTheme.typography.bodyLarge)
                Text("导出全部资产为 assets.csv", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = {
                        saveLauncher.launch("assets.csv")
                    }) { Text("保存文件") }
                    TextButton(onClick = { viewModel.shareAll() }) { Text("分享") }
                }
            }
        }

        // 导出 PIN 对话框
    if (state.showExportPinDialog) {
        var exportPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportPinDialog() },
            title = { Text("设置加密密码") },
            text = {
                Column {
                    Text("输入 4 位 PIN 对导出文件加密，留空则不加密。")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) exportPin = it },
                        label = { Text("4 位 PIN (留空=不加密)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = state.pendingExportUri
                    if (uri != null) {
                        viewModel.exportAll(uri, exportPin.ifBlank { null })
                        Toast.makeText(context, "导出完成", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.dismissExportPinDialog()
                    }
                }) { Text("导出") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissExportPinDialog() }) { Text("取消") }
            }
        )
    }

    // 导入 PIN 对话框
    if (state.showImportPinDialog) {
        var importPin by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPinDialog() },
            title = { Text("输入解密密码") },
            text = {
                Column {
                    Text("此文件已加密，请输入 PIN 解密。")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) importPin = it },
                        label = { Text("4 位 PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importPin.length == 4) {
                        val uri = state.pendingImportUri
                        if (uri != null) {
                            viewModel.confirmImportAll(uri, importPin)
                            Toast.makeText(context, "导入完成", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请输入 4 位 PIN", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPinDialog() }) { Text("取消") }
            }
        )
    }

    // 分享 intent 处理
        state.shareIntent?.let { intent ->
            LaunchedEffect(intent) {
                context.startActivity(Intent.createChooser(intent, "分享 CSV"))
                viewModel.clearShareIntent()
            }
        }

        // 数据导入
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { openLauncher.launch(arrayOf("text/*", "*/*")) },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.refreshRates() },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("汇率基准", style = MaterialTheme.typography.bodyLarge)
                    Text(state.rates, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 显示倍率
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("显示倍率", style = MaterialTheme.typography.bodyLarge)
                Text("所有页面的金额显示将乘以所选倍率", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (ratio in listOf(
                        java.math.BigDecimal("0.1") to "10%",
                        java.math.BigDecimal("0.5") to "50%",
                        java.math.BigDecimal.ONE to "100%"
                    )) {
                        val isSelected = state.displayMultiplier == ratio.first
                        OutlinedCard(
                            modifier = Modifier.clickable { viewModel.setDisplayMultiplier(ratio.first) },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = ratio.second,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 清空数据
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.showClearConfirm() },
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("清空全部数据", style = MaterialTheme.typography.bodyLarge, color = FinancialColors.up)
            }
        }

        // 关于
        ElevatedCard(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
