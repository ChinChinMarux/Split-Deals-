package com.example.splitthedeals.domain

import java.util.UUID

enum class SplitMode { EVEN, BY_ITEM }

enum class TaxKind { NONE, PERCENT, FIXED }

enum class DiscountKind { NONE, PERCENT, FIXED }

enum class BillStatus { BERJALAN, SELESAI }

data class PersonInput(
    val id: String,
    val name: String,
)

data class ItemInput(
    val id: String,
    val name: String,
    val priceCents: Long,
    val sharerIds: List<String>,
)


data class PaymentStatus(
    val personId: String,
    val paid: Boolean = false,
)

data class BillInput(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val people: List<PersonInput>,
    val items: List<ItemInput>,
    val mode: SplitMode,
    val taxKind: TaxKind,
    val taxPercentBps: Int = 0,
    val taxCents: Long = 0L,
    val otherCostsCents: Long = 0L,
    val discountKind: DiscountKind = DiscountKind.NONE,
    val discountPercentBps: Int = 0,
    val discountCents: Long = 0L,
    val status: BillStatus = BillStatus.BERJALAN,
    val payments: List<PaymentStatus> = emptyList(),
    val createdAt: Long = 0L,
)

data class ItemAllocation(
    val itemId: String = "",
    val name: String,
    val amountCents: Long,
)

data class PersonResult(
    val personId: String,
    val name: String,
    val itemLines: List<ItemAllocation>,
    val itemCents: Long,
    val taxCents: Long,
    val otherCostsCents: Long,
    val discountCents: Long,
    val amountCents: Long,
)

data class SplitResult(
    val totalItemsCents: Long,
    val taxCents: Long,
    val otherCostsCents: Long,
    val discountCents: Long,
    val grandTotalCents: Long,
    val people: List<PersonResult>,
)
