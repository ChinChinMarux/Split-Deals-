package com.example.splitthedeals.domain

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class SplitCalculatorTest {

    private fun person(name: String) = PersonInput(UUID.randomUUID().toString(), name)

    private fun item(name: String, priceCents: Long, sharers: List<PersonInput>) =
        ItemInput(UUID.randomUUID().toString(), name, priceCents, sharers.map { it.id })

    private fun bill(
        people: List<PersonInput>,
        items: List<ItemInput>,
        mode: SplitMode,
        taxKind: TaxKind = TaxKind.NONE,
        taxPercentBps: Int = 0,
        taxCents: Long = 0L,
        otherCostsCents: Long = 0L,
        discountKind: DiscountKind = DiscountKind.NONE,
        discountPercentBps: Int = 0,
        discountCents: Long = 0L,
    ) = BillInput(
        id = UUID.randomUUID().toString(),
        title = "Test",
        people = people,
        items = items,
        mode = mode,
        taxKind = taxKind,
        taxPercentBps = taxPercentBps,
        taxCents = taxCents,
        otherCostsCents = otherCostsCents,
        discountKind = discountKind,
        discountPercentBps = discountPercentBps,
        discountCents = discountCents,
    )

    @Test
    fun contoh1_split_rata_tiga_orang_dengan_tip() {
        val a = person("A")
        val b = person("B")
        val c = person("C")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b, c),
                items = listOf(item("Makan", 6_000L, listOf(a, b, c))),
                mode = SplitMode.EVEN,
                taxKind = TaxKind.PERCENT,
                taxPercentBps = 1_000,
            ),
        )

        assertEquals(6_000L, result.totalItemsCents)
        assertEquals(600L, result.taxCents)
        assertEquals(0L, result.discountCents)
        assertEquals(6_600L, result.grandTotalCents)
        assertEquals(listOf(2_200L, 2_200L, 2_200L), result.people.map { it.amountCents })
        assertEquals(6_600L, result.people.sumOf { it.amountCents })
    }

    @Test
    fun contoh2_per_item_dengan_item_ditengah() {
        val a = person("A")
        val b = person("B")
        val c = person("C")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b, c),
                items = listOf(
                    item("Pizza", 1_800L, listOf(a, b, c)),
                    item("Burger", 1_250L, listOf(a)),
                    item("Salad", 800L, listOf(b)),
                    item("Drink", 700L, listOf(c)),
                ),
                mode = SplitMode.BY_ITEM,
            ),
        )

        assertEquals(4_550L, result.totalItemsCents)
        assertEquals(0L, result.taxCents)
        assertEquals(0L, result.discountCents)
        assertEquals(4_550L, result.grandTotalCents)
        assertEquals(listOf(1_850L, 1_400L, 1_300L), result.people.map { it.amountCents })
        assertEquals(4_550L, result.people.sumOf { it.amountCents })

        assertEquals(listOf(600L, 1_250L), result.people[0].itemLines.map { it.amountCents })
        assertEquals("Pizza", result.people[0].itemLines[0].name)
        assertEquals("Burger", result.people[0].itemLines[1].name)
        assertEquals(listOf(600L, 800L), result.people[1].itemLines.map { it.amountCents })
        assertEquals(listOf(600L, 700L), result.people[2].itemLines.map { it.amountCents })
    }

    @Test
    fun contoh3_sisa_sen_dan_tip_persen() {
        val a = person("A")
        val b = person("B")
        val c = person("C")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b, c),
                items = listOf(item("Makan", 2_002L, listOf(a, b, c))),
                mode = SplitMode.EVEN,
                taxKind = TaxKind.PERCENT,
                taxPercentBps = 1_500,
            ),
        )

        assertEquals(2_002L, result.totalItemsCents)
        assertEquals(300L, result.taxCents)
        assertEquals(0L, result.discountCents)
        assertEquals(2_302L, result.grandTotalCents)
        assertEquals(listOf(768L, 767L, 767L), result.people.map { it.amountCents })
        assertEquals(2_302L, result.people.sumOf { it.amountCents })
    }

    @Test
    fun diskon_nominal_mengurangi_grand_total_dan_dibagi_rata() {
        val a = person("A")
        val b = person("B")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b),
                items = listOf(item("Makan", 10_000L, listOf(a, b))),
                mode = SplitMode.EVEN,
                discountKind = DiscountKind.FIXED,
                discountCents = 1_000L,
            ),
        )

        assertEquals(10_000L, result.totalItemsCents)
        assertEquals(1_000L, result.discountCents)
        assertEquals(9_000L, result.grandTotalCents)
        assertEquals(9_000L, result.people.sumOf { it.amountCents })
        assertEquals(listOf(500L, 500L), result.people.map { it.discountCents })
    }

    @Test
    fun diskon_persen_dihitung_dari_item_plus_pajak_plus_biaya_lain() {
        val a = person("A")
        val b = person("B")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b),
                items = listOf(item("Makan", 10_000L, listOf(a, b))),
                mode = SplitMode.EVEN,
                taxKind = TaxKind.PERCENT,
                taxPercentBps = 1_000,
                otherCostsCents = 1_000L,
                discountKind = DiscountKind.PERCENT,
                discountPercentBps = 1_000,
            ),
        )

        assertEquals(1_200L, result.discountCents)
        assertEquals(10_800L, result.grandTotalCents)
        assertEquals(10_800L, result.people.sumOf { it.amountCents })
    }

    @Test
    fun diskon_proporsional_by_item_mengikuti_subtotal_item() {
        val a = person("A")
        val b = person("B")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b),
                items = listOf(
                    item("Pizza", 1_000L, listOf(a)),
                    item("Burger", 3_000L, listOf(b)),
                ),
                mode = SplitMode.BY_ITEM,
                discountKind = DiscountKind.FIXED,
                discountCents = 800L,
            ),
        )


        assertEquals(listOf(200L, 600L), result.people.map { it.discountCents })
        assertEquals(800L, result.people.sumOf { it.discountCents })
        assertEquals(3_200L, result.grandTotalCents)
        assertEquals(3_200L, result.people.sumOf { it.amountCents })
    }

    @Test
    fun diskon_melebihi_total_ditolak() {
        val a = person("A")
        val b = person("B")
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(
                    people = listOf(a, b),
                    items = listOf(item("Makan", 1_000L, listOf(a, b))),
                    mode = SplitMode.EVEN,
                    discountKind = DiscountKind.FIXED,
                    discountCents = 1_001L,
                ),
            )
        }
    }

    @Test
    fun diskon_persen_diluar_rentang_ditolak() {
        val a = person("A")
        val b = person("B")
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(
                    people = listOf(a, b),
                    items = listOf(item("Makan", 1_000L, listOf(a, b))),
                    mode = SplitMode.EVEN,
                    discountKind = DiscountKind.PERCENT,
                    discountPercentBps = 10_001,
                ),
            )
        }
    }

    @Test
    fun diskon_nominal_negatif_ditolak() {
        val a = person("A")
        val b = person("B")
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(
                    people = listOf(a, b),
                    items = listOf(item("Makan", 1_000L, listOf(a, b))),
                    mode = SplitMode.EVEN,
                    discountKind = DiscountKind.FIXED,
                    discountCents = -1L,
                ),
            )
        }
    }

    @Test
    fun invarian_jumlah_bagian_selalu_sama_dengan_total() {
        val modeList = listOf(SplitMode.EVEN, SplitMode.BY_ITEM)
        val taxList = listOf(
            Triple(TaxKind.NONE, 0, 0L),
            Triple(TaxKind.PERCENT, 1_000, 0L),
            Triple(TaxKind.PERCENT, 1_500, 0L),
            Triple(TaxKind.PERCENT, 1_234, 0L),
            Triple(TaxKind.FIXED, 0, 1L),
            Triple(TaxKind.FIXED, 0, 999L),
        )
        val discountList = listOf(
            Triple(DiscountKind.NONE, 0, 0L),
            Triple(DiscountKind.PERCENT, 500, 0L),
            Triple(DiscountKind.PERCENT, 1_000, 0L),
            Triple(DiscountKind.FIXED, 0, 1L),
            Triple(DiscountKind.FIXED, 0, 999L),
        )
        val totals = listOf(1L, 99L, 100L, 101L, 999L, 1_000L, 2_002L, 123_456L)
        val otherCostsList = listOf(0L, 1L, 999L)

        for (count in 2..8) {
            val people = (1..count).map { person("P$it") }
            for (total in totals) {
                for (mode in modeList) {
                    for ((taxKind, bps, fixed) in taxList) {
                        for (otherCosts in otherCostsList) {

                            val taxForCombo = when (taxKind) {
                                TaxKind.NONE -> 0L
                                TaxKind.PERCENT -> BigDecimal.valueOf(total)
                                    .multiply(BigDecimal.valueOf(bps.toLong()))
                                    .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
                                    .toLong()
                                TaxKind.FIXED -> fixed
                            }
                            val preDiscount = total + taxForCombo + otherCosts
                            for ((discountKind, dbps, dnominal) in discountList) {


                                if (discountKind == DiscountKind.FIXED && dnominal > preDiscount) continue
                                val currentBill = bill(
                                    people = people,
                                    items = listOf(item("Item", total, people)),
                                    mode = mode,
                                    taxKind = taxKind,
                                    taxPercentBps = bps,
                                    taxCents = fixed,
                                    otherCostsCents = otherCosts,
                                    discountKind = discountKind,
                                    discountPercentBps = dbps,
                                    discountCents = dnominal,
                                )
                                val result = SplitCalculator.calculate(currentBill)

                                assertEquals(
                                    "itemCents mismatch count=$count total=$total mode=$mode tax=$taxKind disc=$discountKind",
                                    result.totalItemsCents,
                                    result.people.sumOf { it.itemCents },
                                )
                                assertEquals(
                                    "taxCents mismatch count=$count total=$total mode=$mode tax=$taxKind disc=$discountKind",
                                    result.taxCents,
                                    result.people.sumOf { it.taxCents },
                                )
                                assertEquals(
                                    "otherCosts mismatch count=$count total=$total mode=$mode tax=$taxKind disc=$discountKind",
                                    result.otherCostsCents,
                                    result.people.sumOf { it.otherCostsCents },
                                )
                                assertEquals(
                                    "discountCents mismatch count=$count total=$total mode=$mode tax=$taxKind disc=$discountKind",
                                    result.discountCents,
                                    result.people.sumOf { it.discountCents },
                                )
                                assertEquals(
                                    "amountCents mismatch count=$count total=$total mode=$mode tax=$taxKind disc=$discountKind",
                                    result.grandTotalCents,
                                    result.people.sumOf { it.amountCents },
                                )
                                assertTrue(result.people.all { it.amountCents >= 0L })
                                assertTrue(result.discountCents <= result.totalItemsCents + result.taxCents + result.otherCostsCents)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun item_sepuluh_dolar_untuk_tiga_orang_sisa_satu_sen() {
        val a = person("A")
        val b = person("B")
        val c = person("C")
        val result = SplitCalculator.calculate(
            bill(
                people = listOf(a, b, c),
                items = listOf(item("Item", 1_000L, listOf(a, b, c))),
                mode = SplitMode.BY_ITEM,
            ),
        )

        assertEquals(listOf(334L, 333L, 333L), result.people.map { it.itemCents })
        assertEquals(1_000L, result.people.sumOf { it.itemCents })
    }

    @Test
    fun validasi_menolak_bill_tidak_valid() {
        val a = person("A")
        val b = person("B")

        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(bill(listOf(a), listOf(item("X", 100L, listOf(a))), SplitMode.EVEN))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(bill(listOf(a, b), emptyList(), SplitMode.EVEN))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(bill(listOf(a, b), listOf(item("X", 0L, listOf(a, b))), SplitMode.EVEN))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(listOf(a, b), listOf(item("X", 100L, emptyList())), SplitMode.BY_ITEM),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(
                    listOf(a, b),
                    listOf(item("X", 100L, listOf(a, b))),
                    SplitMode.EVEN,
                    taxKind = TaxKind.PERCENT,
                    taxPercentBps = 0,
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculate(
                bill(
                    listOf(a, b),
                    listOf(item("X", 100L, listOf(a, b))),
                    SplitMode.EVEN,
                    otherCostsCents = -1L,
                ),
            )
        }
    }

    @Test
    fun money_parse_dan_format() {
        assertEquals(1_250L, Money.parse("12.50"))
        assertEquals(1_250L, Money.parse("$12.5"))
        assertEquals(1_250L, Money.parse("Rp12.50"))
        assertEquals(1_000L, Money.parse("10"))
        assertEquals(1L, Money.parse("0.005"))
        assertEquals(null, Money.parse(""))
        assertEquals(null, Money.parse("abc"))
        assertEquals(null, Money.parse("-1"))

        assertEquals("Rp", Money.format(0L))
        assertEquals("Rp0,5", Money.format(5L))
        assertEquals("Rp12,5", Money.format(1_250L))
        assertEquals("Rp1.234,56", Money.format(123_456L))

        assertEquals(1_000, Money.parsePercent("10"))
        assertEquals(1_250, Money.parsePercent("12.5"))
        assertEquals("10%", Money.formatPercent(1_000))
        assertEquals("12.50%", Money.formatPercent(1_250))
    }
}
