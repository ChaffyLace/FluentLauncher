package com.example.fluentui.ui.search

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.fluentui.ui.components.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FileSearchStrategy leverages the Android MediaStore API to perform
 * high-performance indexed file searching.
 * * Engineering Note: We query the external content URI to find documents,
 * downloads, and media without manual disk crawling, preserving battery life.
 */
class FileSearchStrategy : SearchStrategy {
    override val priority = 70

    override suspend fun search(
        context: Context,
        query: String,
        apps: List<AppModel>
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.length < 3) return@withContext emptyList()

        val results = mutableListOf<SearchResult>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE
        )

        // Search in Files, Downloads and Media
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val queryUri = MediaStore.Files.getContentUri("external")

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                var count = 0
                while (cursor.moveToNext() && count < 5) { // Limit to top 5 file results
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val mimeType = cursor.getString(mimeColumn) ?: "application/octet-stream"
                    val size = cursor.getLong(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(queryUri, id)

                    results.add(
                        SearchResult(
                            id = contentUri.toString(),
                            title = name,
                            subtitle = formatFileSize(size),
                            type = ResultType.FILE,
                            iconData = contentUri, // Used for thumbnail loading
                            intentData = contentUri,
                            score = FuzzyMatcher.score(name, query),
                            strategyPriority = priority
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Log as a system-level warning
        }
        return@withContext results
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}