package ch.parkassist.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.parkassist.app.R
import ch.parkassist.app.domain.model.ActivityLogEntry
import ch.parkassist.app.ui.ParkingViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(vm: ParkingViewModel, onBack: () -> Unit) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_log)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.clearLog() }) { Text("Löschen") }
                }
            )
        }
    ) { padding ->
        if (uiState.log.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.log_empty),
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.log) { entry ->
                    LogEntryCard(entry)
                }
            }
        }
    }
}

private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
private fun LogEntryCard(entry: ActivityLogEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = formatter.format(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(entry.event, style = MaterialTheme.typography.bodyMedium)
            if (entry.detail.isNotBlank()) {
                Text(entry.detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
