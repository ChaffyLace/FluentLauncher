package com.example.fluentui.ui.search

import android.content.Context
import com.example.fluentui.ui.components.AppModel

// ==============================================================================
// DOMAIN MODELS - SEARCH SYSTEM
// ==============================================================================

enum class ResultType {
    APP,
    CONTACT,
    FILE,
    SETTING,
    WEB,
    CALCULATION,
    CONVERSION,
    ACTION
}

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: ResultType,
    val iconData: Any? = null,
    val intentData: Any,
    val score: Double = 0.0,
    val strategyPriority: Int = 0
)

// ==============================================================================
// STRATEGY PATTERN INTERFACE
// ==============================================================================

interface SearchStrategy {
    val priority: Int // 0 (Low) to 100 (High)
    suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult>
}

// ==============================================================================
// CURRENCY PROVIDER INTERFACE
// ==============================================================================

interface CurrencyRateProvider {
    suspend fun getRate(from: String, to: String): Double?
}