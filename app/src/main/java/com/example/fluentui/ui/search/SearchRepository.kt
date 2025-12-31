package com.example.fluentui.ui.search

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

// ==============================================================================
// SEARCH CACHE
// ==============================================================================

class SearchCache {
    private val cache = LruCache<String, List<SearchResult>>(50)

    fun get(query: String): List<SearchResult>? {
        return cache.get(query)
    }

    fun put(query: String, results: List<SearchResult>) {
        if (results.isNotEmpty()) {
            cache.put(query, results)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}

// ==============================================================================
// STUB CURRENCY PROVIDER
// ==============================================================================

class StubCurrencyProvider : CurrencyRateProvider {
    private val rates = mapOf(
        "usd" to 1.0,
        "eur" to 0.92,
        "gbp" to 0.79,
        "jpy" to 148.0,
        "btc" to 0.00002,
        "chf" to 0.88,
        "cad" to 1.35,
        "aud" to 1.52
    )

    override suspend fun getRate(from: String, to: String): Double? {
        val fromRate = rates[from.lowercase()]
        val toRate = rates[to.lowercase()]

        if (fromRate != null && toRate != null) {
            return toRate / fromRate
        }
        return null
    }
}

// ==============================================================================
// SEARCH REPOSITORY
// ==============================================================================

class SearchRepository(
    private val strategies: List<SearchStrategy>,
    private val cache: SearchCache
) {
    suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        // 1. Check Cache (Fast Path)
        val cacheKey = "$trimmedQuery-${apps.size}"
        cache.get(cacheKey)?.let {
            Log.d("SearchRepo", "Cache hit for: $trimmedQuery")
            return@withContext it
        }

        // 2. Execute Strategies in Parallel
        val deferredResults = strategies.map { strategy ->
            async {
                try {
                    val results = strategy.search(context, trimmedQuery, apps)
                    // Inject strategy priority into results
                    results.map { it.copy(strategyPriority = strategy.priority) }
                } catch (e: Exception) {
                    Log.e("SearchRepo", "Strategy failed: ${strategy.javaClass.simpleName}", e)
                    emptyList()
                }
            }
        }

        // 3. Merge & Sort with Weighted Scoring
        // Final score = (text_score * 0.7) + (strategy_priority * 0.3)
        val finalResults = deferredResults.awaitAll()
            .flatten()
            .sortedWith(
                compareByDescending<SearchResult> {
                    (it.score * 0.7) + (it.strategyPriority.toDouble() * 0.3)
                }.thenBy { it.title }
            )
            .take(20)

        // 4. Update Cache
        cache.put(cacheKey, finalResults)

        Log.d("SearchRepo", "Found ${finalResults.size} results for: $trimmedQuery")
        return@withContext finalResults
    }
}