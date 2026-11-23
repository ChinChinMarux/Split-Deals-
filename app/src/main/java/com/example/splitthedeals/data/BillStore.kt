package com.example.splitthedeals.data

import android.content.Context
import androidx.core.content.edit
import com.example.splitthedeals.domain.BillInput
import com.example.splitthedeals.domain.BillStatus
import com.example.splitthedeals.domain.DiscountKind
import com.example.splitthedeals.domain.ItemInput
import com.example.splitthedeals.domain.PaymentStatus
import com.example.splitthedeals.domain.PersonInput
import com.example.splitthedeals.domain.SplitMode
import com.example.splitthedeals.domain.TaxKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class BillStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("split_the_bills", Context.MODE_PRIVATE)

    init { refresh() }

    fun save(bill: BillInput): BillInput {
        val toStore = if (bill.createdAt == 0L) {
            bill.copy(createdAt = System.currentTimeMillis())
        } else {
            bill
        }
        val all = loadAll().filterNot { it.id == toStore.id } + toStore
        prefs.edit { putString(KEY, encode(all)) }
        refresh()
        return toStore
    }

    fun loadAll(): List<BillInput> = Companion.bills.value

    fun load(id: String): BillInput? = loadAll().firstOrNull { it.id == id }

    fun delete(id: String) {
        val all = loadAll().filterNot { it.id == id }
        prefs.edit { putString(KEY, encode(all)) }
        refresh()
    }

    fun refresh() {
        Companion._bills.value = decode(prefs.getString(KEY, null))
    }

    companion object {
        private const val KEY = "bills"
        private val _bills = MutableStateFlow<List<BillInput>>(emptyList())
        val bills: StateFlow<List<BillInput>> = _bills.asStateFlow()
    }

    private fun encode(bills: List<BillInput>): String {
        val array = JSONArray()
        bills.forEach { bill ->
            val people = JSONArray()
            bill.people.forEach { person ->
                people.put(JSONObject().put("id", person.id).put("name", person.name))
            }
            val items = JSONArray()
            bill.items.forEach { item ->
                val sharers = JSONArray()
                item.sharerIds.forEach { sharers.put(it) }
                items.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("name", item.name)
                        .put("priceCents", item.priceCents)
                        .put("sharerIds", sharers),
                )
            }
            val payments = JSONArray()
            bill.payments.forEach { p ->
                payments.put(JSONObject().put("personId", p.personId).put("paid", p.paid))
            }
            array.put(
                JSONObject()
                    .put("id", bill.id)
                    .put("title", bill.title)
                    .put("mode", bill.mode.name)
                    .put("taxKind", bill.taxKind.name)
                    .put("taxPercentBps", bill.taxPercentBps)
                    .put("taxCents", bill.taxCents)
                    .put("otherCostsCents", bill.otherCostsCents)
                    .put("discountKind", bill.discountKind.name)
                    .put("discountPercentBps", bill.discountPercentBps)
                    .put("discountCents", bill.discountCents)
                    .put("status", bill.status.name)
                    .put("payments", payments)
                    .put("createdAt", bill.createdAt)
                    .put("people", people)
                    .put("items", items),
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<BillInput> {
        if (raw.isNullOrEmpty()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val bill = array.getJSONObject(index)
                val people = bill.getJSONArray("people")
                val items = bill.getJSONArray("items")
                val payments = bill.optJSONArray("payments")
                BillInput(
                    id = bill.getString("id"),
                    title = bill.getString("title"),
                    people = (0 until people.length()).map { position ->
                        val obj = people.getJSONObject(position)
                        PersonInput(obj.getString("id"), obj.getString("name"))
                    },
                    items = (0 until items.length()).map { position ->
                        val obj = items.getJSONObject(position)
                        val sharers = obj.getJSONArray("sharerIds")
                        ItemInput(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            priceCents = obj.getLong("priceCents"),
                            sharerIds = (0 until sharers.length()).map { sharers.getString(it) },
                        )
                    },
                    mode = SplitMode.valueOf(bill.getString("mode")),
                    taxKind = TaxKind.valueOf(bill.optString("taxKind", bill.optString("tipKind", "NONE"))),
                    taxPercentBps = bill.optInt("taxPercentBps", bill.optInt("tipPercentBps", 0)),
                    taxCents = bill.optLong("taxCents", bill.optLong("tipCents", 0L)),
                    otherCostsCents = bill.optLong("otherCostsCents", 0L),
                    discountKind = DiscountKind.valueOf(bill.optString("discountKind", "NONE")),
                    discountPercentBps = bill.optInt("discountPercentBps", 0),
                    discountCents = bill.optLong("discountCents", 0L),
                    status = BillStatus.valueOf(bill.optString("status", "BERJALAN")),
                    payments = if (payments == null) emptyList() else (0 until payments.length()).map { position ->
                        val obj = payments.getJSONObject(position)
                        PaymentStatus(obj.getString("personId"), obj.optBoolean("paid", false))
                    },
                    createdAt = bill.optLong("createdAt", 0L),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


}
