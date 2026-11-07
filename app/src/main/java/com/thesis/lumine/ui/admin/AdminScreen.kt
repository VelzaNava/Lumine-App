package com.thesis.lumine.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.thesis.lumine.utils.formatDisplayDate
import com.thesis.lumine.utils.formatPeso
import com.thesis.lumine.data.model.AdminUserInfo
import com.thesis.lumine.data.model.EvaluationSummary
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.components.EmptyState
import com.thesis.lumine.ui.components.ErrorState
import com.thesis.lumine.ui.components.PhotoPager
import com.thesis.lumine.viewmodel.AuthViewModel
import com.thesis.lumine.viewmodel.EvaluationViewModel
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.PasswordChangeState
import com.thesis.lumine.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

// Red for sign out / delete. colorScheme.error is a light pink in the dark theme.
private val DestructiveRed = Color(0xFFB71C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onLogout: () -> Unit,
    onTryAr: (Jewelry) -> Unit,
    onViewProduct: (Jewelry) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    jewelryViewModel: JewelryViewModel = viewModel(),
    evalViewModel: EvaluationViewModel = viewModel()
) {
    // saveable, otherwise coming back from AR always resets to the Jewelry tab
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "LUMINE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            Text(
                                "Admin Dashboard",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                // No back arrow: on this screen it would sign the admin out, which a back arrow
                // shouldn't do. Log out is its own red button, outlined so it shows on the maroon bar.
                actions = {
                    IconButton(onClick = { showChangePassword = true }) {
                        Icon(Icons.Filled.Key, contentDescription = "Change password", tint = Color.White)
                    }
                    Surface(
                        onClick         = { showLogoutConfirm = true },
                        shape           = RoundedCornerShape(20.dp),
                        color           = DestructiveRed,
                        contentColor    = Color.White,
                        shadowElevation = 2.dp,
                        border          = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier        = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Log out", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary,
                divider          = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                listOf(
                    Pair("Jewelry", Icons.Filled.Diamond),
                    Pair("Users",   Icons.Filled.People),
                    Pair("Ratings", Icons.Filled.Star)
                ).forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        selectedContentColor   = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold
                                             else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> JewelryTab(jewelryViewModel, onTryAr)
                1 -> UsersTab(profileViewModel, jewelryViewModel, onViewProduct)
                2 -> RatingsTab(evalViewModel)
            }
        }
    }

    if (showChangePassword) {
        val context = LocalContext.current
        val passwordChange by authViewModel.passwordChange.collectAsState()

        fun closeChangePassword() {
            showChangePassword = false
            authViewModel.resetPasswordChange()
        }

        LaunchedEffect(passwordChange) {
            if (passwordChange is PasswordChangeState.Done) {
                Toast.makeText(context, "Password changed.", Toast.LENGTH_SHORT).show()
                closeChangePassword()
            }
        }

        ChangePasswordDialog(
            email     = authViewModel.getSignedInEmail(),
            state     = passwordChange,
            onSubmit  = authViewModel::changeAdminPassword,
            onDismiss = ::closeChangePassword
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout, null,
                    tint = DestructiveRed
                )
            },
            title = { Text("Log out?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You'll need to sign in again to get back into the admin dashboard.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showLogoutConfirm = false; onLogout() },
                    colors  = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = Color.White)
                ) { Text("Log Out") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// Jewelry tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelryTab(viewModel: JewelryViewModel, onTryAr: (Jewelry) -> Unit) {
    val jewelryList by viewModel.jewelryList.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val crudSuccess by viewModel.crudSuccess.collectAsState()
    val error       by viewModel.error.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    // The ID, not the item, so the sheet always reads the current jewelryList and
    // shows edits and AR toggles right away.
    var detailId      by rememberSaveable { mutableStateOf<String?>(null) }
    var editTarget    by remember { mutableStateOf<Jewelry?>(null) }
    var deleteTarget  by remember { mutableStateOf<Jewelry?>(null) }

    val detailItem = jewelryList.firstOrNull { it.id == detailId }
    val isSaving by viewModel.isSaving.collectAsState()

    var query    by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val visibleItems = jewelryList
        .filter { category == "All" || it.type.equals(category, ignoreCase = true) }
        .filter {
            query.isBlank() ||
                it.name.contains(query.trim(), ignoreCase = true) ||
                it.material.contains(query.trim(), ignoreCase = true)
        }

    val snackbarHostState = remember { SnackbarHostState() }

    // the add/edit form and the detail sheet show their own errors, this snackbar would be hidden behind them
    val showsOwnErrors = showAddDialog || editTarget != null || detailItem != null

    LaunchedEffect(crudSuccess) {
        crudSuccess?.let { snackbarHostState.showSnackbar(it); viewModel.clearCrudSuccess() }
    }
    LaunchedEffect(error, showsOwnErrors) {
        if (!showsOwnErrors) error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            jewelryList.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Diamond, null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No jewelry yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap + to add your first item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // extra bottom padding so the FAB doesn't cover the last card
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "All Items",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${jewelryList.size} items · ${jewelryList.count { it.isArEnabled }} AR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search by name or material") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("All", "Ring", "Earring", "Bracelet", "Necklace")) { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                    if (visibleItems.isEmpty()) {
                        item {
                            Text(
                                "No items match these filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            )
                        }
                    }
                    items(visibleItems, key = { it.id }) { jewelry ->
                        AdminJewelryCard(
                            jewelry = jewelry,
                            onClick = { detailId = jewelry.id }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Jewelry")
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showAddDialog) {
        JewelryFormDialog(
            title     = "Add Jewelry",
            initial   = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { jewelry -> viewModel.createJewelry(jewelry) { showAddDialog = false } }
        )
    }

    // tapping a card opens the detail sheet, editing starts from there
    detailItem?.let { item ->
        // an error the sheet already showed shouldn't pop up again once it closes
        fun closeSheet() {
            detailId = null
            viewModel.clearError()
        }

        JewelryDetailSheet(
            jewelry    = item,
            isSaving   = isSaving,
            error      = error,
            onDismiss  = { closeSheet() },
            onEdit     = { closeSheet(); editTarget = item },
            onDelete   = { closeSheet(); deleteTarget = item },
            onTryAr    = { closeSheet(); onTryAr(item) },
            onToggleAr = { enabled ->
                viewModel.clearError()
                viewModel.updateJewelry(item.id, item.copy(isArEnabled = enabled)) {}
            }
        )
    }

    editTarget?.let { target ->
        JewelryFormDialog(
            title     = "Edit Jewelry",
            initial   = target,
            onDismiss = { editTarget = null },
            onConfirm = { updated ->
                viewModel.updateJewelry(target.id, updated) {
                    editTarget = null
                    detailId   = target.id   // reopen the sheet with the saved data
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) deleteTarget = null },
            icon  = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Jewelry?", fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "\"${target.name}\" will be permanently removed, along with customers' favorites of it.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteJewelry(target.id) { deleteTarget = null } },
                    enabled = !isSaving,
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }, enabled = !isSaving) { Text("Cancel") }
            }
        )
    }
}

