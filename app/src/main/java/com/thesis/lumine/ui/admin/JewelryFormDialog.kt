package com.thesis.lumine.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.data.model.ArModels
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.components.JewelryImage
import com.thesis.lumine.utils.formatPeso
import com.thesis.lumine.viewmodel.JewelryViewModel
import java.math.BigDecimal

private val JEWELRY_TYPES = listOf("ring", "earring", "bracelet", "necklace")
private val MATERIAL_SUGGESTIONS = listOf("Gold", "White Gold", "Rose Gold", "Silver", "Platinum")

private const val MAX_NAME_LENGTH = 120
private const val MAX_MATERIAL_LENGTH = 60
private const val MAX_DESCRIPTION_LENGTH = 1000
private const val MAX_STOCK = 99_999

/** Everything the admin can edit - compared against the starting values to detect unsaved changes. */
private data class JewelryDraft(
    val name: String,
    val description: String,
    val type: String,
    val material: String,
    val price: String,
    val stock: String,
    val photos: List<String>,
    val isAvailable: Boolean,
    val isArEnabled: Boolean,
    val arModel: String
)

private fun Jewelry?.toDraft() = JewelryDraft(
    name = this?.name.orEmpty(),
    description = this?.description.orEmpty(),
    type = this?.type?.lowercase()?.takeIf { it in JEWELRY_TYPES } ?: JEWELRY_TYPES.first(),
    material = this?.material.orEmpty(),
    price = this?.price?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty(),
    stock = this?.stockQuantity?.toString().orEmpty(),
    photos = this?.photos.orEmpty(),
    isAvailable = this?.isAvailable ?: true,
    isArEnabled = this?.isArEnabled ?: false,
    arModel = this?.modelUrl.orEmpty()
)

private val StringListSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

