package com.example.fluentui.ui.search

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI State for the Search Screen.
 * Professional Note: Now includes dynamic recent searches and actual top apps.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val topApps: List<AppModel> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ==============================================================================
 * SEARCH HISTORY & TELEMETRY MANAGER
 * ==============================================================================
 * Engineering Note: Handles local persistence for user habits (Queries & App usage).
 * Operates purely on SharedPreferences to avoid the overhead of a Room Database
 * for simple key-value tracking.
 */
class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)

    // Limit the number of recent searches to prevent UI clutter
    private val MAX_RECENT_SEARCHES = 10

    fun getRecentSearches(): List<String> {
        val history = prefs.getString("recent_queries", "") ?: ""
        return if (history.isEmpty()) emptyList() else history.split("|||")
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return

        val currentList = getRecentSearches().toMutableList()
        currentList.remove(trimmed) // Remove if exists to push it to the top
        currentList.add(0, trimmed) // Add to front

        val newList = currentList.take(MAX_RECENT_SEARCHES).joinToString("|||")
        prefs.edit().putString("recent_queries", newList).apply()
    }

    fun trackAppLaunch(packageName: String) {
        val currentCount = prefs.getInt("app_usage_$packageName", 0)
        prefs.edit().putInt("app_usage_$packageName", currentCount + 1).apply()
    }

    fun getTopApps(allApps: List<AppModel>, limit: Int = 8): List<AppModel> {
        return allApps.sortedByDescending { app ->
            prefs.getInt("app_usage_${app.packageName}", 0)
        }.take(limit)
    }
}

/**
 * SearchViewModel handles the business logic for the Mica Search System.
 */
@OptIn(FlowPreview::class)
class SearchViewModel(
    application: Application,
    private val repository: SearchRepository,
    private val historyManager: SearchHistoryManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var cachedApps: List<AppModel> = emptyList()

    init {
        // Load initial data (Zero-State)
        loadZeroStateData()

        viewModelScope.launch {
            _uiState
                .map { it.query }
                .distinctUntilChanged()
                .debounce(300L)
                .collect { query ->
                    if (query.length >= 2) {
                        performSearch(query)
                    } else {
                        _uiState.update {
                            it.copy(results = emptyList(), isLoading = false)
                        }
                    }
                }
        }
    }

    private fun loadZeroStateData() {
        viewModelScope.launch(Dispatchers.IO) {
            val recents = historyManager.getRecentSearches()
            val top = historyManager.getTopApps(cachedApps)
            _uiState.update { it.copy(recentSearches = recents, topApps = top) }
        }
    }

    fun updateApps(apps: List<AppModel>) {
        this.cachedApps = apps
        // Refresh top apps now that we have the full list
        loadZeroStateData()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isLoading = true) }
    }

    /**
     * Registers an app launch to update the "Top Apps" ranking.
     * Also saves the current query if it led to an app launch.
     */
    fun onAppLaunched(packageName: String, currentQuery: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.trackAppLaunch(packageName)
            if (currentQuery.isNotEmpty()) {
                historyManager.addRecentSearch(currentQuery)
            }
            loadZeroStateData() // Refresh UI state silently
        }
    }

    /**
     * Registers a non-app action (like opening a file or setting) to save the query.
     */
    fun onActionExecuted(currentQuery: String) {
        if (currentQuery.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                historyManager.addRecentSearch(currentQuery)
                loadZeroStateData()
            }
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                val results = repository.search(
                    context = getApplication(),
                    query = query,
                    apps = cachedApps
                )

                val hasPriorityResult = results.any { it.strategyPriority >= 95 }
                val finalResults = if (!hasPriorityResult) {
                    results + SearchResult(
                        id = "web",
                        title = "Rechercher \"$query\"",
                        subtitle = "Google Search",
                        type = ResultType.WEB,
                        iconData = null,
                        intentData = query,
                        score = 5.0,
                        strategyPriority = 0
                    )
                } else results

                _uiState.update {
                    it.copy(results = finalResults, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(results = emptyList(), isLoading = false)
                }
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val localization = MultilingualProvider(app)
            val calculator = SmartCalculator()
            val rateProvider = StubCurrencyProvider()
            val cache = SearchCache()
            val historyManager = SearchHistoryManager(app)

            val strategies = listOf(
                MathStrategy(calculator),
                ConverterStrategy(rateProvider),
                AppStrategy(localization),
                SettingsStrategy(localization),
                ContactStrategy(),
                FileSearchStrategy()
            )

            val repo = SearchRepository(strategies, cache)

            return SearchViewModel(app, repo, historyManager) as T
        }
    }
}