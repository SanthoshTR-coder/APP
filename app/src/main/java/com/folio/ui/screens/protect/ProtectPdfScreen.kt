package com.folio.ui.screens.protect

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.domain.usecase.security.PasswordStrength
import com.folio.ui.components.*
import com.folio.ui.theme.FolioTheme
import com.folio.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectPdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProtectViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val allowPrinting by viewModel.allowPrinting.collectAsStateWithLifecycle()
    val allowCopying by viewModel.allowCopying.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val passwordStrength by viewModel.passwordStrength.collectAsStateWithLifecycle()
    val passwordsMatch by viewModel.passwordsMatch.collectAsStateWithLifecycle()
    val adsRemoved by viewModel.adsRemoved.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val accentColor = FolioTheme.colors.secureAccent
    val pastelColor = FolioTheme.colors.securePastel

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.selectFile(it) } }

    Scaffold(
        topBar = {
            FolioTopBar(
                title = "Protect PDF",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ProtectUiState.Success -> {
                FolioSuccessScreen(
                    outputFileName = state.outputFile.name,
                    originalSize = state.originalSize,
                    outputSize = state.outputSize,
                    operationLabel = "Protection Applied",
                    onShare = {
                        val shareUri = FileUtil.getShareableUri(context, state.outputFile)
                        FileUtil.shareFile(context, shareUri, "application/pdf")
                    },
                    onOpen = {
                        val openUri = FileUtil.getShareableUri(context, state.outputFile)
                        FileUtil.openFile(context, openUri, "application/pdf")
                    },
                    onDone = { viewModel.clearAll() }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File Picker
                    if (selectedFile == null) {
                        FilePicker(
                            label = "Select PDF to protect",
                            onPickFile = { filePicker.launch(arrayOf("application/pdf")) }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = pastelColor.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFile!!.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = FileUtil.formatFileSize(selectedFile!!.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.clearAll() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove file")
                                }
                            }
                        }
                    }

                    // Password Section
                    AnimatedVisibility(visible = selectedFile != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Set Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Password field
                            OutlinedTextField(
                                value = password,
                                onValueChange = { viewModel.setPassword(it) },
                                label = { Text("Password") },
                                placeholder = { Text("Enter a strong password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor
                                )
                            )

                            // Password strength indicator
                            AnimatedVisibility(visible = passwordStrength != null) {
                                passwordStrength?.let { strength ->
                                    PasswordStrengthIndicator(strength = strength, accentColor = accentColor)
                                }
                            }

                            // Confirm password field
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { viewModel.setConfirmPassword(it) },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("Re-enter your password") },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (confirmPasswordVisible) "Hide" else "Show"
                                        )
                                    }
                                },
                                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                                supportingText = {
                                    if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                                        Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    cursorColor = accentColor
                                )
                            )

                            // Permissions Section
                            Text(
                                text = "Document Permissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Print,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Allow Printing", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Switch(
                                            checked = allowPrinting,
                                            onCheckedChange = { viewModel.setAllowPrinting(it) },
                                            colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Allow Copying", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Switch(
                                            checked = allowCopying,
                                            onCheckedChange = { viewModel.setAllowCopying(it) },
                                            colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                                        )
                                    }
                                }
                            }

                            // Error state
                            if (uiState is ProtectUiState.Error) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text(
                                            text = (uiState as ProtectUiState.Error).message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            // Protect button
                            FolioButton(
                                text = if (uiState is ProtectUiState.Processing) "Protecting..." else "Protect PDF",
                                onClick = { viewModel.protect() },
                                enabled = passwordsMatch && uiState !is ProtectUiState.Processing,
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Ad banner
                    if (!adsRemoved) {
                        Spacer(modifier = Modifier.weight(1f))
                        AdBanner(modifier = Modifier.fillMaxWidth())
                    }
                }

                // Progress sheet
                if (uiState is ProtectUiState.Processing) {
                    FolioProgressSheet(
                        fileName = selectedFile?.name ?: "",
                        operationLabel = "Encrypting",
                        progress = progress.fraction,
                        onCancel = { /* Cannot cancel mid-encrypt */ },
                        onDismiss = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength, accentColor: androidx.compose.ui.graphics.Color) {
    val (label, color, fraction) = when (strength) {
        PasswordStrength.WEAK -> Triple("Weak", MaterialTheme.colorScheme.error, 0.25f)
        PasswordStrength.FAIR -> Triple("Fair", MaterialTheme.colorScheme.tertiary, 0.5f)
        PasswordStrength.STRONG -> Triple("Strong", accentColor, 0.75f)
        PasswordStrength.VERY_STRONG -> Triple("Very Strong", accentColor, 1f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
