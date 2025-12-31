package com.example.fluentui.ui.search

class SimpleCurrencyRateProvider {

    private val rates = mapOf(
        "usd" to 1.0,
        "eur" to 0.92,
        "gbp" to 0.79,
        "jpy" to 150.0,
        "cad" to 1.35
    )

    fun getRate(from: String, to: String): Double? {
        val fromRate = rates[from.lowercase()]
        val toRate = rates[to.lowercase()]

        return if (fromRate != null && toRate != null) {
            toRate / fromRate
        } else {
            null
        }
    }
}