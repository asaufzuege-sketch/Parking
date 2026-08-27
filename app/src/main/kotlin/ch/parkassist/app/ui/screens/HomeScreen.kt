package ch.parkassist.app.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.parkassist.app.R
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.*
import ch.parkassist.app.provider.MockProviderAdapter
import ch.parkassist.app.ui.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ParkingViewModel, onNavigateToLog: () -> Unit) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Launch provider activity and handle result
    val providerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val status = result.data?.getStringExtra(MockProviderAdapter.RESULT_STATUS)
                if (status == MockProviderAdapter.STATUS_CONFIRMED) {
                    // ViewModel handles state via StateFlow
                }
            }
            Activity.RESULT_CANCELED -> vm.stopParking()
            else -> vm.stopParking()
        }
    }

    // Trigger intent launch when ViewModel sets it
    LaunchedEffect(uiState.pendingLaunchIntent) {
        uiState.pendingLaunchIntent?.let { intent ->
            providerLauncher.launch(intent)
            vm.clearPendingIntent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = onNavigateToLog) {
                        Text(stringResource(R.string.screen_log))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status card
            val stateLabel = when (val s = uiState.parkingState) {
                is ParkingState.Idle -> stringResource(R.string.status_idle)
                is ParkingState.Scheduled -> stringResource(R.string.status_scheduled)
                is ParkingState.LaunchingProvider -> stringResource(R.string.status_launching)
                is ParkingState.AwaitingUser -> stringResource(R.string.status_awaiting)
                is ParkingState.Active -> stringResource(R.string.status_active)
                is ParkingState.ExtensionDue -> stringResource(R.string.status_extension_due)
                is ParkingState.Completed -> stringResource(R.string.status_completed)
                is ParkingState.Cancelled -> stringResource(R.string.status_cancelled)
                is ParkingState.Error -> "${stringResource(R.string.status_error)}: ${s.message}"
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stateLabel,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val isIdle = uiState.parkingState is ParkingState.Idle ||
                uiState.parkingState is ParkingState.Error ||
                uiState.parkingState is ParkingState.Completed ||
                uiState.parkingState is ParkingState.Cancelled

            if (isIdle) {
                // Input form
                Text(stringResource(R.string.label_provider), style = MaterialTheme.typography.labelMedium)
                ProviderDropdown(
                    selected = uiState.provider,
                    onSelect = vm::setProvider,
                )

                OutlinedTextField(
                    value = uiState.zone,
                    onValueChange = vm::setZone,
                    label = { Text(stringResource(R.string.label_zone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.licensePlate,
                    onValueChange = vm::setLicensePlate,
                    label = { Text(stringResource(R.string.label_plate)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.ticketDurationMinutes,
                    onValueChange = vm::setTicketDuration,
                    label = { Text(stringResource(R.string.label_duration_minutes)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.maxExtensions,
                    onValueChange = vm::setMaxExtensions,
                    label = { Text(stringResource(R.string.label_max_extensions)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.startNow,
                        onCheckedChange = vm::setStartNow
                    )
                    Text(stringResource(R.string.label_start_now))
                }

                // Confirmation checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.userConfirmed,
                        onCheckedChange = vm::setUserConfirmed
                    )
                    Text(
                        text = stringResource(R.string.confirm_message),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                uiState.validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { vm.startParking() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.btn_start))
                }
            } else {
                // Active session controls
                val isExtensionDue = uiState.parkingState is ParkingState.ExtensionDue
                val canRequestExtension = uiState.parkingState is ParkingState.Active
                if (isExtensionDue) {
                    // User must explicitly confirm the extension
                    Button(
                        onClick = { vm.confirmExtension() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.btn_confirm_action))
                    }
                } else if (canRequestExtension) {
                    OutlinedButton(
                        onClick = { vm.requestExtension() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.btn_extend))
                    }
                }
                Button(
                    onClick = { vm.stopParking() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.btn_stop))
                }
                if (uiState.parkingState is ParkingState.Error) {
                    OutlinedButton(
                        onClick = { vm.resetError() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Zurücksetzen")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderDropdown(selected: Provider, onSelect: (Provider) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Provider.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.displayName) },
                    onClick = { onSelect(p); expanded = false }
                )
            }
        }
    }
}
