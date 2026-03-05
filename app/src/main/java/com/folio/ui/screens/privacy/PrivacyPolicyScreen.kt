package com.folio.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.folio.ui.components.FolioTopBar
import com.folio.ui.theme.FolioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Privacy Policy",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FolioTheme.colors.secureAccent.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = FolioTheme.colors.secureAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Your Privacy Matters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Folio processes everything locally on your device. We never upload, collect, or store your files.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Principles
            PrivacySection(
                icon = Icons.Default.PhoneAndroid,
                title = "100% On-Device Processing",
                content = "All PDF operations — merging, splitting, compressing, converting, signing, and editing — happen entirely on your device. No file is ever uploaded to any server."
            )

            PrivacySection(
                icon = Icons.Default.CloudOff,
                title = "No Cloud Storage",
                content = "Folio does not use any cloud services. Your documents are never sent to, processed by, or stored on external servers."
            )

            PrivacySection(
                icon = Icons.Default.VisibilityOff,
                title = "No Data Collection",
                content = "We do not collect, track, or analyze the content of your documents. We do not collect personal information, browsing data, or usage patterns."
            )

            PrivacySection(
                icon = Icons.Default.Storage,
                title = "Local Storage Only",
                content = "Folio stores only: operation history metadata (file names, sizes, timestamps) and your app preferences (theme, settings). No file content is ever stored in the database."
            )

            PrivacySection(
                icon = Icons.Default.Delete,
                title = "Temporary Files",
                content = "During operations, temporary files may be created in the app's cache directory. These are automatically cleaned up after each operation completes."
            )

            PrivacySection(
                icon = Icons.Default.Ads,
                title = "Advertising",
                content = "Folio uses Google AdMob to display banner and interstitial ads. Google may collect anonymous device data as described in their privacy policy. You can remove all ads with a one-time in-app purchase."
            )

            PrivacySection(
                icon = Icons.Default.ShoppingCart,
                title = "In-App Purchases",
                content = "Purchases are processed through Google Play Billing. Folio does not store or process payment information directly. All billing is handled by Google."
            )

            PrivacySection(
                icon = Icons.Default.Security,
                title = "File Permissions",
                content = "Folio requests storage permissions only to read your selected files and save processed output. We use Android's FileProvider for secure file sharing."
            )

            // Contact
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Questions?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "If you have any questions about this privacy policy, please contact us through the Google Play Store listing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Effective date
            Text(
                text = "Effective Date: March 2026 · Folio v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrivacySection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = FolioTheme.colors.secureAccent,
                modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
