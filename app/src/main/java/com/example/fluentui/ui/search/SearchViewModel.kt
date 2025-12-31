package com.example.fluentui.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    application: Application,
    private val repository: SearchRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var cachedApps: List<AppModel> = emptyList()

    init {
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

    fun updateApps(apps: List<AppModel>) {
        this.cachedApps = apps
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isLoading = true) }
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
                        subtitle = "Google",
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

            // CORRECTION ICI : Ajout du mot-clé 'suspend'
            val rateProvider = object : CurrencyRateProvider {
                private val rates = mapOf(
                    "usd" to 1.0, "eur" to 0.92, "gbp" to 0.79,
                    "jpy" to 150.0, "cad" to 1.35
                )

                override suspend fun getRate(from: String, to: String): Double? {
                    val fromRate = rates[from.lowercase()]
                    val toRate = rates[to.lowercase()]
                    return if (fromRate != null && toRate != null) toRate / fromRate else null
                }
            }

            val cache = SearchCache()

            val strategies = listOf(
                MathStrategy(calculator),
                ConverterStrategy(rateProvider),
                AppStrategy(localization),
                SettingsStrategy(localization),
                ContactStrategy()
            )

            val repo = SearchRepository(strategies, cache)
            return SearchViewModel(app, repo) as T
        }
    }
}