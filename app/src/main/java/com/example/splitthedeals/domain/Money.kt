package com.example.splitthedeals.domain

import java.math.BigDecimal
import java.math.RoundingMode


object Money {

    private val HUNDRED = BigDecimal(100)


    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .replace("IDR", "", ignoreCase = true)
            .replace("Rp", "", ignoreCase = true)
            .replace("A$", "", ignoreCase = true)
            .replace("$", "")
            .replace(" ", "")
            .replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned)
            if (value.signum() < 0) return null
            value.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }


    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        val whole = abs / 100
        val frac = abs % 100
        if (abs == 0L) return "Rp"
        val wholeStr = if (whole == 0L && frac != 0L) "0" else whole.toString().reversed().chunked(3).joinToString(".").reversed()
        val fracStr = if (frac != 0L) ",${frac.toString().trimEnd('0')}" else ""
        return sign + "Rp" + wholeStr + fracStr
    }


    fun formatInput(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = if (cents < 0) -cents else cents
        return sign + (abs / 100).toString() + "." + (abs % 100).toString().padStart(2, '0')
    }


    fun formatPercentInput(bps: Int): String {
        return BigDecimal(bps).divide(BigDecimal(100)).stripTrailingZeros().toPlainString()
    }


    fun parsePercent(input: String): Int? {
        val cleaned = input.trim()
            .replace("%", "")
            .replace(" ", "")
            .replace(",", ".")
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned)
            if (value.signum() < 0) return null
            value.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).toInt()
        } catch (e: NumberFormatException) {
            null
        }
    }


    fun formatPercent(bps: Int): String {
        val whole = bps / 100
        val frac = bps % 100
        return if (frac == 0) "$whole%" else "$whole.${frac.toString().padStart(2, '0')}%"
    }
}
