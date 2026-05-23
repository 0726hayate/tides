package com.hayate0726.tides.ui.feedback

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun FeedbackScreen() {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf("") }
    var includeDiagnostic by remember { mutableStateOf(false) }

    val diagnosticBlock = if (includeDiagnostic) {
        "\n\n--- Diagnostic ---\n" +
            "App version: 0.1.0\n" +
            "Android: ${android.os.Build.VERSION.SDK_INT}\n" +
            "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n"
    } else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            "Two channels. Pick the one that fits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = includeDiagnostic, onCheckedChange = { includeDiagnostic = it })
            Text("Include diagnostic info (app version, device, no cycle data)",
                 style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.size(24.dp))

        Button(
            onClick = {
                val body = (message + diagnosticBlock).let { Uri.encode(it) }
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://github.com/0726hayate/tides/issues/new?labels=feedback&body=$body"
                    )
                )
                ctx.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Report a bug (public — GitHub)") }

        Spacer(Modifier.size(12.dp))

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:0726hayate@gmail.com")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Tides feedback")
                    putExtra(Intent.EXTRA_TEXT, message + diagnosticBlock)
                }
                ctx.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Send private feedback (email)") }
    }
}