/**
 * Full-screen form for adding or editing a catalog item. Sections follow the
 * order an admin fills them in - photos, what it is, price and stock, then
 * where it shows up. Nothing is saved until Save, and closing with unsaved
 * changes asks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelryFormDialog(
    title: String,
    initial: Jewelry?,
    onDismiss: () -> Unit,
    onConfirm: (Jewelry) -> Unit,
    viewModel: JewelryViewModel = viewModel()
) {
    val isUploading by viewModel.isUploading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val start = remember { initial.toDraft() }
    var name by rememberSaveable { mutableStateOf(start.name) }
    var description by rememberSaveable { mutableStateOf(start.description) }
    var type by rememberSaveable { mutableStateOf(start.type) }
    var material by rememberSaveable { mutableStateOf(start.material) }
    var price by rememberSaveable { mutableStateOf(start.price) }
    var stock by rememberSaveable { mutableStateOf(start.stock) }
    val photos = rememberSaveable(saver = StringListSaver) { start.photos.toMutableStateList() }
    var isAvailable by rememberSaveable { mutableStateOf(start.isAvailable) }
    var isArEnabled by rememberSaveable { mutableStateOf(start.isArEnabled) }
    var arModel by rememberSaveable { mutableStateOf(start.arModel) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // Once units are counted they decide whether the item is in stock; the switch is for when they aren't.
    val units = stock.toIntOrNull()
    val inStock = units?.let { it > 0 } ?: isAvailable

    // Validation
    val parsedPrice = price.toDoubleOrNull()
    val nameError = if (name.isBlank()) "Enter the item's name." else null
    val priceError = when {
        price.isBlank() -> "Enter a price."
        parsedPrice == null -> "Enter a valid price, e.g. 25303.50"
        else -> null
    }
    val arModelError = if (isArEnabled && arModel.isBlank()) "Choose the 3D model customers will try on." else null
    val isValid = nameError == null && priceError == null && arModelError == null

    val isDirty = JewelryDraft(
        name, description, type, material, price, stock, photos.toList(), isAvailable, isArEnabled, arModel
    ) != start

    fun requestClose() {
        when {
            isSaving -> Unit
            isDirty -> confirmDiscard = true
            else -> onDismiss()
        }
    }

    fun save() {
        showErrors = true
        if (!isValid || isUploading || isSaving) return
        onConfirm(
            Jewelry(
                id = initial?.id.orEmpty(),
                createdAt = initial?.createdAt.orEmpty(),
                name = name.trim(),
                type = type,
                material = material.trim(),
                price = parsedPrice ?: return,
                imageUrl = photos.firstOrNull(),
                imageUrls = photos.toList(),
                modelUrl = arModel.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                isAvailable = inStock,
                isArEnabled = isArEnabled,
                stockQuantity = units
            )
        )
    }

    // While the form is open, save errors show here (the list's snackbar is hidden behind it).
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Android's photo picker: no storage permission needed.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            viewModel.uploadJewelryImage(it) { url ->
                if (photos.size < Jewelry.MAX_PHOTOS) photos.add(url)
            }
        }
    }

    Dialog(
        onDismissRequest = ::requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        StyleDialogSystemBars()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = ::requestClose) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = ::save,
                            enabled = !isUploading && !isSaving,
                            modifier = Modifier.padding(end = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color.White.copy(alpha = 0.5f),
                                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            when {
                                isSaving -> CircularProgressIndicator(
                                    Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary
                                )
                                isUploading -> Text("Uploading…")
                                else -> Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0),
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Photos
                FormSection(
                    title = "Photos",
                    subtitle = if (photos.size > 1) "Up to ${Jewelry.MAX_PHOTOS}. Tap a photo to make it the cover."
                               else "Up to ${Jewelry.MAX_PHOTOS}. The first photo is the cover shown in the catalog."
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        photos.forEachIndexed { index, url ->
                            PhotoSlot(
                                url = url,
                                isCover = index == 0,
                                onMakeCover = { photos.removeAt(index); photos.add(0, url) },
                                onRemove = { photos.removeAt(index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (photos.size < Jewelry.MAX_PHOTOS) {
                            AddPhotoSlot(
                                isUploading = isUploading,
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // Keep every slot the same width however many photos there are.
                            repeat(Jewelry.MAX_PHOTOS - photos.size - 1) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Details
                FormSection(title = "Details") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                        label = { Text("Name") },
                        isError = showErrors && nameError != null,
                        supportingText = if (showErrors && nameError != null) {
                            { Text(nameError) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(MAX_DESCRIPTION_LENGTH) },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Weight, finish, what makes it special…") },
                        supportingText = { Text("${description.length}/$MAX_DESCRIPTION_LENGTH") },
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category
                FormSection(title = "Category") {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        JEWELRY_TYPES.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = type == option,
                                onClick = {
                                    type = option
                                    // A ring model can't dress a necklace: drop a model from another category.
                                    if (arModel.isNotBlank() && arModel.lowercase() !in ArModels.forType(option)) arModel = ""
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, JEWELRY_TYPES.size),
                                icon = {},
                                label = { Text(option.replaceFirstChar { it.uppercase() }, maxLines = 1) }
                            )
                        }
                    }
                }

                // Material
                FormSection(title = "Material") {
                    OutlinedTextField(
                        value = material,
                        onValueChange = { material = it.take(MAX_MATERIAL_LENGTH) },
                        label = { Text("Material (optional)") },
                        placeholder = { Text("e.g. White Gold") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        MATERIAL_SUGGESTIONS.forEach { suggestion ->
                            FilterChip(
                                selected = material.equals(suggestion, ignoreCase = true),
                                onClick = { material = suggestion },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }

                // Price and stock
                FormSection(title = "Price & stock") {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { input ->
                            // Digits and one decimal point, at most 2 decimals.
                            val cleaned = input.filter { it.isDigit() || it == '.' }
                            val decimals = cleaned.substringAfter('.', missingDelimiterValue = "")
                            if (cleaned.count { it == '.' } <= 1 && decimals.length <= 2 && cleaned.length <= 12) {
                                price = cleaned
                            }
                        },
                        label = { Text("Price") },
                        prefix = { Text("₱ ") },
                        isError = showErrors && priceError != null,
                        supportingText = {
                            when {
                                showErrors && priceError != null -> Text(priceError)
                                parsedPrice != null -> Text("Shown as ${formatPeso(parsedPrice)}")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Blank = "not tracked" (null) - never coerced to 0. The x on the field
                    // is how an admin returns to "not tracked" after typing a number.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { it.isDigit() }) stock = input
                            },
                            label = { Text("Units in stock (optional)") },
                            placeholder = { Text("Not tracked") },
                            leadingIcon = { Icon(Icons.Filled.Inventory2, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (stock.isNotBlank()) {
                                    IconButton(onClick = { stock = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Stop tracking units", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        StepperButton(Icons.Filled.Remove, "Decrease units") {
                            val current = stock.toIntOrNull()
                            if (current != null && current > 0) stock = (current - 1).toString()
                        }
                        StepperButton(Icons.Filled.Add, "Increase units") {
                            val current = stock.toIntOrNull() ?: 0
                            if (current < MAX_STOCK) stock = (current + 1).toString()
                        }
                    }
                    Text(
                        when {
                            units == null -> "Leave blank if you don't count units."
                            units == 0 -> "0 units left, so customers see this item as unavailable."
                            else -> "Shown to you as \"$stock units\"."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Visibility
                FormSection(title = "Visibility") {
                    SwitchRow(
                        icon = Icons.Filled.Storefront,
                        title = "In stock",
                        subtitle = when {
                            units != null -> "Set by the units above"
                            isAvailable -> "Customers can buy this item"
                            else -> "Shown in the catalog as unavailable"
                        },
                        checked = inStock,
                        enabled = units == null,
                        onCheckedChange = { isAvailable = it }
                    )
                    SwitchRow(
                        icon = Icons.Filled.ViewInAr,
                        title = "AR try-on",
                        subtitle = if (isArEnabled) "Customers can try this on with the camera"
                                   else "Not available for try-on",
                        checked = isArEnabled,
                        onCheckedChange = { isArEnabled = it }
                    )
                    if (isArEnabled) {
                        ArModelField(
                            type = type,
                            selected = arModel,
                            error = if (showErrors) arModelError else null,
                            onSelect = { arModel = it }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?", fontWeight = FontWeight.Bold) },
            text = { Text("What you've entered for this item won't be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDismiss() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            }
        )
    }
}

/**
 * A full-screen dialog is its own window, drawn edge to edge: give its status
 * bar white icons (the top bar is dark red) and its navigation bar icons that
 * suit the page behind them.
 */
