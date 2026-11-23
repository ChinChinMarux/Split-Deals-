package com.example.splitthedeals.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.domain.ItemAllocation
import com.example.splitthedeals.domain.Money
import com.example.splitthedeals.domain.SplitCalculator
import com.example.splitthedeals.domain.SplitMode
import com.example.splitthedeals.ui.common.ErrorState
import com.example.splitthedeals.ui.common.HairlineCard
import com.example.splitthedeals.ui.common.LoadingState
import com.example.splitthedeals.ui.common.MoneyText
import com.example.splitthedeals.ui.common.PrimaryButton
import com.example.splitthedeals.ui.common.ScreenHeader
import com.example.splitthedeals.ui.common.SectionHeader
import com.example.splitthedeals.ui.common.UiState
import com.example.splitthedeals.ui.navigation.Screen

@Composable
fun BillDetailScreen(navController: NavController, billId: String) {
    val context = LocalContext.current
    val vm: BillDetailViewModel = viewModel { BillDetailViewModel(BillStore(context), billId) }
    val state by vm.state.collectAsState()
    var pendingCompleteId by rememberSaveable { mutableStateOf<String?>(null) }

    if (pendingCompleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingCompleteId = null },
            text = { Text("Semua anggota sudah membayar tagihan. Selesaikan tagihan?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.selesaikan()
                    pendingCompleteId = null
                }) {
                    Text("Ya, selesaikan", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val id = pendingCompleteId
                    pendingCompleteId = null
                    vm.prepareForEdit()
                    if (id != null) navController.navigate(Screen.splitEdit(id))
                }) {
                    Text("Tidak, ubah tagihan", color = MaterialTheme.colorScheme.secondary)
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Detail Bill", null, Modifier.weight(1f))
                TextButton(onClick = { navController.popBackStack() }) { Text("Kembali") }
            }
        }

        when (val s = state) {
            is UiState.Loading -> item { LoadingState() }
            is UiState.Error -> item { ErrorState(s.message, onRetry = { vm.load() }) }
            is UiState.Success -> {
                val data = s.data
                val bill = data.bill
                val result = SplitCalculator.calculate(bill)

                item {
                    Text(
                        bill.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        statusText(bill.status) + "  ·  ${data.paidCount}/${data.totalPeople} lunas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item { SectionHeader("Ringkasan") }
                item {
                    HairlineCard {
                        SummaryRow("Total item", result.totalItemsCents)
                        SummaryRow("Pajak", result.taxCents)
                        SummaryRow("Biaya lain", result.otherCostsCents)
                        if (result.discountCents > 0L) {
                            SummaryRowDiskon(result.discountCents)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand total", style = MaterialTheme.typography.titleMedium)
                            MoneyText(result.grandTotalCents, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                item { SectionHeader("Catat pembayaran") }
                items(bill.people.size, key = { bill.people[it].id }) { index ->
                    val person = bill.people[index]
                    val amount = data.perPersonAmount[person.id] ?: 0L
                    val paid = bill.payments.firstOrNull { it.personId == person.id }?.paid ?: false
                    val ownerCountById = bill.items.associate { it.id to it.sharerIds.size }
                    HairlineCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${person.name.ifBlank { "Anggota ${index + 1}" }}: ",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    MoneyText(
                                        amount,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                                if (bill.mode == SplitMode.BY_ITEM) {
                                    val lines = result.people
                                        .firstOrNull { it.personId == person.id }
                                        ?.itemLines
                                        .orEmpty()
                                        .sortedWith(
                                            compareByDescending<ItemAllocation> { ownerCountById[it.itemId] ?: 0 }
                                                .thenByDescending { it.amountCents },
                                        )
                                    lines.forEach { line ->
                                        Text(
                                            "- ${line.name} . ${Money.format(line.amountCents)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Checkbox(
                                checked = paid,
                                onCheckedChange = { checked ->
                                    vm.togglePayment(person.id)
                                    if (checked && !paid &&
                                        data.paidCount + 1 == data.totalPeople &&
                                        bill.status != BillStatus.SELESAI
                                    ) {
                                        pendingCompleteId = bill.id
                                    }
                                },
                            )
                        }
                    }
                }

                item { Spacer(Modifier.padding(top = 8.dp)) }
                item {
                    PrimaryButton(
                        text = if (data.paidCount == data.totalPeople) "Selesaikan bill" else "Tandai selesai",
                        onClick = { vm.selesaikan() },
                        enabled = bill.status != BillStatus.SELESAI,
                    )
                }
                item {
                    OutlinedButton(
                        onClick = {
                            vm.prepareForEdit()
                            navController.navigate(Screen.splitEdit(bill.id))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (bill.status == BillStatus.SELESAI) "Ubah tagihan" else "Edit bill")
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun SummaryRow(label: String, cents: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        MoneyText(cents)
    }
}

@Composable
private fun SummaryRowDiskon(discountCents: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "- ${Money.format(discountCents)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun statusText(status: BillStatus): String =
    if (status == BillStatus.SELESAI) "Selesai" else "Berjalan"
