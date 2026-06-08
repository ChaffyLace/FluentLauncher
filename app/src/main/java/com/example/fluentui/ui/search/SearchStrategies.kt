/**
 * @file SearchStrategies.kt
 * @description Core search logic including Fuzzy Matching (Levenshtein),
 * Text Normalization, and specialized search strategies (Math, Apps, Settings, Contacts).
 * Engineering Note: All user-facing strings are extracted to resources for production.
 */

package com.example.fluentui.ui.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.fluentui.R
import com.example.fluentui.ui.components.AppModel
import java.text.DecimalFormat
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

// ==============================================================================
// FUZZY MATCHER - LEVENSHTEIN DISTANCE
// ==============================================================================

object FuzzyMatcher {
    private val normalizer = TextNormalizer()

    /**
     * Calculates a similarity score (0-100) between target and query.
     */
    fun score(target: String, query: String, aliases: List<String> = emptyList()): Double {
        val normTarget = normalizer.normalize(target)
        val normQuery = normalizer.normalize(query)

        if (normTarget == normQuery) return 100.0
        if (normTarget.startsWith(normQuery)) return 90.0
        if (normTarget.contains(" $normQuery") || normTarget.contains("-$normQuery")) return 85.0

        for (alias in aliases) {
            val normAlias = normalizer.normalize(alias)
            if (normAlias == normQuery) return 95.0
            if (normAlias.startsWith(normQuery)) return 80.0
        }

        if (normTarget.contains(normQuery)) return 60.0

        if (normQuery.length > 2) {
            val dist = levenshtein(normTarget, normQuery)
            val maxLen = max(normTarget.length, normQuery.length)
            val similarity = 1.0 - (dist.toDouble() / maxLen)
            if (similarity > 0.65) return similarity * 70.0
        }

        return 0.0
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1)

        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = min(min(cost[j] + 1, newCost[j - 1] + 1), cost[j - 1] + match)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }
}

// ==============================================================================
// TEXT NORMALIZER
// ==============================================================================

class TextNormalizer {
    private val diacritics = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+")

    fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return diacritics.matcher(nfd)
            .replaceAll("")
            .lowercase(Locale.getDefault())
            .trim()
    }
}

// ==============================================================================
// STRATEGY 1: MATH
// ==============================================================================

class MathStrategy(private val calculator: SmartCalculator = SmartCalculator()) : SearchStrategy {
    override val priority = 100

    override suspend fun search(context: Context, query: String, apps: List<AppModel>): List<SearchResult> {
        val result = calculator.evaluate(query) ?: return emptyList()

        return listOf(
            SearchResult(
                id = "calc",
                title = result,
                subtitle = context.getString(R.string.search_type_calc),
                type = ResultType.CALCULATION,
                iconData = null,
                intentData = result,
                score = 100.0,
                strategyPriority = priority
            )
        )
    }
}

// ==============================================================================
// STRATEGY 2: CONVERTER
// ==============================================================================

class ConverterStrategy(private val rateProvider: CurrencyRateProvider) : SearchStrategy {
    override val priority = 95

    override suspend fun search(context: Context, query: String, apps: List<AppModel>): List<SearchResult> {
        val regex = Regex("([0-9]+[.,]?[0-9]*)\\s*([a-zA-Z]+)\\s*(to|in|en|vers)?\\s*([a-zA-Z]+)")
        val match = regex.find(query.lowercase()) ?: return emptyList()

        val value = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return emptyList()
        val from = match.groupValues[2]
        val to = match.groupValues[4]

        // Temp
        if ((from == "c" || from == "celsius") && (to == "f" || to == "fahrenheit")) {
            return listOf(createResult(context, "${DecimalFormat("#.#").format((value * 9/5) + 32)} °F"))
        }
        if ((from == "f" || from == "fahrenheit") && (to == "c" || to == "celsius")) {
            return listOf(createResult(context, "${DecimalFormat("#.#").format((value - 32) * 5/9)} °C"))
        }

        // Currency
        rateProvider.getRate(from, to)?.let { rate ->
            return listOf(createResult(context, "${DecimalFormat("#.##").format(value * rate)} ${to.uppercase()}"))
        }

        return emptyList()
    }

    private fun createResult(context: Context, text: String) = SearchResult(
        id = "conv",
        title = text,
        subtitle = context.getString(R.string.search_type_conv),
        type = ResultType.CONVERSION,
        iconData = null,
        intentData = text,
        score = 100.0,
        strategyPriority = priority
    )
}

// ==============================================================================
// STRATEGY 3: APPS
// ==============================================================================

class AppStrategy(private val localization: MultilingualProvider) : SearchStrategy {
    override val priority = 90

    override suspend fun search(context: Context, query: String, apps: List<AppModel>): List<SearchResult> {
        return apps.mapNotNull { app ->
            val aliases = localization.getAppAliases(app.packageName, app.label)
            val score = FuzzyMatcher.score(app.label, query, aliases)

            if (score > 40) {
                SearchResult(
                    id = app.packageName,
                    title = app.label,
                    subtitle = context.getString(R.string.search_type_app),
                    type = ResultType.APP,
                    iconData = null,
                    intentData = app.packageName,
                    score = score,
                    strategyPriority = priority
                )
            } else null
        }
    }
}

// ==============================================================================
// STRATEGY 4: SETTINGS
// ==============================================================================

class SettingsStrategy(private val localization: MultilingualProvider) : SearchStrategy {
    override val priority = 85

    override suspend fun search(context: Context, query: String, apps: List<AppModel>): List<SearchResult> {
        return localization.getSettingsAliases().mapNotNull { (action, keywords) ->
            val title = keywords.first().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val score = FuzzyMatcher.score(title, query, keywords)

            if (score > 50) {
                SearchResult(
                    id = action,
                    title = title,
                    subtitle = context.getString(R.string.search_type_setting),
                    type = ResultType.SETTING,
                    iconData = null,
                    intentData = Intent(action),
                    score = score,
                    strategyPriority = priority
                )
            } else null
        }
    }
}

// ==============================================================================
// STRATEGY 5: CONTACTS
// ==============================================================================

class ContactStrategy : SearchStrategy {
    override val priority = 80

    override suspend fun search(context: Context, query: String, apps: List<AppModel>): List<SearchResult> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        try {
            // Optimisation : Utilisation du paramètre de filtrage natif d'Android pour plus de rapidité
            val selectionUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query))

            context.contentResolver.query(selectionUri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                val seen = mutableSetOf<String>()
                while (cursor.moveToNext() && results.size < 4) {
                    val id = cursor.getString(idIdx)
                    if (id !in seen) {
                        val name = cursor.getString(nameIdx)
                        val score = FuzzyMatcher.score(name, query)

                        if (score > 50) {
                            seen.add(id)
                            results.add(
                                SearchResult(
                                    id = id,
                                    title = name,
                                    subtitle = cursor.getString(numIdx),
                                    type = ResultType.CONTACT,
                                    iconData = cursor.getString(photoIdx)?.let { android.net.Uri.parse(it) },
                                    intentData = id,
                                    score = score,
                                    strategyPriority = priority
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }
}