package com.thesis.lumine.ui.ar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.utils.formatPeso
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.utils.CameraManager
import com.thesis.lumine.utils.MediaPipeHelper
import com.thesis.lumine.utils.UnityBridge
import com.thesis.lumine.viewmodel.EvaluationViewModel
import com.thesis.lumine.viewmodel.JewelryViewModel

// AR screen - main screen ng try-on feature, may camera, Unity, at 2D overlay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    jewelry: Jewelry,
    onBack: () -> Unit,
    // false for admins: they use AR to check an item renders correctly, and
    // their ratings would land in the customer evaluation data (Ratings tab).
    allowRating: Boolean = true,
    evalViewModel: EvaluationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRatingDialog by remember { mutableStateOf(false) }
    val submitted by evalViewModel.submitted.collectAsState()

    // Once the rating is submitted, close the dialog and go back.
    LaunchedEffect(submitted) {
        if (submitted) {
            evalViewModel.resetSubmitted()
            onBack()
        }
    }

    // The rating dialog opens when the user presses Back.
    if (allowRating && showRatingDialog) {
        ARRatingDialog(
            jewelryName = jewelry.name,
            onSubmit = { rating, comment ->
                evalViewModel.submitRating(jewelry.id, jewelry.name, rating, comment)
            },
            onSkip = onBack,
            isLoading = evalViewModel.isLoading.collectAsState().value
        )
    }

    // AR needs the camera: check the permission before starting.
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Set once the user has said no. If Android then stops showing the prompt
    // ("Don't allow" twice), asking again does nothing - the only way forward is
    // the app's settings page, so the button switches to that.
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    val activity = context as? Activity
    val promptBlocked = cameraDenied && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraDenied = !granted
    }

    // Coming back from the settings page: pick up a permission granted there.
    val permissionLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(permissionLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasCameraPermission) {
                hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
        permissionLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { permissionLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for the camera permission on entry if it isn't granted yet.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Try On: ${jewelry.name}") },
                navigationIcon = {
                    // Back asks customers for a rating first, then leaves.
                    IconButton(onClick = { if (allowRating) showRatingDialog = true else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                ARCameraView(
                    jewelry = jewelry,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // No camera permission: explain why and offer a way to grant it.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (promptBlocked) {
                            "Camera access is turned off for Lumine. Turn it on in Settings → Permissions → Camera, then come back."
                        } else {
                            "AR try-on shows the jewelry on you through the camera. Nothing is recorded or uploaded."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        if (promptBlocked) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Text(if (promptBlocked) "Open Settings" else "Allow Camera")
                    }
                }
            }

            // Info card at the bottom - the item's details while trying it on.
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = jewelry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${jewelry.material.replaceFirstChar { it.uppercase() }} • ${jewelry.type.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatPeso(jewelry.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Rating dialog shown when a customer leaves the AR screen.
@Composable
fun ARRatingDialog(
    jewelryName: String,
    onSubmit: (rating: Int, comment: String?) -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean
) {
    var selectedRating by remember { mutableStateOf(0) }
    var comment        by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("How was your experience?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Rate the AR try-on for:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    jewelryName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(16.dp))

                // 5 stars - tap a star to set the rating.
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= selectedRating) Icons.Filled.Star
                                          else Icons.Outlined.StarBorder,
                            contentDescription = "$star stars",
                            tint = if (star <= selectedRating) Color(0xFFFFD700)
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedRating = star }
                        )
                    }
                }

                if (selectedRating > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (selectedRating) {
                            1 -> "Poor"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Very Good"
                            5 -> "Excellent!"
                            else -> ""
                        },
                        fontSize = 13.sp,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comments (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedRating > 0) onSubmit(selectedRating, comment) },
                enabled = selectedRating > 0 && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    )
}

// UnityPlayer can only be created once per process: libunity.so keeps its native
// state after pause(), and a second instance crashes with SIGSEGV. So it's kept here.
private var cachedUnityPlayer: Any? = null

