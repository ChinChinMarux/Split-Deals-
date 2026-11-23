package com.example.splitthedeals.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.domain.BillInput
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
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel { HomeViewModel(BillStore(context)) }
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader("Home", "Ringkasan tagihan.") }

        when (val s = state) {
            is UiState.Loading -> item { LoadingState() }
            is UiState.Error -> item { ErrorState(s.message, onRetry = { vm.load() }) }
            is UiState.Success -> {
                val data = s.data
                item { SummaryCard(data.summary) }
                item { Spacer(Modifier.padding(top = 4.dp)) }
                item {
                    PrimaryButton(
                        text = "Buat tagihan baru",
                        onClick = { navController.navigate(Screen.splitNew()) },
                    )
                }
                item { SectionHeader("Tagihan berjalan") }
                if (data.runningBills.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Belum ada tagihan berjalan",
                            hint = "Buat tagihan baru untuk mulai.",
                        )
                    }
                } else {
                    items(data.runningBills.size, key = { data.runningBills[it].id }) { index ->
                        RunningBillItem(data.runningBills[index]) {
                            navController.navigate(Screen.billDetail(data.runningBills[index].id))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}

@Composable
private fun SummaryCard(summary: HomeSummary) {
    HairlineCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryCell("Berjalan", summary.runningCount.toString())
            SummaryCell("Selesai", summary.doneCount.toString())
            SummaryCell("Belum bayar", summary.totalUnpaid.toString())
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RunningBillItem(bill: BillInput, onClick: () -> Unit) {
    val total = remember(bill.id) { SplitCalculator.calculate(bill).grandTotalCents }
    val date = remember(bill.createdAt) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(bill.createdAt))
    }
    val paid = bill.payments.count { it.paid }
    val totalPeople = bill.people.size
    HairlineCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(bill.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$date  ·  $paid/$totalPeople lunas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MoneyText(total, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
