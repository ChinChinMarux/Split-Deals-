package com.example.splitthedeals.domain

import java.math.BigDecimal
import java.math.RoundingMode

object SplitCalculator {

    fun calculate(bill: BillInput): SplitResult {
        require(bill.people.size >= 2) { "Minimal 2 orang." }
        require(bill.items.isNotEmpty()) { "Minimal 1 item." }
        require(bill.items.all { it.priceCents >= 0 }) { "Harga item tidak boleh negatif." }

        val totalItemsCents = bill.items.sumOf { it.priceCents }
        require(totalItemsCents > 0) { "Total belanja harus lebih dari nol." }

        if (bill.mode == SplitMode.BY_ITEM) {
            require(bill.items.all { it.sharerIds.isNotEmpty() }) {
                "Setiap item harus punya minimal 1 anggota."
            }
            require(bill.items.all { item -> item.sharerIds.all { id -> bill.people.any { it.id == id } } }) {
                "Ada anggota yang tidak dikenal."
            }
        }

        val count = bill.people.size
        val itemCentsByPerson = MutableList(count) { 0L }
        val linesByPerson = Array(count) { mutableListOf<ItemAllocation>() }

        when (bill.mode) {
            SplitMode.EVEN -> {
                val shares = splitEven(totalItemsCents, count)
                for (index in 0 until count) itemCentsByPerson[index] = shares[index]
            }

            SplitMode.BY_ITEM -> {
                for (item in bill.items) {
                    val sharerIndices = item.sharerIds
                        .distinct()
                        .mapNotNull { id -> bill.people.indexOfFirst { it.id == id } }
                    if (sharerIndices.isEmpty()) continue
                    val shares = splitEven(item.priceCents, sharerIndices.size)
                    sharerIndices.forEachIndexed { offset, personIndex ->
                        val amount = shares[offset]
                        itemCentsByPerson[personIndex] += amount
                        linesByPerson[personIndex].add(
                            ItemAllocation(itemId = item.id, name = item.name, amountCents = amount),
                        )
                    }
                }
            }
        }

        val taxCents = when (bill.taxKind) {
            TaxKind.NONE -> 0L
            TaxKind.FIXED -> bill.taxCents.also {
                require(it >= 0) { "Pajak tidak boleh negatif." }
            }
            TaxKind.PERCENT -> {
                require(bill.taxPercentBps > 0) { "Persen pajak harus lebih dari nol." }
                BigDecimal.valueOf(totalItemsCents)
                    .multiply(BigDecimal.valueOf(bill.taxPercentBps.toLong()))
                    .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
                    .toLong()
            }
        }

        require(bill.otherCostsCents >= 0) { "Biaya lain tidak boleh negatif." }

        val taxByPerson = when (bill.mode) {
            SplitMode.EVEN -> splitEven(taxCents, count)
            SplitMode.BY_ITEM -> allocateProportional(taxCents, itemCentsByPerson)
        }


        val otherByPerson = splitEven(bill.otherCostsCents, count)


        val preDiscountTotal = totalItemsCents + taxCents + bill.otherCostsCents


        val discountCents = when (bill.discountKind) {
            DiscountKind.NONE -> 0L
            DiscountKind.PERCENT -> {
                require(bill.discountPercentBps in 0..10_000) {
                    "Persen diskon harus antara 0 dan 100."
                }
                BigDecimal.valueOf(preDiscountTotal)
                    .multiply(BigDecimal.valueOf(bill.discountPercentBps.toLong()))
                    .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
                    .toLong()
            }
            DiscountKind.FIXED -> bill.discountCents.also {
                require(it >= 0) { "Diskon nominal tidak boleh negatif." }
            }
        }

        require(discountCents <= preDiscountTotal) {
            "Diskon (${Money.format(discountCents)}) melebihi subtotal bill (${Money.format(preDiscountTotal)})."
        }





        val preDiscountByPerson = (0 until count).map {
            itemCentsByPerson[it] + taxByPerson[it] + otherByPerson[it]
        }
        val discountByPerson = when (bill.mode) {
            SplitMode.EVEN -> splitEven(discountCents, count)
            SplitMode.BY_ITEM -> allocateProportional(discountCents, preDiscountByPerson)
        }

        val grandTotalCents = totalItemsCents + taxCents + bill.otherCostsCents - discountCents

        val results = bill.people.mapIndexed { index, person ->
            PersonResult(
                personId = person.id,
                name = person.name,
                itemLines = linesByPerson[index].toList(),
                itemCents = itemCentsByPerson[index],
                taxCents = taxByPerson[index],
                otherCostsCents = otherByPerson[index],
                discountCents = discountByPerson[index],
                amountCents = itemCentsByPerson[index] + taxByPerson[index] + otherByPerson[index] - discountByPerson[index],
            )
        }

        return SplitResult(
            totalItemsCents = totalItemsCents,
            taxCents = taxCents,
            otherCostsCents = bill.otherCostsCents,
            discountCents = discountCents,
            grandTotalCents = grandTotalCents,
            people = results,
        )
    }


    private fun splitEven(totalCents: Long, count: Int): List<Long> {
        require(count > 0) { "Jumlah orang harus lebih dari nol." }
        if (totalCents <= 0L) return List(count) { 0L }
        val base = totalCents / count
        val remainder = (totalCents % count).toInt()
        return List(count) { index -> if (index < remainder) base + 1 else base }
    }


    private fun allocateProportional(totalCents: Long, weights: List<Long>): List<Long> {
        val count = weights.size
        if (totalCents <= 0L) return List(count) { 0L }

        val totalWeight = weights.sum()
        if (totalWeight <= 0L) return splitEven(totalCents, count)

        val result = MutableList(count) { 0L }
        val remainders = MutableList(count) { BigDecimal.ZERO }
        var assigned = 0L

        for (index in 0 until count) {
            val product = BigDecimal.valueOf(totalCents).multiply(BigDecimal.valueOf(weights[index]))
            val quotient = product.divide(BigDecimal.valueOf(totalWeight), 0, RoundingMode.FLOOR)
            result[index] = quotient.toLong()
            assigned += result[index]
            remainders[index] = product.remainder(BigDecimal.valueOf(totalWeight))
        }

        var leftover = totalCents - assigned
        val order = (0 until count).sortedWith(
            compareByDescending<Int> { remainders[it] }.thenBy { it },
        )
        for (index in order) {
            if (leftover <= 0L) break
            result[index] += 1L
            leftover -= 1L
        }
        return result
    }
}