// camera + AR layers composable - dalawang layers: camera at Unity
@Composable
fun ARCameraView(
    jewelry: Jewelry,
    modifier: Modifier = Modifier,
    jewelryVM: JewelryViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraManager: CameraManager? by remember { mutableStateOf(null) }
    var mediaPipeHelper: MediaPipeHelper? by remember { mutableStateOf(null) }
    var trackingActive by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }

    // second item from Try More (max 2 items)
    val secondJewelryState = remember { mutableStateOf<Jewelry?>(null) }
    var secondJewelry by secondJewelryState
    var tryMoreExpanded by remember { mutableStateOf(false) }

    val allJewelry by jewelryVM.jewelryList.collectAsState()
    // Any other type can be added; a second necklace would replace the main one (see pairsWith).
    val availableForSecond = allJewelry.filter(jewelry::pairsWith)

    // reuse the cached player, see cachedUnityPlayer
    val unityPlayer = remember {
        if (cachedUnityPlayer != null) {
            Log.d("ARScreen", "Reusing cached UnityPlayer")
            cachedUnityPlayer
        } else try {
            val cls = Class.forName("com.unity3d.player.UnityPlayer")

            // IUnityPlayerLifecycleEvents is a package-level interface (not inner class)
            val eventsInterface = Class.forName("com.unity3d.player.IUnityPlayerLifecycleEvents")

            // no-op proxy, we don't need Unity's lifecycle callbacks
            val eventsProxy = java.lang.reflect.Proxy.newProxyInstance(
                cls.classLoader,
                arrayOf(eventsInterface)
            ) { _, _, _ -> null }

            // Unity's constructor needs a Unity theme; with Theme.LumineApp it throws
            // NotFoundException (String resource ID #0x0). The translucent one also keeps
            // Unity's background see-through so the camera shows.
            val unityThemeResId = context.resources.getIdentifier(
                "UnityThemeSelector.Translucent", "style", context.packageName
            )
            val unityContext = if (unityThemeResId != 0) {
                Log.d("ARScreen", "Unity theme found (id=$unityThemeResId) - applying translucent theme")
                android.view.ContextThemeWrapper(context, unityThemeResId)
            } else {
                Log.w("ARScreen", "Unity theme not found in resources - using raw context")
                context
            }

            // Unity's native surface setup calls currentActivity.getWindow(). Without an
            // activity some GPUs get buffers in unsupported formats (Gralloc "isSupported
            // failed with 7") and the app crashes.
            try {
                val actField = cls.getDeclaredField("currentActivity")
                actField.isAccessible = true
                actField.set(null, context as? android.app.Activity)
                Log.d("ARScreen", "UnityPlayer.currentActivity set")
            } catch (e: Exception) {
                Log.w("ARScreen", "Could not set currentActivity: ${e.message}")
            }

            // Unity 2022.3 constructor: UnityPlayer(Context, IUnityPlayerLifecycleEvents)
            val ctor = cls.getConstructor(android.content.Context::class.java, eventsInterface)
            val player = ctor.newInstance(unityContext, eventsProxy)

            // Unity doesn't render until it has window focus
            cls.getMethod("windowFocusChanged", Boolean::class.javaPrimitiveType)
                .invoke(player, true)
            Log.d("ARScreen", "UnityPlayer created")
            cachedUnityPlayer = player
            player

        } catch (e: Exception) {
            // unwrap nested InvocationTargetExceptions
            var cause: Throwable? = e
            while (cause is java.lang.reflect.InvocationTargetException) {
                cause = cause.cause
            }
            Log.e("ARScreen", "UnityPlayer creation failed: ${cause?.javaClass?.simpleName}: ${cause?.message}")
            cause?.stackTrace?.take(12)?.forEachIndexed { i, frame ->
                Log.e("ARScreen", "  [$i] $frame")
            }
            null
        }
    }

    // The window background sits between the camera's SurfaceView (below the window)
    // and Unity's (above it), so it's transparent while this screen is open.
    // Keep PreviewView on its default SurfaceView: COMPATIBLE mode uses a TextureView,
    // which shares the window's GL context and crashes alongside Unity's.
    DisposableEffect("window_bg") {
        val activity = context as? android.app.Activity
        val window = activity?.window
        val prevBg = window?.decorView?.background
        window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        onDispose {
            window?.setBackgroundDrawable(prevBg)
        }
    }

    // Set up the AR pipeline (UnityBridge, MediaPipe) and tear it down on exit.
    DisposableEffect(Unit) {
        UnityBridge.initialize()

        unityPlayer?.let {
            try {
                it.javaClass.getMethod("resume").invoke(it)
                Log.d("ARScreen", "Unity resumed")
            } catch (e: Exception) {
                Log.w("ARScreen", "Unity resume failed: ${e.message}")
            }
        }

        // pick the model before tracking starts
        UnityBridge.setActiveModel(jewelry.type, jewelry.name, jewelry.modelUrl)

        try {
            mediaPipeHelper = MediaPipeHelper(context)
            trackingActive = true

            // secondJewelryState.value is read inside the callbacks so they see the current pick
            mediaPipeHelper?.onFaceDetected = { result ->
                val pt = jewelry.type.lowercase()
                val st = secondJewelryState.value?.type?.lowercase()
                if (pt == "necklace" || st == "necklace") UnityBridge.sendNecklacePosition(result)
                if (pt == "earring"  || st == "earring")  UnityBridge.sendEarringPositions(result)
            }
            mediaPipeHelper?.onHandDetected = { result ->
                val pt = jewelry.type.lowercase()
                val st = secondJewelryState.value?.type?.lowercase()
                if (pt == "ring"     || st == "ring")     UnityBridge.sendRingPosition(result)
                if (pt == "bracelet" || st == "bracelet") UnityBridge.sendBraceletPosition(result)
            }
        } catch (e: Exception) {
            Log.e(AR_TAG, "Couldn't send tracking results to Unity", e)
            trackingActive = false
        }

        onDispose {
            cameraManager?.shutdown()
            mediaPipeHelper?.close()
            // Remove second jewelry type before the full hide sweep
            secondJewelryState.value?.let { second -> UnityBridge.removeModel(second.type) }
            UnityBridge.hideRing()
            UnityBridge.hideEarrings()
            UnityBridge.hideBracelet()
            UnityBridge.hideNecklace()
            unityPlayer?.let {
                // Pause only. Calling destroy() from a Compose disposal crashes Unity's render
                // thread (SIGSEGV); its native memory is freed when the activity finishes.
                try {
                    it.javaClass.getMethod("pause").invoke(it)
                    Log.d("ARScreen", "Unity paused on AR exit")
                } catch (e: Exception) {
                    Log.w("ARScreen", "Unity pause on exit: ${e.message}")
                }
            }
        }
    }

    Box(modifier = modifier.background(Color.Transparent)) {

        // bottom layer: camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val manager = CameraManager(ctx, this, lifecycleOwner)
                    cameraManager = manager

                    // Keep the Compose isFrontCamera state in sync when the camera switches.
                    manager.onCameraFacingChanged = { front -> isFrontCamera = front }

                    manager.onFrameAvailable = { image, timestamp ->
                        try {
                            // this closure is created once, so read secondJewelryState.value on every frame
                            val primaryType = jewelry.type.lowercase()
                            val secondType  = secondJewelryState.value?.type?.lowercase()
                            val needsFace   = primaryType in listOf("necklace", "earring") ||
                                              secondType  in listOf("necklace", "earring")
                            val needsHand   = primaryType in listOf("ring", "bracelet") ||
                                              secondType  in listOf("ring", "bracelet")
                            if (needsFace) mediaPipeHelper?.detectFace(image, timestamp)
                            if (needsHand) mediaPipeHelper?.detectHand(image, timestamp)
                        } catch (e: Exception) {
                            Log.e(AR_TAG, "Couldn't run tracking on a camera frame", e)
                        }
                    }

                    manager.startCamera()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // middle layer: Unity. UnityPlayer is itself a FrameLayout; its SurfaceView gets
        // setZOrderMediaOverlay so it draws above the camera but under normal views.
        unityPlayer?.let { player ->
            AndroidView(
                factory = { _ ->
                    val unityView = player as android.view.View
                    unityView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    // Unity adds its SurfaceView asynchronously, so this runs again at 300 ms and 1 s.
                    fun applyZOrder(root: android.view.View) {
                        if (root is android.view.SurfaceView) {
                            root.setZOrderMediaOverlay(true)
                            // TRANSLUCENT rather than TRANSPARENT: some Adreno drivers treat TRANSPARENT as opaque.
                            root.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                            Log.d("ARScreen", "ZOrder+Format: setZOrderMediaOverlay + TRANSLUCENT on ${root.javaClass.simpleName}")
                        }
                        if (root is android.view.ViewGroup) {
                            for (i in 0 until root.childCount) applyZOrder(root.getChildAt(i))
                        }
                    }

                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    applyZOrder(unityView)                       // immediate (may be empty)
                    handler.postDelayed({ applyZOrder(unityView) }, 300L)   // after Unity init
                    handler.postDelayed({ applyZOrder(unityView) }, 1000L)  // safety retry

                    // so Unity gets touch input
                    unityView.isFocusableInTouchMode = true
                    unityView.requestFocus()

                    Log.d("ARScreen", "Unity view attached (class=${unityView.javaClass.simpleName})")
                    unityView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Try More: the button lists the other AR items; after one is picked, a chip with
        // an X takes its place. The second item is cleared on X or when leaving AR.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {

                // chip for the second item, X removes it
                secondJewelry?.let { second ->
                    AssistChip(
                        onClick = {},
                        label   = {
                            Text(
                                text     = "${second.name} • ${formatPeso(second.price)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                modifier = Modifier.widthIn(max = 220.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick  = {
                                    UnityBridge.removeModel(second.type)
                                    secondJewelry = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Close,
                                    contentDescription = "Remove ${second.name}",
                                    modifier           = Modifier.size(14.dp)
                                )
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                        colors   = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    )
                }

                // "Try More" FAB - hidden once the second slot is occupied
                if (secondJewelry == null && availableForSecond.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick        = { tryMoreExpanded = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Try More")
                    }

                    DropdownMenu(
                        expanded         = tryMoreExpanded,
                        onDismissRequest = { tryMoreExpanded = false }
                    ) {
                        Text(
                            text     = "Goes with this ${jewelry.type.lowercase()}",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        availableForSecond.forEach { item ->
                            DropdownMenuItem(
                                text    = {
                                    Column {
                                        Text(
                                            text       = item.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text  = "${item.type.replaceFirstChar { it.uppercase() }} • ${item.material.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    UnityBridge.addModel(item.type, item.name, item.modelUrl)
                                    secondJewelry   = item
                                    tryMoreExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // camera flip button (bottom-left)
        SmallFloatingActionButton(
            onClick = { cameraManager?.switchCamera() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 100.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector        = Icons.Default.FlipCameraAndroid,
                contentDescription = if (isFrontCamera) "Switch to back camera"
                                     else "Switch to front camera",
                modifier = Modifier.size(22.dp)
            )
        }

        // tracking status
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (trackingActive)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = if (trackingActive) "AR Tracking Active" else "Tracking Unavailable",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private const val AR_TAG = "ARScreen"
