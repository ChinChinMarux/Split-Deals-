package com.example.splitthedeals.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeSummary(
    val runningCount: Int,
    val doneCount: Int,
    val totalUnpaid: Int,
)

data class HomeData(
    val summary: HomeSummary,
    val runningBills: List<BillInput>,
)

class HomeViewModel(private val store: BillStore) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    init {
        _state.value = UiState.Loading
        viewModelScope.launch {
            BillStore.bills.collect { bills ->
                try {
                    val sorted = bills.sortedByDescending { it.createdAt }
                    val running = sorted.filter { it.status == BillStatus.BERJALAN }
                    val done = sorted.filter { it.status == BillStatus.SELESAI }
                    val totalUnpaid = running.sumOf { bill ->
                        val total = bill.people.size
                        val paid = bill.payments.count { it.paid }
                        (total - paid).coerceAtLeast(0)
                    }
                    _state.value = UiState.Success(HomeData(HomeSummary(running.size, done.size, totalUnpaid), running))
                } catch (e: Exception) {
                    _state.value = UiState.Error("Gagal memuat data: ${e.message ?: "tidak diketahui"}")
                }
            }
        }
    }

    fun load() {
        store.refresh()
    }
}
