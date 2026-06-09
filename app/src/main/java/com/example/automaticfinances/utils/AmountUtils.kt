package com.example.automaticfinances.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a cent amount (COP) as a short peso string for display, e.g. 1234500 -> "$1.234.500".
 * Cents are dropped — Colombian pesos are effectively whole-number in everyday use.
 */
fun Long.centsToCopString(): String {
    val nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
        maximumFractionDigits = 0
    }
    return nf.format(this / 100.0)
}

/**
 * Parses a Colombian peso string to cents.
 * Supports formats: "39.500,00", "39,500.00", "50.000", "50,000"
 */
fun String.parseColombiaCents(): Long? {
    val raw = this.filter { it.isDigit() || it == '.' || it == ',' }
    if (raw.isEmpty()) return null

    val hasDot = raw.contains('.')
    val hasComma = raw.contains(',')

    if (!hasDot && !hasComma) {
        return raw.toLongOrNull()?.times(100)
    }

    val lastSepIdx = raw.indexOfLast { it == '.' || it == ',' }
    val rightDigits = raw.substring(lastSepIdx + 1).count(Char::isDigit)
    val both = hasDot && hasComma

    val isDecimalSeparator = when {
        both -> true
        rightDigits in 1..2 -> true
        else -> false
    }

    return if (isDecimalSeparator) {
        val intPart = raw.substring(0, lastSepIdx).filter(Char::isDigit).ifEmpty { "0" }
        val frac = raw.substring(lastSepIdx + 1).filter(Char::isDigit).padEnd(2, '0').take(2)
        (intPart + frac).toLongOrNull()
    } else {
        raw.filter(Char::isDigit).toLongOrNull()?.times(100)
    }
}
