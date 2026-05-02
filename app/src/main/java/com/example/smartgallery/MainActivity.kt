@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.smartgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.smartgallery.ui.theme.SmartGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartGalleryTheme {
                GalleryScreen()
            }
        }
    }
}

private enum class GallerySortMode(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name A-Z")
}

private enum class GalleryFeedMode(val label: String) {
    PHOTOS("Photos"),
    FAVORITES("Favorites"),
    ALBUMS("Albums"),
    TRASH("Trash")
}

private data class GallerySection(
    val title: String,
    val images: List<GalleryImage>
)

@Composable
private fun GalleryScreen() {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val favoriteStore = remember { FavoriteStore(context) }
    val trashStore = remember { TrashStore(context) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    var refreshTick by remember { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(GallerySortMode.NEWEST) }
    var feedMode by rememberSaveable { mutableStateOf(GalleryFeedMode.PHOTOS) }
    var favoriteUris by remember { mutableStateOf(favoriteStore.loadFavorites()) }
    var deletedUris by remember { mutableStateOf(trashStore.loadTrash().map { it.imageUri }.toSet()) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedViewerImages by remember { mutableStateOf<List<GalleryImage>?>(null) }
    var selectedViewerIndex by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) refreshTick++
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(permission)
    }

    val allImages = remember(hasPermission, refreshTick) {
        if (hasPermission) getAllImages(context) else emptyList()
    }
    val albums = remember(allImages) { getAlbums(allImages) }

    val activeImages = remember(allImages, deletedUris, feedMode) {
        when (feedMode) {
            GalleryFeedMode.TRASH -> allImages.filter { deletedUris.contains(it.uri.toString()) }
            else -> allImages.filter { !deletedUris.contains(it.uri.toString()) }
        }
    }
    val feedFilteredImages = remember(activeImages, favoriteUris, feedMode) {
        when (feedMode) {
            GalleryFeedMode.FAVORITES -> activeImages.filter { favoriteUris.contains(it.uri.toString()) }
            else -> activeImages
        }
    }
    val albumFilteredImages = remember(feedFilteredImages, selectedAlbumId, feedMode) {
        if (feedMode == GalleryFeedMode.TRASH || selectedAlbumId == null) {
            feedFilteredImages
        } else {
            feedFilteredImages.filter { it.bucketId == selectedAlbumId }
        }
    }
    val visibleImages = remember(albumFilteredImages, query, sortMode) {
        albumFilteredImages
            .asSequence()
            .filter { query.isBlank() || it.displayName.contains(query, ignoreCase = true) }
            .let { images ->
                when (sortMode) {
                    GallerySortMode.NEWEST -> images.sortedByDescending { it.dateAdded }
                    GallerySortMode.OLDEST -> images.sortedBy { it.dateAdded }
                    GallerySortMode.NAME -> images.sortedBy { it.displayName.lowercase() }
                }
            }
            .toList()
    }

    val toggleFavorite = rememberUpdatedState<(GalleryImage) -> Unit> { image ->
        val uriString = image.uri.toString()
        val updated = if (favoriteUris.contains(uriString)) favoriteUris - uriString else favoriteUris + uriString
        favoriteUris = updated
        favoriteStore.saveFavorites(updated)
    }
    val deleteImage = rememberUpdatedState<(GalleryImage) -> Unit> { image ->
        deletedUris = deletedUris + image.uri.toString()
        trashStore.addToTrash(image)
        if (feedMode != GalleryFeedMode.TRASH) refreshTick++
    }
    val restoreImage = rememberUpdatedState<(GalleryImage) -> Unit> { image ->
        deletedUris = deletedUris - image.uri.toString()
        trashStore.restoreFromTrash(image.uri.toString())
        if (feedMode == GalleryFeedMode.TRASH) refreshTick++
    }
    val openViewer = rememberUpdatedState<(GalleryImage) -> Unit> { image ->
        selectedViewerImages = visibleImages
        selectedViewerIndex = visibleImages.indexOfFirst { it.uri == image.uri }.coerceAtLeast(0)
    }

    val subtitle = when {
        !hasPermission -> "Grant access to view photos"
        feedMode == GalleryFeedMode.TRASH -> "${visibleImages.size} item${if (visibleImages.size == 1) "" else "s"} in trash"
        feedMode == GalleryFeedMode.FAVORITES -> "${visibleImages.size} favorite photo${if (visibleImages.size == 1) "" else "s"}"
        query.isNotBlank() -> "${visibleImages.size} result${if (visibleImages.size == 1) "" else "s"} for \"$query\""
        else -> "${visibleImages.size} photo${if (visibleImages.size == 1) "" else "s"} on device"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("SmartGallery", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { refreshTick++ }, enabled = hasPermission) { Text("Refresh") }
                    TextButton(onClick = { sortMenuExpanded = true }, enabled = hasPermission) { Text(sortMode.label) }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        GallerySortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = {
                                    sortMode = mode
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroHeader(
                    hasPermission = hasPermission,
                    visibleCount = visibleImages.size,
                    totalCount = allImages.size,
                    favoriteCount = favoriteUris.size,
                    query = query,
                    onQueryChange = { query = it },
                    feedMode = feedMode,
                    onFeedModeChange = { mode ->
                        feedMode = mode
                        selectedAlbumId = null
                    },
                    onRefresh = { refreshTick++ }
                )

                when {
                    !hasPermission -> PermissionState(onGrantClick = { permissionLauncher.launch(permission) })
                    visibleImages.isEmpty() -> EmptyGalleryState(
                        message = when {
                            feedMode == GalleryFeedMode.TRASH -> "Your trash is empty. Deleted items will appear here with a restore option."
                            feedMode == GalleryFeedMode.FAVORITES -> "No favorites yet. Tap the heart on photos to build a favorites collection."
                            query.isNotBlank() -> "No photos match \"$query\". Try another keyword."
                            else -> "Add photos to the device and reopen SmartGallery to see them here."
                        }
                    )
                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        GalleryGrid(
                            images = visibleImages,
                            favoriteUris = favoriteUris,
                            onToggleFavorite = { toggleFavorite.value(it) },
                            onImageClick = { openViewer.value(it) },
                            onDeleteImage = { deleteImage.value(it) },
                            onRestoreImage = { restoreImage.value(it) },
                            showTrashActions = feedMode == GalleryFeedMode.TRASH,
                            modifier = Modifier.weight(1f)
                        )
                        AlbumStrip(
                            albums = albums,
                            trashCount = deletedUris.size,
                            selectedAlbumId = selectedAlbumId,
                            selectedFeedMode = feedMode,
                            onAlbumSelected = {
                                selectedAlbumId = it
                                feedMode = GalleryFeedMode.ALBUMS
                            },
                            onFeedModeSelected = { feedMode = it }
                        )
                    }
                }
            }

            selectedViewerImages?.let { images ->
                FullScreenImageViewer(
                    images = images,
                    startIndex = selectedViewerIndex,
                    favoriteUris = favoriteUris,
                    onToggleFavorite = { toggleFavorite.value(it) },
                    onDismiss = { selectedViewerImages = null }
                )
            }
        }
    }
}

