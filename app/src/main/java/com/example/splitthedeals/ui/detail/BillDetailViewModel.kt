package com.example.splitthedeals.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitthedeals.data.BillStore
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.domain.PaymentStatus
import com.example.splitthedeals.domain.SplitCalculator
import com.example.splitthedeals.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.splitthedeals.domain.SplitResult
import kotlinx.coroutines.Dispatchers

data class BillDetailData(
    val bill: BillInput,
    val result: SplitResult,
    val grandTotal: Long,
    val perPersonAmount: Map<String, Long>,
    val paidCount: Int,
    val totalPeople: Int,
)

class BillDetailViewModel(private val store: BillStore, private val billId: String) : ViewModel() {

    private val _state = MutableStateFlow<UiState<BillDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<BillDetailData>> = _state.asStateFlow()

    init {
        _state.value = UiState.Loading
        viewModelScope.launch {
            BillStore.bills.collect { bills ->
                val bill = bills.firstOrNull { it.id == billId } ?: store.load(billId)
                if (bill == null) {
                    _state.value = UiState.Error("Bill tidak ditemukan.")
                } else {
                    publish(bill)
                }
            }
        }
    }

    fun load() {
        store.refresh()
    }

    fun togglePayment(personId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bill = store.load(billId) ?: return@launch
            val updated = bill.copy(
                payments = bill.people.map { p ->
                    val existing = bill.payments.firstOrNull { it.personId == p.id }
                    if (p.id == personId) PaymentStatus(p.id, !(existing?.paid ?: false))
                    else existing ?: PaymentStatus(p.id, false)
                },
            )
            store.save(updated)
            publish(updated)
        }
    }


    fun selesaikan() {
        viewModelScope.launch(Dispatchers.IO) {
            val bill = store.load(billId) ?: return@launch
            store.save(bill.copy(status = BillStatus.SELESAI))
        }
    }

    fun prepareForEdit() {
        viewModelScope.launch(Dispatchers.IO) {
            val bill = store.load(billId) ?: return@launch
            if (bill.status == BillStatus.SELESAI) {
                store.save(bill.copy(status = BillStatus.BERJALAN))
            }
        }
    }

    private fun publish(bill: BillInput) {
        try {
            val result = SplitCalculator.calculate(bill)
            val perPerson = result.people.associate { it.personId to it.amountCents }
            val paid = bill.payments.count { it.paid }
            _state.value = UiState.Success(
                BillDetailData(bill, result, result.grandTotalCents, perPerson, paid, bill.people.size),
            )
        } catch (e: IllegalArgumentException) {
            _state.value = UiState.Error(e.message ?: "Data bill tidak valid.")
        }
    }
}
