package com.thesis.lumine.ui.catalog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.utils.formatPeso
import com.thesis.lumine.R
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.components.EmptyState
import com.thesis.lumine.ui.components.ErrorState
import com.thesis.lumine.ui.components.JewelryGridSkeleton
import com.thesis.lumine.ui.components.JewelryImage
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

// The shop's own Facebook Page - the "f" icon in the catalog header links here.
private const val FACEBOOK_PAGE_URL = "https://www.facebook.com/jrjewelryshop"

private val CATEGORIES = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")

/** The customer home screen: a searchable, filterable 2-column catalog grid. */
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
    val context      = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { profileViewModel.loadFavorites() }

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery      by remember { mutableStateOf("") }

    // Category and search both filter the already-loaded catalog: instant, no network round trip.
    val displayList = jewelryList
        .filter { selectedCategory == "All" || it.type.equals(selectedCategory, ignoreCase = true) }
        .filter {
            searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.type.contains(searchQuery, ignoreCase = true) ||
                it.material.contains(searchQuery, ignoreCase = true)
        }

    // A failed background refresh keeps the list on screen and just says so.
    LaunchedEffect(error, jewelryList.isNotEmpty()) {
        val message = error
        if (message != null && jewelryList.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Header banner
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Facebook page link. Image instead of Icon because Icon's tint would turn the
                            // two-tone logo into a flat silhouette.
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(FACEBOOK_PAGE_URL))
                                        )
                                    } catch (_: Exception) {
                                        // no app can handle the intent - nothing sensible to do here
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_facebook),
                                    contentDescription = "Visit our Facebook page",
                                    modifier = Modifier.size(24.dp)
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

            // Category chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CATEGORIES) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick  = {
                            selectedCategory = category
                            searchQuery = ""
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

            // Content area
            val loadError = error
            when {
                isLoading && jewelryList.isEmpty() ->
                    JewelryGridSkeleton(Modifier.fillMaxSize())
                loadError != null && jewelryList.isEmpty() ->
                    ErrorState(message = loadError, onRetry = viewModel::loadJewelry)
                displayList.isEmpty() ->
                    EmptyState(Icons.Filled.SearchOff, "No jewelry found")
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

/** A catalog grid card: photo, name, type/material and price. Tapping opens the product page. */
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
            // product photo - top portion of the card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                JewelryImage(
                    imageUrl = jewelry.imageUrl,
                    contentDescription = jewelry.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )

                // "Unavailable" badge for out-of-stock items
                if (!jewelry.inStock) {
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

                // favorite heart - top-right badge (small indicator, not a button)
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

            // product info - name + type label
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
                    text = formatPeso(jewelry.price),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