@Composable
private fun HeroHeader(
    hasPermission: Boolean,
    visibleCount: Int,
    totalCount: Int,
    favoriteCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    feedMode: GalleryFeedMode,
    onFeedModeChange: (GalleryFeedMode) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasPermission) "Your gallery is ready" else "Welcome to SmartGallery",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (hasPermission) {
                    "$visibleCount photo${if (visibleCount == 1) "" else "s"} visible from $totalCount total"
                } else {
                    "Allow access to search, sort, and explore your photo collection."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$favoriteCount favorited photo${if (favoriteCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = hasPermission,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                placeholder = { Text("Search photos, albums, places") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GalleryFeedMode.entries.forEach { mode ->
                    FilterChip(
                        selected = feedMode == mode,
                        onClick = { onFeedModeChange(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ElevatedButton(onClick = onRefresh, enabled = hasPermission) { Text("Refresh") }
            }
        }
    }
}

@Composable
private fun PermissionState(onGrantClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Allow photo access", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "SmartGallery needs access to your images so it can display your gallery grid.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onGrantClick) { Text("Grant Permission") }
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No images found", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GalleryGrid(
    images: List<GalleryImage>,
    favoriteUris: Set<String>,
    onToggleFavorite: (GalleryImage) -> Unit,
    onImageClick: (GalleryImage) -> Unit,
    onDeleteImage: (GalleryImage) -> Unit,
    onRestoreImage: (GalleryImage) -> Unit,
    showTrashActions: Boolean,
    modifier: Modifier = Modifier
) {
    val sections = remember(images) { buildGallerySections(images) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        sections.forEach { section ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${section.images.size} photo${if (section.images.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(section.images, key = { it.uri.toString() }) { image ->
                GalleryCard(
                    image = image,
                    isFavorite = favoriteUris.contains(image.uri.toString()),
                    onToggleFavorite = onToggleFavorite,
                    onImageClick = onImageClick,
                    onDeleteImage = onDeleteImage,
                    onRestoreImage = onRestoreImage,
                    showTrashActions = showTrashActions
                )
            }
        }
    }
}

@Composable
private fun GalleryCard(
    image: GalleryImage,
    isFavorite: Boolean,
    onToggleFavorite: (GalleryImage) -> Unit,
    onImageClick: (GalleryImage) -> Unit,
    onDeleteImage: (GalleryImage) -> Unit,
    onRestoreImage: (GalleryImage) -> Unit,
    showTrashActions: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFavorite) 8.dp else 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height((image.aspectRatio * 180).dp.coerceIn(150.dp, 280.dp))
            .clickable { onImageClick(image) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image.uri,
                contentDescription = image.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                            startY = 100f
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = { onToggleFavorite(image) }, modifier = Modifier.size(36.dp)) {
                    Text(if (isFavorite) "♥" else "♡", fontSize = 18.sp)
                }
                if (showTrashActions) {
                    TextButton(onClick = { onRestoreImage(image) }, modifier = Modifier.size(36.dp)) {
                        Text("↩", fontSize = 18.sp)
                    }
                } else {
                    TextButton(onClick = { onDeleteImage(image) }, modifier = Modifier.size(36.dp)) {
                        Text("🗑", fontSize = 14.sp)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = image.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = image.formattedDate,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp
                    )
                    Text(
                        text = image.formattedSize,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp
                    )
                }
                if (image.width > 0 && image.height > 0) {
                    Text(
                        text = "${image.width}×${image.height}",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

private fun buildGallerySections(images: List<GalleryImage>): List<GallerySection> {
    val nowSeconds = System.currentTimeMillis() / 1000
    val daySeconds = 24 * 60 * 60
    val weekSeconds = 7 * daySeconds
    val monthSeconds = 30 * daySeconds

    val grouped = images.groupBy { image ->
        val age = nowSeconds - image.dateAdded
        when {
            age < daySeconds -> "Today"
            age < 2 * daySeconds -> "Yesterday"
            age < weekSeconds -> "This week"
            age < monthSeconds -> "This month"
            else -> "Older"
        }
    }

    return listOf("Today", "Yesterday", "This week", "This month", "Older")
        .mapNotNull { title -> grouped[title]?.let { GallerySection(title, it) } }
}

@Composable
private fun AlbumStrip(
    albums: List<GalleryAlbum>,
    trashCount: Int,
    selectedAlbumId: Long?,
    selectedFeedMode: GalleryFeedMode,
    onAlbumSelected: (Long?) -> Unit,
    onFeedModeSelected: (GalleryFeedMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Albums", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${albums.size} folders${if (trashCount > 0) " • $trashCount in trash" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AlbumChip(
                        title = "All Photos",
                        count = albums.sumOf { it.photoCount },
                        selected = selectedAlbumId == null && selectedFeedMode != GalleryFeedMode.TRASH,
                        coverUri = albums.firstOrNull()?.coverImage?.uri,
                        onClick = {
                            onAlbumSelected(null)
                            onFeedModeSelected(GalleryFeedMode.PHOTOS)
                        }
                    )
                }
                items(albums, key = { it.bucketId }) { album ->
                    AlbumChip(
                        title = album.name,
                        count = album.photoCount,
                        selected = selectedAlbumId == album.bucketId,
                        coverUri = album.coverImage.uri,
                        onClick = {
                            onAlbumSelected(album.bucketId)
                            onFeedModeSelected(GalleryFeedMode.ALBUMS)
                        }
                    )
                }
                item {
                    if (trashCount > 0) {
                        AlbumChip(
                            title = "Trash",
                            count = trashCount,
                            selected = selectedFeedMode == GalleryFeedMode.TRASH,
                            coverUri = null,
                            isTrash = true,
                            onClick = {
                                onAlbumSelected(null)
                                onFeedModeSelected(GalleryFeedMode.TRASH)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumChip(
    title: String,
    count: Int,
    selected: Boolean,
    coverUri: android.net.Uri? = null,
    onClick: () -> Unit,
    isTrash: Boolean = false
) {
    val containerTarget = when {
        isTrash -> MaterialTheme.colorScheme.errorContainer
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val containerColor by animateColorAsState(containerTarget, label = "albumContainer")
    val contentColor by animateColorAsState(
        targetValue = if (selected || isTrash) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "albumContent"
    )
    val elevation by animateDpAsState(targetValue = if (selected) 10.dp else 2.dp, label = "albumElevation")
    val scale by animateFloatAsState(targetValue = if (selected) 1.02f else 1f, label = "albumScale")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (isTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (coverUri != null && !isTrash) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = "$title cover",
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Transparent),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isTrash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTrash) Text("🗑", fontSize = 24.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count photos",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun FullScreenImageViewer(
    images: List<GalleryImage>,
    startIndex: Int,
    favoriteUris: Set<String>,
    onToggleFavorite: (GalleryImage) -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { images.size }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF090B10),
            modifier = Modifier
                .fillMaxWidth()
                .height(680.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black, Color(0xFF101521), Color.Black)
                            )
                        )
                )
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    ZoomableImage(image = images[page], modifier = Modifier.fillMaxSize())
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentImage = images[pagerState.currentPage]
                    val isFavorite = favoriteUris.contains(currentImage.uri.toString())
                    Button(onClick = { onToggleFavorite(currentImage) }) {
                        Text(if (isFavorite) "Unfavorite" else "Favorite")
                    }
                    Button(onClick = onDismiss) { Text("Close") }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = images[pagerState.currentPage].displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pagerState.currentPage + 1}/${images.size}",
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    image: GalleryImage,
    modifier: Modifier = Modifier
) {
    var scale by remember(image.uri) { mutableStateOf(1f) }
    var offset by remember(image.uri) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier.pointerInput(image.uri) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 4f)
                offset += pan
            }
        }
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartGalleryTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "SmartGallery",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}