@Composable
private fun StyleDialogSystemBars() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

/** A titled card that groups related fields. */
@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            icon, null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/** Picks which of the AR scene's 3D models this item uses (see [ArModels]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArModelField(type: String, selected: String, error: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = ArModels.forType(type)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.takeIf { it.isNotBlank() }?.let(ArModels::displayName).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("3D model") },
            placeholder = { Text("Choose a model") },
            isError = error != null,
            supportingText = {
                Text(error ?: "The piece customers see on themselves in AR.")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { model ->
                DropdownMenuItem(
                    text = { Text(ArModels.displayName(model)) },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** A chosen product photo: tap to make it the cover, x to remove it. */
@Composable
private fun PhotoSlot(
    url: String,
    isCover: Boolean,
    onMakeCover: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isCover, onClickLabel = "Make cover photo", onClick = onMakeCover)
    ) {
        JewelryImage(
            imageUrl = url,
            contentDescription = if (isCover) "Cover photo" else "Product photo",
            modifier = Modifier.fillMaxSize(),
            iconSize = 24.dp
        )

        if (isCover) {
            Text(
                "Cover",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(vertical = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClickLabel = "Remove photo", onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

/** The empty slot that opens the photo picker, or shows progress while a photo uploads. */
@Composable
private fun AddPhotoSlot(isUploading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = !isUploading, onClickLabel = "Add a photo", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.AddPhotoAlternate, null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text("Add photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(width = 48.dp, height = 56.dp)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}
