package com.financial.freedom.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.financial.freedom.data.remote.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchSheet(
    title: String = "搜索",
    placeholder: String = "输入代码或名称",
    onSearch: suspend (String) -> List<SearchResult>,
    onSelect: (SearchResult) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf<List<SearchResult>>(emptyList()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var searchJob by rememberSaveable { mutableStateOf<Job?>(null) }

    LaunchedEffect(query) {
        if (query.length < 2) {
            results = emptyList()
            searching = false
            error = null
            return@LaunchedEffect
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            searching = true
            error = null
            delay(400) // debounce
            try {
                results = onSearch(query)
            } catch (e: Exception) {
                error = "搜索失败: ${e.message}"
                results = emptyList()
            } finally {
                searching = false
            }
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        if (searching) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp))
        }

        if (results.isEmpty() && !searching && error == null) {
            Text(
                if (query.length >= 2) "未找到结果" else "输入关键词开始搜索",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }

        LazyColumn {
            items(results) { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(result) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(result.symbol, fontWeight = FontWeight.Bold)
                        Text(result.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(result.type, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider()
            }
        }
    }
}
