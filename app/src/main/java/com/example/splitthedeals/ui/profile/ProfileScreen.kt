package com.example.splitthedeals.ui.profile

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.splitthedeals.data.ProfileStore
import com.example.splitthedeals.ui.common.ErrorState
import com.example.splitthedeals.ui.common.HairlineCard
import com.example.splitthedeals.ui.common.LoadingState
import com.example.splitthedeals.ui.common.PrimaryButton
import com.example.splitthedeals.ui.common.ScreenHeader
import com.example.splitthedeals.ui.common.SectionHeader
import com.example.splitthedeals.ui.common.UiState
import com.example.splitthedeals.ui.navigation.Screen

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: ProfileViewModel = viewModel { ProfileViewModel(ProfileStore(context)) }
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Profil", "Data kamu dan info aplikasi.", Modifier.weight(1f))
                OutlinedButton(onClick = { navController.navigate(Screen.EditProfile) }) {
                    Text("Edit Profil")
                }
            }
        }

        when (val s = state) {
            is UiState.Loading -> item { LoadingState() }
            is UiState.Error -> item { ErrorState(s.message, onRetry = { vm.reload() }) }
            is UiState.Success -> {
                val profile = s.data
                item {
                    HairlineCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profile.photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = profile.photoUri,
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
                            Column {
                                Text(profile.name.ifBlank { "Tanpa nama" }, style = MaterialTheme.typography.titleLarge)
                                if (profile.email.isNotBlank()) {
                                    Text(profile.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.padding(top = 12.dp))
                        InfoRow("Email", profile.email)
                        InfoRow("Telepon", profile.phone)
                        InfoRow("Deskripsi", profile.description)
                    }
                }
                item { SectionHeader("About") }
                item {
                    HairlineCard {
                        val info = appInfo(context)
                        Text("Split the Deals", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Versi ${info.first}\nAplikasi Android split bill untuk penggunaan pribadi skala kecil. Berjalan offline-first dan tersinkron dengan Web Service sendiri (Express + libSQL/Turso) yang di-deploy di Vercel.\n Dibuat untuk memenuhi Tugas Akhir Praktikum PPB - Evan Adkara.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun appInfo(context: android.content.Context): Pair<String, String> {
    return try {
        val pm = context.packageManager
        val pkg = context.packageName
        val ver = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.0"
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0).versionName ?: "1.0"
        }
        Pair(ver, pkg)
    } catch (e: Exception) {
        Pair("1.0", context.packageName)
    }
}
