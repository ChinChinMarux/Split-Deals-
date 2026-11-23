package com.example.splitthedeals.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.splitthedeals.data.Profile
import com.example.splitthedeals.data.ProfileStore
import com.example.splitthedeals.ui.common.ErrorText
import com.example.splitthedeals.ui.common.LoadingState
import com.example.splitthedeals.ui.common.PrimaryButton
import com.example.splitthedeals.ui.common.ScreenHeader
import com.example.splitthedeals.ui.common.UiState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel { ProfileViewModel(ProfileStore(context)) }
    val state by vm.state.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {

            }
            photoUri = uri.toString()
        }
    }


    val currentState = state
    if (!loaded && currentState is UiState.Success) {
        val p = currentState.data
        name = p.name
        email = p.email
        phone = p.phone
        description = p.description
        photoUri = p.photoUri
        loaded = true
    }
    if (currentState is UiState.Error) {
        error = currentState.message
    }
    fun validate(): Boolean {
        emailError = when {
            email.isBlank() -> null
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                "Email tidak valid (harus memuat @ dan domain)."
            else -> null
        }
        phoneError = when {
            !phone.all { it.isDigit() } -> "Nomor hanya boleh angka."
            phone.length != 12 -> "Nomor harus 12 digit."
            else -> null
        }
        return emailError == null && phoneError == null
    }


    fun save() {
        if (name.isBlank() && email.isBlank() && phone.isBlank() &&
            description.isBlank() && photoUri.isBlank()
        ) {
            error = "Isi minimal satu field."
            return
        }
        error = null
        if (!validate()) return
        vm.save(Profile(photoUri, name.trim(), email.trim(), phone.trim(), description.trim()))
        navController.popBackStack()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Edit Profil", "Ubah foto dan identitas.", Modifier.weight(1f))
                TextButton(onClick = { navController.popBackStack() }) { Text("Batal") }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photoUri.isNotBlank()) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = "Foto profil",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.padding(start = 16.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                ) { Text("Pilih foto") }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("Email") },
                isError = emailError != null,
                supportingText = { emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 12) {
                        phone = input
                        phoneError = null
                    }
                },
                label = { Text("Nomor telepon") },
                isError = phoneError != null,
                supportingText = { Text(phoneError ?: "${phone.length}/12") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
        }
        item { ErrorText(error) }
        item {
            PrimaryButton(
                text = "Simpan",
                onClick = { save() },
            )
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}
