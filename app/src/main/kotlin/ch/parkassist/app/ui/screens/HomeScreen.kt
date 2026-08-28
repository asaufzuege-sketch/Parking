package ch.parkassist.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.parkassist.app.R
import ch.parkassist.app.domain.model.ParkingSession
import ch.parkassist.app.domain.model.Provider
import ch.parkassist.app.domain.state.ParkingState
import ch.parkassist.app.provider.MockProviderAdapter
import ch.parkassist.app.ui.ParkingViewModel
import ch.parkassist.app.ui.theme.ParkingTheme
import ch.parkassist.app.ui.theme.ParkingThemeTokens
import ch.parkassist.app.ui.theme.parkingCheckboxColors
import ch.parkassist.app.ui.theme.parkingDestructiveButtonColors
import ch.parkassist.app.ui.theme.parkingOutlinedFieldColors
import ch.parkassist.app.ui.theme.parkingPrimaryButtonColors
import ch.parkassist.app.ui.theme.parkingTextButtonColors
import ch.parkassist.app.ui.theme.parkingTopAppBarColors
import ch.parkassist.app.ui.theme.statusContainer
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ParkingViewModel, onNavigateToLog: () -> Unit) {
    val uiState by vm.uiState.collectAsState()

    val providerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val status = result.data?.getStringExtra(MockProviderAdapter.RESULT_STATUS)
        vm.handleProviderResult(result.resultCode, status)
    }

    LaunchedEffect(uiState.pendingLaunchIntent) {
        uiState.pendingLaunchIntent?.let { intent ->
            providerLauncher.launch(intent)
            vm.clearPendingIntent()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = parkingTopAppBarColors(),
                actions = {
                    TextButton(
                        onClick = onNavigateToLog,
                        colors = parkingTextButtonColors(),
                    ) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val stateLabel = when (uiState.parkingState) {
                is ParkingState.Idle -> stringResource(R.string.status_idle)
                is ParkingState.Scheduled -> stringResource(R.string.status_scheduled)
                is ParkingState.LaunchingProvider -> stringResource(R.string.status_launching)
                is ParkingState.AwaitingUser -> stringResource(R.string.status_awaiting)
                is ParkingState.Active -> stringResource(R.string.status_active)
                is ParkingState.ExtensionDue -> stringResource(R.string.status_extension_due)
                is ParkingState.Completed -> stringResource(R.string.status_completed)
                is ParkingState.Cancelled -> stringResource(R.string.status_cancelled)
                is ParkingState.Error -> stringResource(R.string.status_error)
            }

            ParkingStatusCard(
                state = uiState.parkingState,
                stateLabel = stateLabel,
            )

            val isIdle = uiState.parkingState is ParkingState.Idle ||
                uiState.parkingState is ParkingState.Error ||
                uiState.parkingState is ParkingState.Completed ||
                uiState.parkingState is ParkingState.Cancelled

            if (isIdle) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SectionTitle(text = stringResource(R.string.section_parking_details))
                        ProviderDropdown(
                            selected = uiState.provider,
                            onSelect = vm::setProvider,
                        )
                        if (uiState.provider == Provider.MOCK) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small,
                                    )
                            ) {
                                Text(
                                    text = stringResource(R.string.provider_mock_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.zone,
                            onValueChange = vm::setZone,
                            label = { Text(stringResource(R.string.label_zone)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = parkingOutlinedFieldColors(),
                        )
                        OutlinedTextField(
                            value = uiState.licensePlate,
                            onValueChange = vm::setLicensePlate,
                            label = { Text(stringResource(R.string.label_plate)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = parkingOutlinedFieldColors(),
                        )
                        OutlinedTextField(
                            value = uiState.ticketDurationMinutes,
                            onValueChange = vm::setTicketDuration,
                            label = { Text(stringResource(R.string.label_duration_minutes)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = parkingOutlinedFieldColors(),
                        )
                        OutlinedTextField(
                            value = uiState.maxExtensions,
                            onValueChange = vm::setMaxExtensions,
                            label = { Text(stringResource(R.string.label_max_extensions)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = parkingOutlinedFieldColors(),
                        )

                        CheckboxRow(
                            checked = uiState.startNow,
                            onCheckedChange = vm::setStartNow,
                            text = stringResource(R.string.label_start_now),
                        )
                        CheckboxRow(
                            checked = uiState.userConfirmed,
                            onCheckedChange = vm::setUserConfirmed,
                            text = stringResource(R.string.confirm_message),
                            supportingText = stringResource(R.string.confirm_title),
                        )

                        uiState.validationError?.let { errorMessage ->
                            ValidationErrorCard(errorMessage = errorMessage)
                        }

                        Button(
                            onClick = { vm.startParking() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = parkingPrimaryButtonColors(),
                        ) {
                            Text(stringResource(R.string.btn_start))
                        }
                        if (uiState.parkingState is ParkingState.Error) {
                            OutlinedButton(
                                onClick = { vm.resetError() },
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_reset),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SectionTitle(text = stringResource(R.string.section_active_session))
                        val isExtensionDue = uiState.parkingState is ParkingState.ExtensionDue
                        val canRequestExtension = uiState.parkingState is ParkingState.Active
                        if (isExtensionDue) {
                            Button(
                                onClick = { vm.confirmExtension() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = parkingPrimaryButtonColors(),
                            ) {
                                Text(stringResource(R.string.btn_confirm_action))
                            }
                        } else if (canRequestExtension) {
                            OutlinedButton(
                                onClick = { vm.requestExtension() },
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_extend),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Button(
                            onClick = { vm.stopParking() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = parkingDestructiveButtonColors(),
                        ) {
                            Text(stringResource(R.string.btn_stop))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "v${ch.parkassist.app.BuildConfig.VERSION_NAME} (${ch.parkassist.app.BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun ParkingStatusCard(state: ParkingState, stateLabel: String) {
    val accent = when (state.statusTone()) {
        ParkingStatusTone.ACTIVE -> ParkingThemeTokens.statusColors.active
        ParkingStatusTone.WAITING -> ParkingThemeTokens.statusColors.waiting
        ParkingStatusTone.ERROR -> ParkingThemeTokens.statusColors.error
        ParkingStatusTone.NEUTRAL -> ParkingThemeTokens.statusColors.neutral
    }
    val badgeLabel = when (state.statusTone()) {
        ParkingStatusTone.ACTIVE -> stringResource(R.string.status_badge_active)
        ParkingStatusTone.WAITING -> stringResource(R.string.status_badge_waiting)
        ParkingStatusTone.ERROR -> stringResource(R.string.status_badge_error)
        ParkingStatusTone.NEUTRAL -> stringResource(R.string.status_badge_neutral)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.statusTone()) {
                ParkingStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Text(
                    text = stringResource(R.string.section_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusBadge(
                    label = badgeLabel,
                    accent = accent,
                )
            }
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state is ParkingState.Error) {
                    ErrorDot(
                        color = accent,
                        contentDescription = stringResource(R.string.content_desc_error_status),
                    )
                }
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (state is ParkingState.Error && state.message.isNotBlank()) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, accent: androidx.compose.ui.graphics.Color) {
    Surface(
        color = statusContainer(accent),
        contentColor = accent,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ErrorDot(color: Color, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        Text(
            text = "!",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    supportingText: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = parkingCheckboxColors(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ValidationErrorCard(errorMessage: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun ProviderDropdown(selected: Provider, onSelect: (Provider) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline,
            ),
        ) {
            Text(
                text = stringResource(R.string.label_provider) + ": " + selected.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            Provider.entries.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = provider.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onSelect(provider)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080808)
@Composable
private fun ParkingStatusCardPreview() {
    val sampleSession = ParkingSession(
        provider = Provider.MOCK,
        zone = "8001",
        licensePlate = "ZH123456",
        ticketDurationMinutes = 60,
        maxExtensions = 1,
        startTime = Instant.now(),
        confirmedByUser = true,
    )
    ParkingTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ParkingStatusCard(
                state = ParkingState.Active(
                    session = sampleSession,
                    expiresAt = Instant.now().plusSeconds(3600),
                ),
                stateLabel = "Aktiv",
            )
            ParkingStatusCard(
                state = ParkingState.Error(sampleSession, "Anbieter meldet Fehler"),
                stateLabel = "Fehler",
            )
        }
    }
}
