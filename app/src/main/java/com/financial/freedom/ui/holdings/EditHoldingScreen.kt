package com.financial.freedom.ui.holdings

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class EditHoldingViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    var holding by mutableStateOf<Holding?>(null)

    fun load(id: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            holding = holdingDao.getById(id, accountId)
        }
    }

    fun update(holding: Holding, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val oldHolding = this@EditHoldingViewModel.holding
                holdingDao.update(holding)
                // 从旧/新日期中较早的那个开始重新回填
                val fromDate = if (oldHolding != null && oldHolding.costDate < holding.costDate) {
                    oldHolding.costDate
                } else {
                    holding.costDate
                }
                backfillEngine.markDirtyAndBackfill(fromDate, holding.accountId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHoldingScreen(
    holdingId: Long,
    onSaved: () -> Unit = {},
    viewModel: EditHoldingViewModel = hiltViewModel()
) {
    val h = viewModel.holding

    var symbol by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf("CN") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var costPrice by rememberSaveable { mutableStateOf("") }
    var costDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val type = h?.type ?: "STOCK"
    val typeLabel = when (type) { "STOCK" -> "股票"; "FUND" -> "基金"; "GOLD" -> "黄金"; else -> "资产" }

    LaunchedEffect(holdingId) { viewModel.load(holdingId) }

    LaunchedEffect(h) {
        if (h != null && !loaded) {
            symbol = h!!.symbol
            name = h!!.name
            market = h!!.market
            quantity = h!!.quantity.toPlainString()
            costPrice = h!!.costPrice.toPlainString()
            costDate = h!!.costDate.toString()
            note = h!!.note
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑$typeLabel") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (h == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("加载中...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(symbol, { symbol = it }, label = { Text("代码") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            if (type == "STOCK") {
                OutlinedTextField(market, { market = it }, label = { Text("市场 (CN/US/HK)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            val qtyLabel = when (type) { "GOLD" -> "重量 (克)"; "FUND" -> "持有份额"; else -> "持有数量 (股)" }
            OutlinedTextField(quantity, { quantity = it }, label = { Text(qtyLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            val priceLabel = when (type) { "GOLD" -> "买入总价 (¥)"; "FUND" -> "成本净值"; else -> "成本价" }
            OutlinedTextField(costPrice, { costPrice = it }, label = { Text(priceLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(costDate, { costDate = it }, label = { Text("买入日期 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("备注 (选填)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val holding = h ?: return@Button
                    viewModel.update(
                        holding.copy(
                            symbol = symbol, name = name, market = market,
                            quantity = BigDecimal(quantity),
                            costPrice = BigDecimal(costPrice),
                            costDate = LocalDate.parse(costDate),
                            note = note
                        ),
                        onSuccess = {
                            Toast.makeText(context, "$typeLabel 已更新", Toast.LENGTH_SHORT).show()
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
