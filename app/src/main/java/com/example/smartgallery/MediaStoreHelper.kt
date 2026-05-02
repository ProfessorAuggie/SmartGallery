package com.example.smartgallery

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class GalleryImage(
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val bucketId: Long,
    val bucketName: String
)

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
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
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

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getString(nameColumn) ?: "Photo $id"
            val dateAdded = cursor.getLong(dateAddedColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn) ?: "Unknown"
            val uri = ContentUris.withAppendedId(collection, id)
            imageList.add(
                GalleryImage(
                    uri = uri,
                    displayName = displayName,
                    dateAdded = dateAdded,
                    bucketId = bucketId,
                    bucketName = bucketName
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