package ch.parkassist.mockparking.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.parkassist.mockparking.R
import ch.parkassist.mockparking.contract.MockParkingContract

private val MockColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF00A693),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF041412),
    background = androidx.compose.ui.graphics.Color(0xFF071110),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF3F8F7),
    surface = androidx.compose.ui.graphics.Color(0xFF10201E),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF3F8F7),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF18312E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFAAC8C3),
    outline = androidx.compose.ui.graphics.Color(0xFF2C5550),
    error = androidx.compose.ui.graphics.Color(0xFFB64C4C),
    onError = androidx.compose.ui.graphics.Color(0xFFFDF3F3),
)

class MockParkingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action ?: MockParkingContract.ACTION_START
        val zone = intent?.getStringExtra(MockParkingContract.EXTRA_ZONE) ?: ""
        val plate = intent?.getStringExtra(MockParkingContract.EXTRA_PLATE) ?: ""
        val duration = intent?.getIntExtra(MockParkingContract.EXTRA_DURATION_MINUTES, 0) ?: 0

        setContent {
            MockParkingTheme {
                MockParkingScreen(
                    action = action,
                    zone = zone,
                    plate = plate,
                    durationMinutes = duration,
                    onConfirm = { finishWithStatus(MockParkingContract.STATUS_CONFIRMED) },
                    onDeny = { finishWithStatus(MockParkingContract.STATUS_DENIED) },
                    onError = { finishWithStatus(MockParkingContract.STATUS_ERROR) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun finishWithStatus(status: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(MockParkingContract.RESULT_STATUS, status))
        finish()
    }
}

@Composable
private fun MockParkingTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MockColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            content()
        }
    }
}

@Composable
fun MockParkingScreen(
    action: String,
    zone: String,
    plate: String,
    durationMinutes: Int,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
    onError: () -> Unit,
) {
    val actionLabel = when (action) {
        MockParkingContract.ACTION_EXTEND -> "Verlängerung"
        MockParkingContract.ACTION_STOP -> "Beenden"
        else -> "Parkvorgang starten"
    }
    val confirmLabel = when (action) {
        MockParkingContract.ACTION_EXTEND -> stringResource(R.string.btn_confirm_extend)
        MockParkingContract.ACTION_STOP -> stringResource(R.string.btn_confirm_stop)
        else -> stringResource(R.string.btn_confirm_start)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mock_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.mock_test_provider),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.mock_prefilled_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    DetailRow(label = stringResource(R.string.label_zone), value = zone)
                    DetailRow(label = stringResource(R.string.label_plate), value = plate)
                    DetailRow(
                        label = stringResource(R.string.label_duration),
                        value = "$durationMinutes Min.",
                    )
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(confirmLabel)
            }
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(stringResource(R.string.mock_deny))
            }
            TextButton(
                onClick = onError,
                modifier = Modifier.fillMaxWidth(),
                colors = TextButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.btn_error))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF071110)
@Composable
private fun MockParkingScreenPreview() {
    MockParkingTheme {
        MockParkingScreen(
            action = MockParkingContract.ACTION_START,
            zone = "1234",
            plate = "ZH 123456",
            durationMinutes = 30,
            onConfirm = {},
            onDeny = {},
            onError = {},
        )
    }
}
