package com.thesis.lumine.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

// catalog screen — 2-column grid na may search at category filter
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onJewelrySelected: (Jewelry) -> Unit,
    onProfileClicked:  () -> Unit = {},
    viewModel: JewelryViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val favoriteIds  by profileViewModel.favoriteIds.collectAsState()
    val jewelryList  by viewModel.jewelryList.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val error        by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { profileViewModel.loadFavorites() }

    val categories = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery      by remember { mutableStateOf("") }

    // i-filter yung list base sa search text
    val displayList = if (searchQuery.isBlank()) jewelryList
    else jewelryList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.type.contains(searchQuery, ignoreCase = true) ||
        it.material.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── header banner ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LUMINE",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 3.sp
                            )
                            Text(
                                "AR Jewelry Try-On",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick = onProfileClicked,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search jewelry...", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f))
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {{
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.7f))
                            }
                        }} else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor       = Color.White,
                            unfocusedTextColor     = Color.White,
                            focusedBorderColor     = Color.White.copy(alpha = 0.7f),
                            unfocusedBorderColor   = Color.White.copy(alpha = 0.4f),
                            cursorColor            = Color.White,
                            focusedContainerColor  = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor= Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            // ── category chips ────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick  = {
                            selectedCategory = category
                            searchQuery = ""
                            if (category == "All") viewModel.loadJewelry()
                            else viewModel.filterByType(category.lowercase())
                        },
                        label = {
                            Text(
                                category,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold
                                             else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = MaterialTheme.colorScheme.primary,
                            selectedLabelColor        = Color.White,
                            selectedLeadingIconColor  = Color.White
                        )
                    )
                }
            }

            // ── content area ──────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Text(error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadJewelry() }) { Text("Retry") }
                        }
                    }
                }
                displayList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))
                            Text("No jewelry found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    // section header count
                    Text(
                        text = "${displayList.size} item${if (displayList.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayList) { jewelry ->
                            JewelryGridCard(
                                jewelry        = jewelry,
                                isFavorite     = jewelry.id in favoriteIds,
                                onClick        = { onJewelrySelected(jewelry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2-column card — shows image + name only, click → product detail
@Composable
fun JewelryGridCard(
    jewelry: Jewelry,
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // product image — top portion of card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (!jewelry.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = jewelry.imageUrl,
                        contentDescription = jewelry.name,
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Diamond,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                // availability badge pag out of stock
                if (!jewelry.isAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Unavailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // favorite heart — top-right badge (small indicator, not a button)
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.88f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Favorited",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // product info — name + type label
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = jewelry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${jewelry.type.replaceFirstChar { it.uppercase() }} • ${jewelry.material.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "₱${String.format("%.2f", jewelry.price)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
