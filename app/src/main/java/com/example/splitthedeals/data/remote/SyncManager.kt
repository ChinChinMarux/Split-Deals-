package com.example.splitthedeals.data.remote

import com.example.splitthedeals.domain.BillInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SyncManager {
    private val api get() = RetrofitClient.api

    fun push(bill: BillInput) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.saveBill(bill) }
        }
    }

    fun delete(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.deleteBill(id) }
        }
    }

    suspend fun pull(): List<BillInput> =
        runCatching { api.getBills() }.getOrDefault(emptyList())

    fun pushStatus(id: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.updateStatus(id, mapOf("status" to status)) }
        }
    }

    fun pushPaid(id: String, memberId: String, paid: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.updatePaid(id, memberId, mapOf("paid" to paid)) }
        }
    }
}
