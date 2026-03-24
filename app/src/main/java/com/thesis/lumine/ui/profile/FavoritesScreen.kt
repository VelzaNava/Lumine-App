package com.thesis.lumine.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

// favorites screen — ipakita lang yung mga jewelry na naka-save ng user
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onJewelrySelected: (Jewelry) -> Unit,
    profileViewModel: ProfileViewModel = viewModel(),
    jewelryViewModel: JewelryViewModel = viewModel()
) {
    val favoriteIds  by profileViewModel.favoriteIds.collectAsState()
    val allJewelry   by jewelryViewModel.jewelryList.collectAsState()

    val categories = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")
    var selected by remember { mutableStateOf("All") }

    // i-load yung favorites at lahat ng jewelry para ma-cross-match
    LaunchedEffect(Unit) {
        profileViewModel.loadFavorites()
        jewelryViewModel.loadJewelry()
    }

    // i-filter yung allJewelry para makuha lang yung mga naka-save
    val favoriteItems = allJewelry.filter { it.id in favoriteIds }
    val filtered = if (selected == "All") favoriteItems
                   else favoriteItems.filter { it.type.equals(selected, ignoreCase = true) }

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
        }
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

            // category filter chips para ma-filter yung favorites by type
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selected == cat,
                        onClick = { selected = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // pag walang favorites, ipakita yung empty state
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // 2-column grid para sa mga favorite items
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { jewelry ->
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

// card composable para sa bawat item sa favorites grid
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
            // placeholder para sa jewelry image — placeholder text muna
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // jewelry name at favorite star button sa isang row
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
                // i-toggle yung favorite — gold star kung naka-save, gray kung hindi
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFD700)
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
