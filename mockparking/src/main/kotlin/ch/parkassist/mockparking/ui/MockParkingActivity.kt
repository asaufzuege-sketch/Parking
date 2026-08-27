package ch.parkassist.mockparking.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.parkassist.mockparking.contract.MockParkingContract

/**
 * Mock Parking Activity – simulates a real parking provider UI.
 *
 * Receives parking parameters via explicit Intent from ParkingAssistant.
 * Returns RESULT_OK + status extra (CONFIRMED/DENIED/ERROR) or RESULT_CANCELED.
 *
 * Intent actions (see MockParkingContract):
 *   ACTION_START_PARKING, ACTION_EXTEND_PARKING, ACTION_STOP_PARKING
 */
class MockParkingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action ?: MockParkingContract.ACTION_START
        val zone = intent?.getStringExtra(MockParkingContract.EXTRA_ZONE) ?: ""
        val plate = intent?.getStringExtra(MockParkingContract.EXTRA_PLATE) ?: ""
        val duration = intent?.getIntExtra(MockParkingContract.EXTRA_DURATION_MINUTES, 0) ?: 0

        setContent {
            MaterialTheme {
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

    private fun finishWithStatus(status: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(MockParkingContract.RESULT_STATUS, status))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        MockParkingContract.ACTION_STOP   -> "Beenden"
        else                              -> "Parkvorgang starten"
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mock Parking") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Aktion: $actionLabel", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Text("Zone: $zone")
            Text("Kennzeichen: $plate")
            Text("Dauer: $durationMinutes Min.")
            HorizontalDivider()
            Text(
                "Vorausgefüllte Daten (Mock). Bitte Aktion bestätigen oder ablehnen.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("Bestätigen (Mock)")
            }
            OutlinedButton(onClick = onDeny, modifier = Modifier.fillMaxWidth()) {
                Text("Ablehnen")
            }
            TextButton(onClick = onError, modifier = Modifier.fillMaxWidth()) {
                Text("Fehler simulieren")
            }
        }
    }
}
