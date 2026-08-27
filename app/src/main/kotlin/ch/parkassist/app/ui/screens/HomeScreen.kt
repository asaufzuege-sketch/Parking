package ch.parkassist.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.parkassist.app.R
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.ManualOutcome
import ch.parkassist.app.domain.state.ParkingState
import ch.parkassist.app.provider.MockParkingAdapter
import ch.parkassist.app.ui.ParkingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ParkingViewModel, onNavigateToLog: () -> Unit) {
    val uiState by vm.uiState.collectAsState()
    val isManualProvider = uiState.provider != Provider.MOCK

    val providerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val status = result.data?.getStringExtra(MockParkingAdapter.RESULT_STATUS)
        vm.handleProviderResult(result.resultCode, status)
    }

    LaunchedEffect(uiState.pendingLaunchIntent, uiState.showExperimentalWarning) {
        if (!uiState.showExperimentalWarning) {
            uiState.pendingLaunchIntent?.let { intent ->
                providerLauncher.launch(intent)
                vm.clearPendingIntent()
            }
        }
    }

    if (uiState.showExperimentalWarning) {
        AlertDialog(
            onDismissRequest = { vm.dismissExperimentalWarning(false) },
            title = { Text(stringResource(R.string.experimental_warning_title)) },
            text = { Text(stringResource(R.string.experimental_warning_message)) },
            confirmButton = {
                TextButton(onClick = { vm.dismissExperimentalWarning(true) }) {
                    Text(stringResource(R.string.btn_confirm_launch))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissExperimentalWarning(false) }) {
                    Text(stringResource(R.string.btn_cancel_launch))
                }
            }
        )
    }

    if (uiState.pendingManualOutcome) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.manual_outcome_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.dryRunMode) {
                        Text(stringResource(R.string.dry_run_active_hint))
                    }
                    if (isManualProvider) {
                        Text(manualChecklistText(uiState.provider))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.reportManualOutcome(ManualOutcome.CONFIRMED) }) {
                    Text(stringResource(R.string.manual_outcome_confirmed))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.reportManualOutcome(ManualOutcome.UNCLEAR) }) {
                        Text(stringResource(R.string.manual_outcome_unclear))
                    }
                    TextButton(onClick = { vm.reportManualOutcome(ManualOutcome.NOT_COMPLETED) }) {
                        Text(stringResource(R.string.manual_outcome_not_completed))
                    }
                }
            }
        )
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stateLabel, style = MaterialTheme.typography.titleMedium)
                    if (uiState.dryRunMode) {
                        Text(
                            text = stringResource(R.string.dry_run_active_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            val isIdle = uiState.parkingState is ParkingState.Idle ||
                uiState.parkingState is ParkingState.Error ||
                uiState.parkingState is ParkingState.Completed ||
                uiState.parkingState is ParkingState.Cancelled

            if (isIdle) {
                Text(stringResource(R.string.label_provider), style = MaterialTheme.typography.labelMedium)
                ProviderDropdown(
                    selected = uiState.provider,
                    onSelect = vm::setProvider,
                )

                if (isManualProvider) {
                    ManualProviderChecklist(uiState.provider)
                }

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.dryRunMode,
                        onCheckedChange = vm::setDryRunMode,
                    )
                    Column {
                        Text(stringResource(R.string.label_dry_run))
                        Text(
                            text = stringResource(R.string.dry_run_active_hint),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

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
                if (uiState.parkingState is ParkingState.Error) {
                    OutlinedButton(
                        onClick = { vm.resetError() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.btn_reset))
                    }
                }
            } else {
                val isExtensionDue = uiState.parkingState is ParkingState.ExtensionDue
                val canRequestExtension = uiState.parkingState is ParkingState.Active
                if (isManualProvider) {
                    ManualProviderChecklist(uiState.provider)
                }
                if (isExtensionDue) {
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
            }
        }
    }
}

@Composable
private fun ManualProviderChecklist(provider: Provider) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(provider.displayName, style = MaterialTheme.typography.titleSmall)
            Text(manualChecklistText(provider), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun manualChecklistText(provider: Provider): String = when (provider) {
    Provider.PARKINGPAY ->
        "1. Parkingpay öffnen\n2. Zone und Dauer prüfen\n3. Aktion manuell abschliessen\n4. Danach Ergebnis hier bestätigen"
    Provider.TWINT ->
        "1. TWINT öffnen\n2. Parking-Flow manuell durchführen\n3. Angaben prüfen\n4. Danach Ergebnis hier bestätigen"
    Provider.MOCK ->
        "Mock Parking unterstützt den lokalen Test-Flow ohne manuelle Drittanbieter-Schritte."
}

@OptIn(ExperimentalMaterial3Api::class)
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
