package com.example.fluentui.ui.search

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.fluentui.ui.components.AppModel
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

// ==============================================================================
// FUZZY MATCHER - LEVENSHTEIN DISTANCE
// ==============================================================================

object FuzzyMatcher {
    private val normalizer = TextNormalizer()

    fun score(target: String, query: String, aliases: List<String> = emptyList()): Double {
        val normTarget = normalizer.normalize(target)
        val normQuery = normalizer.normalize(query)

        // Exact match
        if (normTarget == normQuery) return 100.0

        // Starts with
        if (normTarget.startsWith(normQuery)) return 90.0

        // Word boundary match
        if (normTarget.contains(" $normQuery") || normTarget.contains("-$normQuery")) return 85.0

        // Alias matching
        for (alias in aliases) {
            val normAlias = normalizer.normalize(alias)
            if (normAlias == normQuery) return 95.0
            if (normAlias.startsWith(normQuery)) return 80.0
        }

        // Contains
        if (normTarget.contains(normQuery)) return 60.0

        // Fuzzy matching (Levenshtein)
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
                newCost[j] = min(
                    min(cost[j] + 1, newCost[j - 1] + 1),
                    cost[j - 1] + match
                )
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
    private val diacritics = java.util.regex.Pattern.compile(
        "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+"
    )

    fun normalize(text: String): String {
        val nfd = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return diacritics.matcher(nfd)
            .replaceAll("")
            .lowercase(java.util.Locale.getDefault())
            .trim()
    }
}

// ==============================================================================
// STRATEGY 1: MATH (Priorité 100)
// ==============================================================================

class MathStrategy(
    private val calculator: SmartCalculator = SmartCalculator()
) : SearchStrategy {
    override val priority = 100

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> {
        val result = calculator.evaluate(query) ?: return emptyList()

        return listOf(
            SearchResult(
                id = "calc",
                title = result,
                subtitle = "Résultat",
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
// STRATEGY 2: CONVERTER (Priorité 95)
// ==============================================================================

class ConverterStrategy(
    private val rateProvider: CurrencyRateProvider
) : SearchStrategy {
    override val priority = 95

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> {
        val regex = Regex("([0-9]+[.,]?[0-9]*)\\s*([a-zA-Z]+)\\s*(to|in|en|vers)?\\s*([a-zA-Z]+)")
        val match = regex.find(query.lowercase()) ?: return emptyList()

        val value = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return emptyList()
        val from = match.groupValues[2]
        val to = match.groupValues[4]

        // Temperature conversion
        if ((from == "c" || from == "celsius") && (to == "f" || to == "fahrenheit")) {
            val result = (value * 9/5) + 32
            return listOf(createResult("${DecimalFormat("#.#").format(result)} °F"))
        }
        if ((from == "f" || from == "fahrenheit") && (to == "c" || to == "celsius")) {
            val result = (value - 32) * 5/9
            return listOf(createResult("${DecimalFormat("#.#").format(result)} °C"))
        }

        // Currency conversion
        val rate = rateProvider.getRate(from, to)
        if (rate != null) {
            val result = value * rate
            return listOf(createResult("${DecimalFormat("#.##").format(result)} ${to.uppercase()}"))
        }

        return emptyList()
    }

    private fun createResult(text: String) = SearchResult(
        id = "conv",
        title = text,
        subtitle = "Conversion",
        type = ResultType.CONVERSION,
        iconData = null,
        intentData = text,
        score = 100.0,
        strategyPriority = priority
    )
}

// ==============================================================================
// STRATEGY 3: APPS (Priorité 90)
// ==============================================================================

class AppStrategy(
    private val localization: MultilingualProvider
) : SearchStrategy {
    override val priority = 90

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> {
        return apps.mapNotNull { app ->
            val aliases = localization.getAppAliases(app.packageName, app.label)
            val score = FuzzyMatcher.score(app.label, query, aliases)

            if (score > 40) {
                SearchResult(
                    id = app.packageName,
                    title = app.label,
                    subtitle = "Application",
                    type = ResultType.APP,
                    iconData = app.icon,
                    intentData = app.packageName,
                    score = score,
                    strategyPriority = priority
                )
            } else null
        }
    }
}

// ==============================================================================
// STRATEGY 4: SETTINGS (Priorité 85)
// ==============================================================================

class SettingsStrategy(
    private val localization: MultilingualProvider
) : SearchStrategy {
    override val priority = 85

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> {
        return localization.getSettingsAliases().mapNotNull { (action, keywords) ->
            val title = keywords.first().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault())
                else it.toString()
            }
            val score = FuzzyMatcher.score(title, query, keywords)

            if (score > 50) {
                SearchResult(
                    id = action,
                    title = title,
                    subtitle = "Paramètre système",
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
// STRATEGY 5: CONTACTS (Priorité 80)
// ==============================================================================

class ContactStrategy : SearchStrategy {
    override val priority = 80

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
                )
                val numIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val idIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )
                val photoIdx = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                )

                val seen = mutableSetOf<String>()
                while (cursor.moveToNext() && results.size < 4) {
                    val id = cursor.getString(idIdx)
                    if (id !in seen) {
                        val name = cursor.getString(nameIdx)
                        val score = FuzzyMatcher.score(name, query)

                        if (score > 50) {
                            seen.add(id)
                            val photoUri = cursor.getString(photoIdx)
                            results.add(
                                SearchResult(
                                    id = id,
                                    title = name,
                                    subtitle = cursor.getString(numIdx),
                                    type = ResultType.CONTACT,
                                    iconData = photoUri?.let { android.net.Uri.parse(it) },
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