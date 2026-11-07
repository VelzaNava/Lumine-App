package com.thesis.lumine.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.components.EmptyState
import com.thesis.lumine.ui.components.JewelryGridSkeleton
import com.thesis.lumine.ui.components.JewelryImage
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

private val CATEGORIES = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")

/** The jewelry the user saved, filterable by category. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onJewelrySelected: (Jewelry) -> Unit,
    profileViewModel: ProfileViewModel = viewModel(),
    jewelryViewModel: JewelryViewModel = viewModel()
) {
    val favoriteIds by profileViewModel.favoriteIds.collectAsState()
    val favoritesLoaded by profileViewModel.favoritesLoaded.collectAsState()
    val allJewelry by jewelryViewModel.jewelryList.collectAsState()
    val catalogLoading by jewelryViewModel.isLoading.collectAsState()
    val error by profileViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selected by remember { mutableStateOf("All") }

    // Favorites are stored as ids; the catalog supplies the rest of each item.
    LaunchedEffect(Unit) { profileViewModel.loadFavorites() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.clearError()
        }
    }

    val favoriteItems = allJewelry.filter { it.id in favoriteIds }
    val filtered = if (selected == "All") favoriteItems
                   else favoriteItems.filter { it.type.equals(selected, ignoreCase = true) }

    // Loading until both halves have arrived - never flash "no favorites" before then.
    val isLoading = !favoritesLoaded || (catalogLoading && allJewelry.isEmpty())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Text(
                "My Likes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { selected = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                isLoading -> JewelryGridSkeleton(Modifier.fillMaxSize(), placeholderCount = 4)
                filtered.isEmpty() -> EmptyState(
                    Icons.Filled.FavoriteBorder,
                    if (favoriteItems.isEmpty()) "No favorites yet.\nTap the heart on any item to save it here."
                    else "No saved items in this category."
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { jewelry ->
                        FavoriteItemCard(
                            jewelry = jewelry,
                            isFavorite = jewelry.id in favoriteIds,
                            onToggleFavorite = { profileViewModel.toggleFavorite(jewelry.id) },
                            onClick = { onJewelrySelected(jewelry) }
                        )
                    }
                }
            }
        }
    }
}

/** A saved item: photo, name and a star to un-save it. */
@Composable
fun FavoriteItemCard(
    jewelry: Jewelry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            JewelryImage(
                imageUrl = jewelry.imageUrl,
                contentDescription = jewelry.name,
                iconSize = 32.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = jewelry.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Gold star when saved; tapping it removes the item from favorites.
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFD700)
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
