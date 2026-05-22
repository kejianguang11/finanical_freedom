package com.financial.freedom.ui.holdings.deposit

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditDepositViewModel @Inject constructor(
    private val depositDao: DepositDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    var deposit by mutableStateOf<Deposit?>(null)

    fun load(id: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            deposit = depositDao.getById(id, accountId)
        }
    }

    fun update(deposit: Deposit, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val oldDeposit = this@EditDepositViewModel.deposit
                depositDao.update(deposit)
                // 从旧/新日期中较早的那个开始重新回填
                val fromDate = if (oldDeposit != null && oldDeposit.startDate < deposit.startDate) {
                    oldDeposit.startDate
                } else {
                    deposit.startDate
                }
                backfillEngine.markDirtyAndBackfill(fromDate, deposit.accountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDepositScreen(
    depositId: Long,
    onSaved: () -> Unit = {},
    viewModel: EditDepositViewModel = hiltViewModel()
) {
    val d = viewModel.deposit

    var name by rememberSaveable { mutableStateOf("") }
    var bank by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("CNY") }
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var maturityDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var currencyExpanded by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(depositId) { viewModel.load(depositId) }

    // 首次加载时预填表单
    LaunchedEffect(d) {
        if (d != null && !loaded) {
            name = d!!.name
            bank = d!!.bank
            currency = d!!.currency
            principal = d!!.principal.toPlainString()
            rate = d!!.interestRate.multiply(BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            startDate = d!!.startDate.toString()
            maturityDate = d!!.maturityDate.toString()
            note = d!!.note
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑存款") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (d == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("加载中...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(name, { name = it }, label = { Text("存款名称") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(bank, { bank = it }, label = { Text("银行/平台") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = currency, onValueChange = {}, readOnly = true,
                    label = { Text("币种") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    currencies.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; currencyExpanded = false })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(principal, { principal = it }, label = { Text("本金") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(rate, { rate = it }, label = { Text("年化利率 (如 2.75)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(startDate, { startDate = it }, label = { Text("存入日期 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(maturityDate, { maturityDate = it }, label = { Text("到期日期 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("备注 (选填)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val dep = d ?: return@Button
                    viewModel.update(
                        dep.copy(
                            name = name, bank = bank, currency = currency,
                            principal = BigDecimal(principal),
                            interestRate = BigDecimal(rate).divide(BigDecimal(100), 6, java.math.RoundingMode.HALF_UP),
                            startDate = LocalDate.parse(startDate),
                            maturityDate = LocalDate.parse(maturityDate),
                            note = note
                        ),
                        onSuccess = {
                            Toast.makeText(context, "存款已更新", Toast.LENGTH_SHORT).show()
                            onSaved()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "更新失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存修改") }
        }
    }
}
