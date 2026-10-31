package com.example.cloudaudioplayer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cloudaudioplayer.domain.model.StorageType
import com.example.cloudaudioplayer.presentation.components.BidiText

@Composable
fun CloudAuthDialog(
    currentStorageType: StorageType,
    currentGoogleAuth: String,
    currentGoogleRoot: String,
    currentDropboxAuth: String,
    currentDropboxRoot: String,
    onSave: (StorageType, String, String, String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onDropboxSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBrowseFoldersClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentStorageType) }
    var googleTokenInput by remember { mutableStateOf(currentGoogleAuth) }
    var googleRootInput by remember { mutableStateOf(currentGoogleRoot) }
    var dropboxRootInput by remember { mutableStateOf(currentDropboxRoot) }
    var dropboxAuthCurrent by remember { mutableStateOf(currentDropboxAuth) }

    androidx.compose.runtime.LaunchedEffect(currentGoogleAuth, currentGoogleRoot, currentDropboxAuth, currentDropboxRoot, currentStorageType) {
        selectedType = currentStorageType
        googleTokenInput = currentGoogleAuth
        googleRootInput = currentGoogleRoot
        dropboxRootInput = currentDropboxRoot
        dropboxAuthCurrent = currentDropboxAuth
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BidiText(
                    text = "CLOUD STORAGE SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Switcher Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val driveSelected = selectedType == StorageType.GOOGLE_DRIVE
                    Button(
                        onClick = { selectedType = StorageType.GOOGLE_DRIVE },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (driveSelected) MaterialTheme.colorScheme.primary else Color(0xFF333344)
                        )
                    ) {
                        Text(
                            text = "Google Drive",
                            color = if (driveSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val dropboxSelected = selectedType == StorageType.DROPBOX
                    Button(
                        onClick = { selectedType = StorageType.DROPBOX },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dropboxSelected) MaterialTheme.colorScheme.primary else Color(0xFF333344)
                        )
                    ) {
                        Text(
                            text = "Dropbox",
                            color = if (dropboxSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                BidiText(
                    text = if (selectedType == StorageType.GOOGLE_DRIVE) "Google Drive Music Root Folder:" else "Dropbox Music Root Folder:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (selectedType == StorageType.GOOGLE_DRIVE) googleRootInput else dropboxRootInput,
                        onValueChange = { newVal ->
                            if (selectedType == StorageType.GOOGLE_DRIVE) {
                                googleRootInput = newVal
                            } else {
                                dropboxRootInput = newVal
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Music") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = onBrowseFoldersClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Browse", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedType == StorageType.GOOGLE_DRIVE) {
                    val isGoogleLoggedIn = googleTokenInput.isNotBlank()
                    Button(
                        onClick = if (isGoogleLoggedIn) onSignOutClick else onGoogleSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGoogleLoggedIn) Color(0xFFD32F2F) else Color(0xFF4285F4)
                        )
                    ) {
                        Text(
                            text = if (isGoogleLoggedIn) "Sign out of Google Drive" else "Sign in with Google Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isGoogleLoggedIn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected via Google Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (selectedType == StorageType.DROPBOX) {
                    val isDropboxLoggedIn = dropboxAuthCurrent.isNotBlank()
                    Button(
                        onClick = if (isDropboxLoggedIn) onSignOutClick else onDropboxSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDropboxLoggedIn) Color(0xFFD32F2F) else Color(0xFF0061FF)
                        )
                    ) {
                        Text(
                            text = if (isDropboxLoggedIn) "Sign out of Dropbox" else "Sign in with Dropbox",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isDropboxLoggedIn) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connected via Dropbox Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSave(selectedType, googleTokenInput, googleRootInput, dropboxAuthCurrent, dropboxRootInput)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
