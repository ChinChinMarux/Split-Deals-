package com.example.splitthedeals.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.data.remote.SyncManager
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RiwayatViewModel(private val store: BillStore) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<BillInput>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BillInput>>> = _state.asStateFlow()

    init {
        _state.value = UiState.Loading
        viewModelScope.launch {
            SyncManager.pull().forEach { store.save(it) }
        }
        viewModelScope.launch {
            BillStore.bills.collect { bills ->
                try {
                    _state.value = UiState.Success(bills.sortedByDescending { it.createdAt })
                } catch (e: Exception) {
                    _state.value = UiState.Error("Gagal memuat riwayat: ${e.message ?: "tidak diketahui"}")
                }
            }
        }
    }

    fun load() {
        store.refresh()
    }

    fun delete(id: String) {
        store.delete(id)
        SyncManager.delete(id)
    }
}
