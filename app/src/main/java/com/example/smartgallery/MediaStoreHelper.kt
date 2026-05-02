package com.example.smartgallery

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryImage(
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val bucketId: Long,
    val bucketName: String,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "image/jpeg"
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(dateAdded * 1000))
        }

    val formattedSize: String
        get() {
            return when {
                size >= 1_000_000 -> "${size / 1_000_000}MB"
                size >= 1_000 -> "${size / 1_000}KB"
                else -> "${size}B"
            }
        }

    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1f
}

data class GalleryAlbum(
    val bucketId: Long,
    val name: String,
    val coverImage: GalleryImage,
    val photoCount: Int
)

fun getAllImages(context: Context): List<GalleryImage> {
    val imageList = mutableListOf<GalleryImage>()

    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.MIME_TYPE
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->

        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(nameColumn) ?: "Photo $id"
            val dateAdded = cursor.getLong(dateAddedColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
            val size = cursor.getLong(sizeColumn)
            val width = cursor.getInt(widthColumn)
            val height = cursor.getInt(heightColumn)
            val mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg"
            val uri = ContentUris.withAppendedId(collection, id)
            imageList.add(
                GalleryImage(
                    uri = uri,
                    displayName = displayName,
                    dateAdded = dateAdded,
                    bucketId = bucketId,
                    bucketName = bucketName,
                    size = size,
                    width = width,
                    height = height,
                    mimeType = mimeType
                )
            )
        }
    }

    return imageList
}

fun getAlbums(images: List<GalleryImage>): List<GalleryAlbum> {
    return images
        .groupBy { it.bucketId }
        .map { (_, bucketImages) ->
            val sorted = bucketImages.sortedByDescending { it.dateAdded }
            GalleryAlbum(
                bucketId = sorted.first().bucketId,
                name = sorted.first().bucketName,
                coverImage = sorted.first(),
                photoCount = sorted.size
            )
        }
        .sortedBy { it.name.lowercase() }
}

class FavoriteStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    private companion object {
        const val PREFS_NAME = "smart_gallery_prefs"
        const val KEY_FAVORITES = "favorite_uris"
    }
}

class TrashStore(context: Context) {
    private val prefs = context.getSharedPreferences(TRASH_PREFS_NAME, Context.MODE_PRIVATE)

    data class TrashItem(
        val imageUri: String,
        val displayName: String,
        val deletedTime: Long
    )

    fun loadTrash(): List<TrashItem> {
        val trashJson = prefs.getString(KEY_TRASH, "[]") ?: "[]"
        return parseTrashJson(trashJson)
    }

    fun addToTrash(image: GalleryImage) {
        val currentTrash = loadTrash().toMutableList()
        currentTrash.add(TrashItem(image.uri.toString(), image.displayName, System.currentTimeMillis()))
        saveTrash(currentTrash)
    }

    fun removeFromTrash(imageUri: String) {
        val currentTrash = loadTrash().filter { it.imageUri != imageUri }
        saveTrash(currentTrash)
    }

    fun permanentlyDelete(imageUri: String): Boolean {
        val currentTrash = loadTrash().filter { it.imageUri != imageUri }
        saveTrash(currentTrash)
        return true // In production, would also delete file from disk
    }

    fun restoreFromTrash(imageUri: String) {
        removeFromTrash(imageUri)
    }

    fun emptyTrash() {
        saveTrash(emptyList())
    }

    private fun saveTrash(items: List<TrashItem>) {
        val json = items.joinToString(",", "[", "]") { item ->
            "{\"uri\":\"${item.imageUri}\",\"name\":\"${item.displayName}\",\"time\":${item.deletedTime}}"
        }
        prefs.edit().putString(KEY_TRASH, json).apply()
    }

    private fun parseTrashJson(json: String): List<TrashItem> {
        return try {
            json.trim().removeSurrounding("[", "]").split(",").mapNotNull { item ->
                val cleaned = item.trim().removeSurrounding("{", "}")
                val parts = cleaned.split("\"")
                if (parts.size >= 6) {
                    TrashItem(
                        imageUri = parts[3],
                        displayName = parts[7],
                        deletedTime = parts[10].replace("time:", "").toLongOrNull() ?: 0
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val TRASH_PREFS_NAME = "smart_gallery_trash"
        const val KEY_TRASH = "trash_items"
    }
}