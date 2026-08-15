package com.energy.app.ui.screens.contact

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.energy.app.BuildConfig
import com.energy.app.ui.components.EnergyButton
import com.energy.app.ui.components.HairlineCard

/**
 * Help & Contact — FAQ + direct email to the developer (APP_SPEC §5.8).
 */
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Help & Contact",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Something broken or missing? Tell us — we read everything.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        HairlineCard {
            Text(
                text = "📧 Email the developer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "spirit.of.kaiky@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            EnergyButton(
                text = "✉️  Send an email",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:spirit.of.kaiky@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Energy app feedback")
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        HairlineCard {
            Text(
                text = "Frequently asked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            FaqItem(
                q = "Does the app work without an account?",
                a = "Yes — guest mode keeps everything on your device. Create an account (or link Google) to sync to the cloud."
            )
            FaqItem(
                q = "Why is my route not appearing on the map?",
                a = "Check that location permission is granted and that you're outdoors with GPS on. Indoor environments can block the signal."
            )
            FaqItem(
                q = "Is my data private?",
                a = "Yes. Guest data never leaves your device. With an account, your data is protected by Supabase row-level security — only you can read it."
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Energy v${BuildConfig.VERSION_NAME} · Made with ⚡",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaqItem(q: String, a: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            text = q,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = a,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
