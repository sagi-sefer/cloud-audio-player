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
    currentAuthData: String,
    currentRootFolder: String,
    onSave: (StorageType, String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onDropboxSignInClick: (String) -> Unit,
    onBrowseFoldersClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentStorageType) }
    var tokenInput by remember { mutableStateOf(currentAuthData) }
    var rootFolderInput by remember { mutableStateOf(currentRootFolder) }
    var appKeyInput by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(currentRootFolder) {
        rootFolderInput = currentRootFolder
    }
    androidx.compose.runtime.LaunchedEffect(currentAuthData) {
        tokenInput = currentAuthData
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

                // Provider Switcher Buttons
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
                    text = "Selected Music Root Folder:",
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
                        value = rootFolderInput,
                        onValueChange = { rootFolderInput = it },
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
                    Button(
                        onClick = onGoogleSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Text(
                            text = "Sign in with Google Account",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "— OR PASTE TOKEN MANUALLY —",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (selectedType == StorageType.DROPBOX) {
                    BidiText(
                        text = "Dropbox App Key (Client ID):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = appKeyInput,
                        onValueChange = { appKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter Dropbox App Key") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onDropboxSignInClick(appKeyInput) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061FF))
                    ) {
                        Text(
                            text = "Sign in with Dropbox",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "— OR PASTE TOKEN MANUALLY —",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                BidiText(
                    text = if (selectedType == StorageType.GOOGLE_DRIVE)
                        "Google Drive OAuth Bearer Token:"
                    else "Dropbox Access Token:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste auth token here...") },
                    singleLine = false,
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

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
                        onClick = { onSave(selectedType, tokenInput, rootFolderInput) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
