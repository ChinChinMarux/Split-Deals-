package com.example.splitthedeals.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.domain.SplitCalculator
import com.example.splitthedeals.ui.common.EmptyState
import com.example.splitthedeals.ui.common.ErrorState
import com.example.splitthedeals.ui.common.HairlineCard
import com.example.splitthedeals.ui.common.LoadingState
import com.example.splitthedeals.ui.common.MoneyText
import com.example.splitthedeals.ui.common.PrimaryButton
import com.example.splitthedeals.ui.common.ScreenHeader
import com.example.splitthedeals.ui.common.SectionHeader
import com.example.splitthedeals.ui.common.UiState
import com.example.splitthedeals.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiwayatScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: RiwayatViewModel = viewModel { RiwayatViewModel(BillStore(context)) }
    val state by vm.state.collectAsState()

    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Hapus bill?") },
            text = { Text("Bill yang dihapus tidak dapat dikembalikan.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteId?.let { vm.delete(it) }
                    pendingDeleteId = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Batal") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader("Riwayat", "Tagihan berjalan dan riwayat selesai.") }

        when (val s = state) {
            is UiState.Loading -> item { LoadingState() }
            is UiState.Error -> item { ErrorState(s.message, onRetry = { vm.load() }) }
            is UiState.Success -> {
                val bills = s.data
                if (bills.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Belum ada bill tersimpan",
                            hint = "Buat bill di tab Split lalu simpan.",
                        )
                    }
                } else {
                    val running = bills.filter { it.status == BillStatus.BERJALAN }
                    val done = bills.filter { it.status == BillStatus.SELESAI }

                    item { SectionHeader("Tagihan berjalan") }
                    if (running.isEmpty()) {
                        item { EmptyState("Belum ada tagihan berjalan", "Tagihan baru tampil di sini.") }
                    } else {
                        items(running.size, key = { running[it].id }) { index ->
                            val bill = running[index]
                            val total = remember(bill.id) { SplitCalculator.calculate(bill).grandTotalCents }
                            val date = remember(bill.createdAt) {
                                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(bill.createdAt))
                            }
                            HairlineCard(onClick = { navController.navigate(Screen.billDetail(bill.id)) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(bill.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "$date  ·  ${statusLabel(bill.status)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    MoneyText(total, style = MaterialTheme.typography.headlineSmall)
                                }
                                OutlinedButton(
                                    onClick = { pendingDeleteId = bill.id },
                                    modifier = Modifier.padding(top = 8.dp),
                                ) { Text("Hapus") }
                            }
                        }
                    }

                    item { SectionHeader("Riwayat tagihan") }
                    if (done.isEmpty()) {
                        item { EmptyState("Belum ada tagihan selesai", "Tagihan yang sudah lunas tampil di sini.") }
                    } else {
                        items(done.size, key = { done[it].id }) { index ->
                            val bill = done[index]
                            val total = remember(bill.id) { SplitCalculator.calculate(bill).grandTotalCents }
                            val date = remember(bill.createdAt) {
                                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(bill.createdAt))
                            }
                            CompletedBillCard(
                                bill = bill,
                                total = total,
                                date = date,
                                onClick = { navController.navigate(Screen.billDetail(bill.id)) },
                                onDelete = { pendingDeleteId = bill.id },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.padding(top = 8.dp)) }
                item {
                    PrimaryButton(text = "Buat tagihan baru", onClick = { navController.navigate(Screen.splitNew()) })
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun CompletedBillCard(
    bill: BillInput,
    total: Long,
    date: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selesai",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    bill.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$date  ·  Selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(total, style = MaterialTheme.typography.bodyLarge)
            }
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) { Text("Hapus") }
    }
}

private fun statusLabel(status: BillStatus): String =
    if (status == BillStatus.SELESAI) "Selesai" else "Berjalan"
