package com.raisoft.grabi.domain

object CompactNumber {

    fun format(value: Long): String = when {
        value >= 1_000_000 -> scaled(value / 1_000_000.0) + "M"
        value >= 1_000 -> scaled(value / 1_000.0) + "K"
        else -> value.toString()
    }

    private fun scaled(value: Double): String =
        if (value < 10 && value % 1.0 != 0.0) {
            String.format(java.util.Locale.US, "%.1f", value)
        } else {
            value.toLong().toString()
        }
}