package com.thesis.lumine.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import com.thesis.lumine.data.model.AdminUserInfo
import com.thesis.lumine.data.model.EvaluationSummary
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.viewmodel.EvaluationViewModel
import com.thesis.lumine.viewmodel.JewelryViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

// admin screen — main dashboard ng admin, may tabs para sa jewelry, users, at ratings
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(),
    jewelryViewModel: JewelryViewModel = viewModel(),
    evalViewModel: EvaluationViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Jewelry", "Users", "Ratings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    // pag nag-back ang admin, i-logout at balik sa login
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // tab navigation — mag-switch sa pagitan ng 3 sections
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // i-render yung tamang tab content base sa selected index
            when (selectedTab) {
                0 -> JewelryTab(jewelryViewModel)
                1 -> UsersTab(profileViewModel)
                2 -> RatingsTab(evalViewModel)
            }
        }
    }
}

// ── Jewelry Tab — CRUD management ng jewelry catalog para sa admin ─────────────

// jewelry tab — ipakita yung lahat ng items at may FAB para mag-add ng bago
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelryTab(viewModel: JewelryViewModel) {
    val jewelryList by viewModel.jewelryList.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val crudSuccess by viewModel.crudSuccess.collectAsState()
    val error       by viewModel.error.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<Jewelry?>(null) }
    var deleteTarget  by remember { mutableStateOf<Jewelry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ipakita yung snackbar pag may success o error na nangyari sa CRUD
    LaunchedEffect(crudSuccess) { crudSuccess?.let { snackbarHostState.showSnackbar(it); viewModel.clearCrudSuccess() } }
    LaunchedEffect(error)       { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() } }

    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // i-list yung lahat ng jewelry — may edit at delete buttons sa bawat card
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jewelryList) { jewelry ->
                    AdminJewelryCard(
                        jewelry  = jewelry,
                        onEdit   = { editTarget = jewelry },
                        onDelete = { deleteTarget = jewelry }
                    )
                }
            }
        }

        // FAB para mag-add ng bagong jewelry item
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "Add Jewelry") }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // add dialog — bubukas pag nag-click ng FAB
    if (showAddDialog) {
        JewelryFormDialog(
            title = "Add Jewelry", initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { jewelry -> viewModel.createJewelry(jewelry) { showAddDialog = false } }
        )
    }

    // edit dialog — bubukas pag may naka-set na editTarget
    editTarget?.let { target ->
        JewelryFormDialog(
            title = "Edit Jewelry", initial = target,
            onDismiss = { editTarget = null },
            onConfirm = { updated -> viewModel.updateJewelry(target.id, updated) { editTarget = null } }
        )
    }

    // delete confirmation dialog — kailangan mag-confirm bago talaga mabura
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Jewelry") },
            text  = { Text("Delete \"${target.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteJewelry(target.id) { deleteTarget = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

// ── Users Tab — para sa admin na nag-mamanage ng registered users ─────────────

// users tab — may navigation between user list, detail, favorites, at delete screen
@Composable
fun UsersTab(viewModel: ProfileViewModel) {
    val users         by viewModel.adminUsers.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val selectedUser  by viewModel.selectedUser.collectAsState()
    val deleteSuccess by viewModel.deleteSuccess.collectAsState()
    var showFavorites by remember { mutableStateOf(false) }

    // i-load yung lahat ng users pagka-open ng tab
    LaunchedEffect(Unit) { viewModel.loadAdminUsers() }

    // i-reset yung showFavorites pag bumalik sa user list
    LaunchedEffect(selectedUser) { if (selectedUser == null) showFavorites = false }

    when {
        // pag na-delete yung user, ipakita yung success screen
        deleteSuccess -> AccountRemovedScreen(onBackToHome = {
            viewModel.resetDeleteSuccess()
            viewModel.clearSelectedUser()
        })
        // pag may selected user at nag-click ng favorites, ipakita ang favorites view
        selectedUser != null && showFavorites -> UserFavoritesView(
            user = selectedUser!!,
            favoriteIds = viewModel.userFavoriteIds.collectAsState().value,
            onBack = { showFavorites = false }
        )
        // pag may selected user, ipakita yung user detail screen
        selectedUser != null -> UserDetailScreen(
            user = selectedUser!!,
            viewModel = viewModel,
            onBack = { viewModel.clearSelectedUser() },
            onViewFavorites = {
                viewModel.loadUserFavorites(selectedUser!!.userId)
                showFavorites = true
            }
        )
        else -> {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                // i-list yung lahat ng registered users
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("User Accounts", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(users) { user ->
                        UserListCard(user = user, onClick = { viewModel.selectUser(user) })
                    }
                }
            }
        }
    }
}

// card para sa bawat user sa listahan — clickable para makita yung details
@Composable
fun UserListCard(user: AdminUserInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = user.username.ifBlank { "(No username)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = user.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// detail screen para sa specific user — pwede rin mag-view ng favorites o mag-remove
@Composable
fun UserDetailScreen(
    user: AdminUserInfo,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onViewFavorites: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Spacer(Modifier.height(16.dp))

        Icon(Icons.Default.AccountCircle, contentDescription = null,
            modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(12.dp))

        Text(user.username.ifBlank { "(Username)" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        // i-combine yung first at last name para sa full name display
        val fullName = listOf(user.firstName, user.lastName).filter { it.isNotBlank() }.joinToString(" ")
        if (fullName.isNotBlank()) Text(fullName, style = MaterialTheme.typography.bodyLarge)
        Text(user.email, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!user.mobileNumber.isNullOrBlank())
            Text(user.mobileNumber, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(32.dp))

        Button(onClick = onViewFavorites, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("View Favorites")
        }

        Spacer(Modifier.weight(1f))

        // remove account button — nasa ibaba at red para malinaw na destructive ito
        TextButton(
            onClick = { showRemoveDialog = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text("Remove Account") }
    }

    // confirmation dialog bago talaga i-delete yung account
    if (showRemoveDialog) {
        RemoveAccountDialog(
            user = user,
            onDismiss = { showRemoveDialog = false },
            onConfirm = { viewModel.deleteUser(user.userId) { showRemoveDialog = false } }
        )
    }
}

// dialog para i-confirm ang pag-remove ng account — may user info para hindi magkamali
@Composable
fun RemoveAccountDialog(user: AdminUserInfo, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("LUMINE", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Remove Account?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user.username.ifBlank { "(Username)" }, fontWeight = FontWeight.Medium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Remove") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Back") } }
    )
}

// success screen pagkatapos mag-delete ng user account
@Composable
fun AccountRemovedScreen(onBackToHome: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("LUMINE", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Text("Account Removed!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Account successfully removed!", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onBackToHome, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Back to Home")
        }
    }
}

// ipakita yung favorites ng specific user — para sa admin view lang
@Composable
fun UserFavoritesView(
    user: AdminUserInfo,
    favoriteIds: List<String>,
    onBack: () -> Unit,
    jewelryViewModel: JewelryViewModel = viewModel()
) {
    val jewelryList by jewelryViewModel.jewelryList.collectAsState()
    // i-load yung lahat ng jewelry para ma-cross-reference sa favoriteIds
    LaunchedEffect(Unit) { jewelryViewModel.loadJewelry() }

    val favorites = jewelryList.filter { it.id in favoriteIds }
    val categories = listOf("All", "Ring", "Necklace", "Earring", "Bracelet")
    var selected by remember { mutableStateOf("All") }
    // i-filter yung favorites base sa selected category
    val filtered = if (selected == "All") favorites
                   else favorites.filter { it.type.equals(selected, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Favorites", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        Text("(User's) Likes", fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp))

        // category filter chips para sa admin favorites view
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(selected = selected == cat, onClick = { selected = cat }, label = { Text(cat) })
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // 2-column grid para sa user's favorite jewelry
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { jewelry ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                Text("Photo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(jewelry.name, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Star, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Ratings Tab — para ma-tingnan ng admin ang lahat ng user ratings ───────────

// ratings tab — i-load at ipakita ang aggregated ratings ng bawat jewelry
@Composable
fun RatingsTab(viewModel: EvaluationViewModel) {
    val summaries by viewModel.summaries.collectAsState()

    // i-load yung summary data pagka-open ng tab
    LaunchedEffect(Unit) { viewModel.loadSummary() }

    if (summaries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No ratings yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "AR Try-On Ratings",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(summaries) { summary ->
                RatingSummaryCard(summary)
            }
        }
    }
}

// card na nagpapakita ng average rating at total count para sa isang jewelry
@Composable
fun RatingSummaryCard(summary: EvaluationSummary) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(summary.jewelryName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // i-render yung star icons base sa average rating — half star kapag may .5
                val fullStars = summary.averageRating.toInt()
                val hasHalf   = (summary.averageRating - fullStars) >= 0.5
                repeat(5) { i ->
                    val tint = if (i < fullStars || (i == fullStars && hasHalf))
                        Color(0xFFFFD700) else MaterialTheme.colorScheme.surfaceVariant
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${summary.averageRating} / 5",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                // ipakita yung total number ng ratings sa kanan
                Text(
                    "${summary.totalRatings} ${if (summary.totalRatings == 1) "rating" else "ratings"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Jewelry Card + Form — para sa admin CRUD ng jewelry items ─────────────────

// card para sa admin jewelry list — may edit at delete action buttons
@Composable
fun AdminJewelryCard(jewelry: Jewelry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(jewelry.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${jewelry.type} • ${jewelry.material}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₱${String.format("%.2f", jewelry.price)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// form dialog na ginagamit para sa add at edit ng jewelry — same UI, iba lang yung initial data
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelryFormDialog(title: String, initial: Jewelry?, onDismiss: () -> Unit, onConfirm: (Jewelry) -> Unit) {
    val types     = listOf("ring", "necklace", "earring", "bracelet")
    val materials = listOf("gold", "silver", "platinum", "rose gold")

    // i-pre-fill yung fields pag may existing jewelry (edit mode), blank kung add
    var name        by remember { mutableStateOf(initial?.name ?: "") }
    var type        by remember { mutableStateOf(initial?.type ?: types[0]) }
    var material    by remember { mutableStateOf(initial?.material ?: materials[0]) }
    var price       by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var imageUrl    by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var isAvailable by remember { mutableStateOf(initial?.isAvailable ?: true) }
    var typeExpanded     by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }
    var formError        by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it; formError = null },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth(),
                    isError = formError != null && name.isBlank())

                // dropdown para sa type ng jewelry
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(value = type, onValueChange = {}, readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        types.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                                onClick = { type = opt; typeExpanded = false })
                        }
                    }
                }

                // dropdown para sa material ng jewelry
                ExposedDropdownMenuBox(expanded = materialExpanded, onExpandedChange = { materialExpanded = it }) {
                    OutlinedTextField(value = material, onValueChange = {}, readOnly = true,
                        label = { Text("Material") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(materialExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                        materials.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt.replaceFirstChar { it.uppercase() }) },
                                onClick = { material = opt; materialExpanded = false })
                        }
                    }
                }

                OutlinedTextField(value = price, onValueChange = { price = it; formError = null },
                    label = { Text("Price (₱)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = formError != null && price.toDoubleOrNull() == null)

                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Image URL (optional)") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Available", modifier = Modifier.weight(1f))
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }

                formError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            // i-validate yung form bago i-submit — name at valid price required
            Button(onClick = {
                val parsedPrice = price.toDoubleOrNull()
                formError = when {
                    name.isBlank()     -> "Name is required."
                    parsedPrice == null -> "Enter a valid price."
                    parsedPrice < 0    -> "Price cannot be negative."
                    else -> null
                }
                if (formError == null) {
                    onConfirm(Jewelry(
                        id = initial?.id ?: "", name = name.trim(), type = type,
                        material = material, price = parsedPrice!!, imageUrl = imageUrl.trim(),
                        modelUrl = initial?.modelUrl ?: "", isAvailable = isAvailable,
                        createdAt = initial?.createdAt ?: ""
                    ))
                }
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