// One row in the jewelry list. The whole card opens the detail sheet, where the
// edit, delete and AR controls are (they were too small to tap inside the row).
@Composable
fun AdminJewelryCard(
    jewelry: Jewelry,
    onClick: () -> Unit
) {
    val dimmed = !jewelry.inStock

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp),
        // surface and background are the same color here, so a thin border marks the card
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // thumbnail
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!jewelry.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = jewelry.imageUrl,
                        contentDescription = jewelry.name,
                        modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.4f else 1f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Diamond, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        jewelry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        formatPeso(jewelry.price),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(3.dp))

                Text(
                    "${jewelry.type.replaceFirstChar { it.uppercase() }} · ${jewelry.material.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(7.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // AR status is always shown
                    MetaChip(
                        text = if (jewelry.isArEnabled) "AR on" else "AR off",
                        icon = Icons.Filled.ViewInAr,
                        container = if (jewelry.isArEnabled) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        content = if (jewelry.isArEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    // stock is only shown when tracked; no chip means not tracked, not zero
                    jewelry.stockQuantity?.let { qty ->
                        val empty = qty == 0
                        MetaChip(
                            text = if (empty) "0 left" else "$qty units",
                            icon = Icons.Filled.Inventory2,
                            container = if (empty) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.secondaryContainer,
                            content = if (empty) MaterialTheme.colorScheme.onErrorContainer
                                      else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // only shown when the item is unavailable
                    if (dimmed) {
                        MetaChip(
                            text = "Out of stock",
                            icon = null,
                            container = MaterialTheme.colorScheme.errorContainer,
                            content = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// small pill for the badges on list rows and the detail sheet
@Composable
private fun MetaChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    container: Color,
    content: Color
) {
    Surface(shape = RoundedCornerShape(7.dp), color = container) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(11.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = content,
                maxLines = 1
            )
        }
    }
}

// Detail sheet for one item: photo with an Edit button, every field, the AR toggle
// and Try in AR so the admin can check the model. Editing opens JewelryFormDialog
// instead of editing inline, since text fields in a bottom sheet behave badly.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelryDetailSheet(
    jewelry: Jewelry,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTryAr: () -> Unit,
    onToggleAr: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    // close the sheet first, then open the next thing (edit / delete / AR)
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {

            // photo with the edit button on it
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (jewelry.photos.isNotEmpty()) {
                    PhotoPager(
                        photos = jewelry.photos,
                        contentDescription = jewelry.name,
                        modifier = Modifier.fillMaxSize(),
                        iconSize = 40.dp
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "No photo yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // fade so the edit pill stays legible over any photo
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                            )
                        )
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { closeThen(onEdit) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit ${jewelry.name}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // name and price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        jewelry.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        formatPeso(jewelry.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(
                        text = jewelry.type.replaceFirstChar { it.uppercase() },
                        icon = Icons.Filled.Category,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    MetaChip(
                        text = jewelry.material.replaceFirstChar { it.uppercase() },
                        icon = Icons.Filled.Diamond,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // status: listing | units | 3D model
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatCell(
                            label = "Listing",
                            value = if (jewelry.inStock) "In stock" else "Out of stock",
                            tint  = if (jewelry.inStock) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        CellDivider()
                        StatCell(
                            label = "Units",
                            // null means not tracked, which isn't the same as 0
                            value = jewelry.stockQuantity?.toString() ?: "Not tracked",
                            tint  = when (jewelry.stockQuantity) {
                                null -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                0    -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CellDivider()
                        StatCell(
                            label = "3D model",
                            value = if (!jewelry.modelUrl.isNullOrBlank()) "Linked" else "Missing",
                            tint  = if (!jewelry.modelUrl.isNullOrBlank()) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // AR toggle. Customers can't try on an item with no 3D model, so it needs one to
                // turn on. The switch only moves once the server has saved the change.
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (jewelry.isArEnabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 60.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ViewInAr, null,
                            modifier = Modifier.size(20.dp),
                            tint = if (jewelry.isArEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "AR try-on",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (jewelry.isArEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (jewelry.isArReady) "Customers can try this on"
                                else "Hidden from customer try-on",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (jewelry.isArEnabled)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSaving) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Switch(
                            checked = jewelry.isArEnabled,
                            onCheckedChange = onToggleAr,
                            enabled = !isSaving && (jewelry.isArEnabled || !jewelry.modelUrl.isNullOrBlank()),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor   = Color.White,
                                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                error?.let { HintRow(it) }

                if (jewelry.modelUrl.isNullOrBlank()) {
                    HintRow("No 3D model linked. Tap Edit to choose one before turning on AR try-on.")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        jewelry.description?.takeIf { it.isNotBlank() }
                            ?: "No description yet. Tap Edit to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (jewelry.description.isNullOrBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "Added ${formatDisplayDate(jewelry.createdAt) ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(2.dp))

                // Try in AR works even when AR is off, so the admin can check it before turning it on.
                Button(
                    onClick  = { closeThen(onTryAr) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.ViewInAr, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Try in AR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick  = { closeThen(onDelete) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors   = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete this item", fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CellDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    )
}

// Gold notice for the admin.
@Composable
private fun HintRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            Icons.Filled.WarningAmber, null,
            tint = Color(0xFFB8922A),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Users tab

// Keeps only ASCII digits. [^0-9] instead of isDigit(), which also accepts
// other scripts' digits (Arabic-Indic, full-width...).
private val NON_DIGIT = Regex("[^0-9]")

internal fun digitsOnly(raw: String?): String = raw.orEmpty().replace(NON_DIGIT, "")

@Composable
fun UsersTab(
    viewModel: ProfileViewModel,
    jewelryViewModel: JewelryViewModel,
    onViewProduct: (Jewelry) -> Unit
) {
    val users         by viewModel.adminUsers.collectAsState()
    val isLoading     by viewModel.isLoadingAdminUsers.collectAsState()
    val selectedUser  by viewModel.selectedUser.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    val error         by viewModel.error.collectAsState()
    val context       = LocalContext.current
    var query         by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadAdminUsers() }

    // a toast because a snackbar would be hidden under the user sheet
    LaunchedEffect(error, users.isNotEmpty()) {
        val message = error
        if (message != null && users.isNotEmpty()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val visibleUsers = if (query.isBlank()) users else users.filter { user ->
        listOf(user.username, user.firstName, user.lastName, user.email, user.mobileNumber.orEmpty())
            .any { it.contains(query.trim(), ignoreCase = true) }
    }

    when {
        deleteSuccess -> AccountRemovedScreen(onBackToHome = {
            viewModel.resetDeleteSuccess()
            viewModel.clearSelectedUser()
        })
        else -> {
            val loadError = error
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                loadError != null && users.isEmpty() ->
                    ErrorState(message = loadError, onRetry = { viewModel.clearError(); viewModel.loadAdminUsers() })
                users.isEmpty() ->
                    EmptyState(Icons.Filled.PeopleOutline, "No customers have signed up yet.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Registered Users",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${users.size} total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search by name, email or mobile") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (visibleUsers.isEmpty()) {
                        item {
                            Text(
                                "No users match \"${query.trim()}\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                            )
                        }
                    }
                    items(visibleUsers, key = { it.userId }) { user ->
                        UserListCard(user = user, onClick = { viewModel.selectUser(user) })
                    }
                }
            }

            // tapping a user opens their profile sheet
            selectedUser?.let { user ->
                UserDetailSheet(
                    user             = user,
                    viewModel        = viewModel,
                    jewelryViewModel = jewelryViewModel,
                    onDismiss        = { viewModel.clearSelectedUser() },
                    onViewProduct    = onViewProduct
                )
            }
        }
    }
}

@Composable
fun UserListCard(user: AdminUserInfo, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(user, size = 46.dp, initialSize = 18.sp)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.username.ifBlank { "(No username)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// Profile picture over the username's initial, so the initial shows while the
// photo loads or if it fails.
@Composable
private fun UserAvatar(user: AdminUserInfo, size: Dp, initialSize: TextUnit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = user.username.take(1).uppercase().ifBlank { "?" },
            fontSize   = initialSize,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        if (!user.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model              = user.avatarUrl,
                contentDescription = "${user.username} profile picture",
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        }
    }
}

// Profile sheet for one user: photo, name, mobile, email, favorites (tapping one
// opens the product page) and Remove Account.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailSheet(
    user: AdminUserInfo,
    viewModel: ProfileViewModel,
    jewelryViewModel: JewelryViewModel,
    onDismiss: () -> Unit,
    onViewProduct: (Jewelry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    // keyed to the user, so the favorites grid stays open after coming back from a
    // product page but starts closed for another user
    var showFavorites    by rememberSaveable(user.userId) { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    val favoriteIds      by viewModel.userFavoriteIds.collectAsState()
    val loadingFavorites by viewModel.isLoadingUserFavorites.collectAsState()
    val jewelryList      by jewelryViewModel.jewelryList.collectAsState()
    val favorites        = jewelryList.filter { it.id in favoriteIds }

    // also runs when coming back from a product page with the grid open
    LaunchedEffect(user.userId, showFavorites) {
        if (showFavorites) viewModel.loadUserFavorites(user.userId)
    }

    val mobile   = digitsOnly(user.mobileNumber)
    val fullName = listOf(user.firstName, user.lastName)
        .filter { it.isNotBlank() }.joinToString(" ")

    // hide the sheet before navigating; the user stays selected so it reopens on return
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(user, size = 88.dp, initialSize = 32.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                user.username.ifBlank { "(No username)" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (fullName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape    = RoundedCornerShape(14.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminDetailRow(Icons.Filled.Phone, "Mobile", mobile.ifEmpty { "Not provided" })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    AdminDetailRow(Icons.Filled.Email, "Email", user.email)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                onClick  = { showFavorites = !showFavorites },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Favorite, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "View saved favorites",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showFavorites) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showFavorites) "Hide favorites" else "Show favorites",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showFavorites) {
                Spacer(Modifier.height(12.dp))
                when {
                    loadingFavorites -> CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp).size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    favorites.isEmpty() -> Text(
                        "No saved favorites yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                    else -> FavoritesGrid(
                        items   = favorites,
                        onClick = { item -> closeThen { onViewProduct(item) } }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick  = { showRemoveDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = DestructiveRed)
            ) {
                Icon(Icons.Filled.PersonRemove, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remove Account", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showRemoveDialog) {
        RemoveAccountDialog(
            user      = user,
            onDismiss = { showRemoveDialog = false },
            onConfirm = { viewModel.deleteUser(user.userId) { showRemoveDialog = false } }
        )
    }
}

// Favorites in two columns. Plain rows instead of a lazy grid because it's inside
// the sheet's scrolling column and the list is short.
@Composable
private fun FavoritesGrid(items: List<Jewelry>, onClick: (Jewelry) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { jewelry ->
                    Card(
                        onClick   = { onClick(jewelry) },
                        modifier  = Modifier.weight(1f),
                        shape     = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!jewelry.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = jewelry.imageUrl,
                                        contentDescription = jewelry.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Diamond, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    jewelry.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    formatPeso(jewelry.price),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                // keep a lone last card at half width instead of stretching it
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RemoveAccountDialog(user: AdminUserInfo, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.PersonRemove, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp))
        },
        title = { Text("Remove Account?", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "This will permanently delete:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    user.username.ifBlank { user.email },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Remove") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AccountRemovedScreen(onBackToHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(24.dp))
            Text("Account Removed",
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "The account has been successfully removed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Users", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Ratings tab

@Composable
fun RatingsTab(viewModel: EvaluationViewModel) {
    val summaries by viewModel.summaries.collectAsState()
    val loaded by viewModel.summaryLoaded.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadSummary() }

    val loadError = error
    if (!loaded && summaries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (loadError != null && summaries.isEmpty()) {
        ErrorState(message = loadError, onRetry = viewModel::loadSummary)
    } else if (summaries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.StarBorder, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                Spacer(Modifier.height(16.dp))
                Text("No ratings yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text("Ratings will appear here after users try on jewelry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AR Try-On Ratings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text("${summaries.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
            items(summaries) { summary -> RatingSummaryCard(summary) }
        }
    }
}

@Composable
fun RatingSummaryCard(summary: EvaluationSummary) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    summary.jewelryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${summary.totalRatings} review${if (summary.totalRatings != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row {
                    val fullStars = summary.averageRating.toInt()
                    val hasHalf   = (summary.averageRating - fullStars) >= 0.5
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i < fullStars || (i == fullStars && hasHalf))
                                Color(0xFFFFB300)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    String.format("%.1f", summary.averageRating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("/ 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    "${(summary.averageRating / 5.0 * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (summary.averageRating / 5.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color     = Color(0xFFFFB300),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
