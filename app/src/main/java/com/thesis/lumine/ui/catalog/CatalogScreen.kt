package com.thesis.lumine.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

// catalog screen — ipakita yung lahat ng jewelry, may filter at favorites
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onJewelrySelected: (Jewelry) -> Unit,
    onProfileClicked: () -> Unit = {},
    viewModel: JewelryViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val favoriteIds by profileViewModel.favoriteIds.collectAsState()
    // i-load yung favorites agad para updated agad ang star icons
    LaunchedEffect(Unit) { profileViewModel.loadFavorites() }
    val jewelryList by viewModel.jewelryList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val categories = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LUMINE Catalog") },
                actions = {
                    IconButton(onClick = onProfileClicked) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // category filter chips — i-reload yung jewelry pag nag-change ng category
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            if (category == "All") {
                                viewModel.loadJewelry()
                            } else {
                                viewModel.filterByType(category.lowercase())
                            }
                        },
                        label = { Text(category) }
                    )
                }
            }

            when {
                // pag loading pa, ipakita yung spinner sa gitna
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // pag may error, ipakita yung message at retry button
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadJewelry() }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                // walang results sa filter
                jewelryList.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No jewelry found")
                    }
                }

                // i-render yung list ng jewelry cards
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(jewelryList) { jewelry ->
                            JewelryCard(
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
}

// card component para sa bawat jewelry item sa catalog
@Composable
fun JewelryCard(
    jewelry: Jewelry,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = jewelry.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${jewelry.type.replaceFirstChar { it.uppercase() }} • ${jewelry.material.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // price at action buttons — star para sa favorite, button para sa AR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${String.format("%.2f", jewelry.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // i-toggle yung favorite — gold star pag nasa favorites na
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFFD700)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Try On AR")
                    }
                }
            }
        }
    }
}