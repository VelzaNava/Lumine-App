package com.thesis.lumine.ui.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.utils.CameraManager
import com.thesis.lumine.utils.MediaPipeHelper
import com.thesis.lumine.utils.AROverlayView
import com.thesis.lumine.utils.UnityBridge
import com.thesis.lumine.viewmodel.EvaluationViewModel

// AR screen — main screen ng try-on feature, may camera, Unity, at 2D overlay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    jewelry: Jewelry,
    onBack: () -> Unit,
    evalViewModel: EvaluationViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showRatingDialog by remember { mutableStateOf(false) }
    val submitted by evalViewModel.submitted.collectAsState()

    // pag na-submit na yung rating, i-dismiss yung dialog tapos balik sa catalog
    LaunchedEffect(submitted) {
        if (submitted) {
            evalViewModel.resetSubmitted()
            onBack()
        }
    }

    // ipakita yung rating dialog pag pinindot yung back button
    if (showRatingDialog) {
        ARRatingDialog(
            jewelryName = jewelry.name,
            onSubmit = { rating, comment ->
                evalViewModel.submitRating(jewelry.id, jewelry.name, rating, comment)
            },
            onSkip = onBack,
            isLoading = evalViewModel.isLoading.collectAsState().value
        )
    }

    // i-check muna kung may camera permission bago mag-start ng AR
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // i-request ang camera permission pag wala pa sa init
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
                    // back button nagti-trigger ng rating dialog muna bago umalis
                    IconButton(onClick = { showRatingDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                // pag walang permission, ipakita yung fallback UI para mag-grant
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
                        text = "Camera access is needed for AR try-on",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            // info card sa ibaba ng screen — ipakita yung jewelry details habang naka-AR
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                            text = "${jewelry.material.replaceFirstChar { it.uppercase() }} ${jewelry.type.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₱${String.format("%.2f", jewelry.price)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// rating dialog na lalabas pag mag-back ang user mula sa AR screen
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

                // 5 stars — i-click yung star para mag-set ng rating
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

                // ipakita yung label base sa piniling rating
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

                // optional comment field para sa additional feedback
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
            // submit button — enabled lang pag may napiling rating
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

// camera + AR layers composable — tatlong layers: camera, Unity, at 2D overlay
@Composable
fun ARCameraView(
    jewelry: Jewelry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraManager: CameraManager? by remember { mutableStateOf(null) }
    var mediaPipeHelper: MediaPipeHelper? by remember { mutableStateOf(null) }
    var overlayView: AROverlayView? by remember { mutableStateOf(null) }
    var trackingActive by remember { mutableStateOf(false) }

    // ── Unity Player ──────────────────────────────────────────────────────────
    // Created once via reflection so the app still compiles/runs without Unity.
    // Unity 2022.3 uses UnityPlayer(Context, IUnityPlayerLifecycleEvents).
    // Older versions use UnityPlayer(Context). We try both.
    val unityPlayer = remember {
        try {
            val cls = Class.forName("com.unity3d.player.UnityPlayer")

            // IUnityPlayerLifecycleEvents is a package-level interface (not inner class)
            val eventsInterface = Class.forName("com.unity3d.player.IUnityPlayerLifecycleEvents")

            // Create a no-op proxy — we don't need lifecycle callbacks from Unity
            val eventsProxy = java.lang.reflect.Proxy.newProxyInstance(
                cls.classLoader,
                arrayOf(eventsInterface)
            ) { _, _, _ -> null }

            // Unity's constructor requires a Unity-themed context.
            // Our app uses Theme.LumineApp (Material3) which Unity can't resolve,
            // causing NotFoundException: String resource ID #0x0.
            // Fix: wrap context with UnityThemeSelector.Translucent (from unityLibrary/res)
            // — this ALSO gives us a transparent Unity background so camera shows through! 🎯
            val unityThemeResId = context.resources.getIdentifier(
                "UnityThemeSelector.Translucent", "style", context.packageName
            )
            val unityContext = if (unityThemeResId != 0) {
                Log.d("ARScreen", "Unity theme found (id=$unityThemeResId) — applying translucent theme")
                android.view.ContextThemeWrapper(context, unityThemeResId)
            } else {
                Log.w("ARScreen", "Unity theme not found in resources — using raw context")
                context
            }

            // Give Unity's native code a real Activity reference BEFORE construction.
            // Unity's native GPU surface initialisation calls currentActivity.getWindow()
            // to obtain an ANativeWindow. Without this, it may fall back to a code path
            // that allocates AHardwareBuffers in unsupported pixel formats (0x38/0x3b),
            // causing Gralloc "isSupported failed with 7" and a native crash.
            try {
                val actField = cls.getDeclaredField("currentActivity")
                actField.isAccessible = true
                actField.set(null, context as? android.app.Activity)
                Log.d("ARScreen", "UnityPlayer.currentActivity set ✅")
            } catch (e: Exception) {
                Log.w("ARScreen", "Could not set currentActivity: ${e.message}")
            }

            // Unity 2022.3 constructor: UnityPlayer(Context, IUnityPlayerLifecycleEvents)
            val ctor = cls.getConstructor(android.content.Context::class.java, eventsInterface)
            val player = ctor.newInstance(unityContext, eventsProxy)

            // Tell Unity it has window focus so it starts rendering
            cls.getMethod("windowFocusChanged", Boolean::class.javaPrimitiveType)
                .invoke(player, true)
            Log.d("ARScreen", "UnityPlayer created successfully ✅")
            player

        } catch (e: Exception) {
            // Unwrap potentially nested InvocationTargetExceptions to get root cause
            var cause: Throwable? = e
            while (cause is java.lang.reflect.InvocationTargetException) {
                cause = cause.cause
            }
            Log.e("ARScreen", "UnityPlayer creation failed: ${cause?.javaClass?.simpleName}: ${cause?.message}")
            // Log stack frames so we can see exactly which Unity line throws
            cause?.stackTrace?.take(12)?.forEachIndexed { i, frame ->
                Log.e("ARScreen", "  [$i] $frame")
            }
            null
        }
    }

    // ── Make window background transparent ───────────────────────────────────
    // WHY: Camera PreviewView (SurfaceView) renders BELOW the Android window.
    // Unity SurfaceView (setZOrderMediaOverlay) renders ABOVE the window.
    // The window background color (Material3 theme = light blue) sits between
    // them and blocks the camera feed when Unity is transparent.
    //
    // Fix: set window background to transparent so the camera SurfaceView
    // shows through the window layer. Restored when leaving AR screen.
    //
    // NOTE: Do NOT use PreviewView.ImplementationMode.COMPATIBLE (TextureView).
    // TextureView uses the same GL context as the window. Unity also owns a GL
    // context (GLSurfaceView). Two GL contexts on the same thread = crash.
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

    // i-setup yung AR components — UnityBridge, MediaPipe, at i-cleanup pag umalis
    DisposableEffect(Unit) {
        // Initialize bridge — checks if Unity runtime class is on classpath
        UnityBridge.initialize()

        // Resume Unity rendering loop
        unityPlayer?.let {
            try {
                it.javaClass.getMethod("resume").invoke(it)
                Log.d("ARScreen", "Unity resumed")
            } catch (e: Exception) {
                Log.w("ARScreen", "Unity resume failed: ${e.message}")
            }
        }

        // i-initialize yung MediaPipe helper para sa hand/face tracking
        try {
            mediaPipeHelper = MediaPipeHelper(context)
            trackingActive = true
        } catch (e: Exception) {
            e.printStackTrace()
            trackingActive = false
        }

        onDispose {
            // i-cleanup lahat ng resources pag lumalabas ng AR screen
            cameraManager?.shutdown()
            mediaPipeHelper?.close()
            // Hide jewelry overlays in Unity when leaving AR screen
            UnityBridge.hideRing()
            UnityBridge.hideNecklace()
            // Pause then destroy Unity player
            unityPlayer?.let {
                // Only pause — do NOT call destroy().
                // Unity's native render thread (libunity.so) throws SIGSEGV when destroy()
                // is called mid-session from a Compose disposal (wrong thread timing).
                // The process will reclaim native resources when the Activity finishes.
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

        // ── LAYER 1: Camera Preview (bottom) ─────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val manager = CameraManager(ctx, this, lifecycleOwner)
                    cameraManager = manager

                    manager.onFrameAvailable = { image, timestamp ->
                        try {
                            when (jewelry.type.lowercase()) {
                                "necklace", "earring" -> {
                                    mediaPipeHelper?.detectFace(image, timestamp)
                                }
                                "ring", "bracelet" -> {
                                    mediaPipeHelper?.detectHand(image, timestamp)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    manager.startCamera()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── LAYER 2: Unity 3D Rendering (middle) ─────────────────────────────
        // UnityPlayer IS a FrameLayout (confirmed: UnityPlayerActivity calls
        // setContentView(mUnityPlayer) directly — there is no getView() in Unity 2022.x).
        //
        // SurfaceView Z-ordering for AR:
        //   - Camera PreviewView (SurfaceView, default Z) → below window → bottom layer
        //   - Unity GLSurfaceView (setZOrderMediaOverlay) → above camera SurfaceView → middle
        //   - 2D overlay / Cards (normal Views) → window level → top layer
        // With Unity background = transparent, camera is visible through Unity. ✅
        unityPlayer?.let { player ->
            AndroidView(
                factory = { _ ->
                    // Cast player directly — UnityPlayer extends FrameLayout, it IS the view.
                    val unityView = player as android.view.View
                    unityView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                    // Traverse view tree and apply setZOrderMediaOverlay to every SurfaceView.
                    // Unity creates its inner GLSurfaceView asynchronously, so we retry
                    // at 0 ms (immediate), 300 ms, and 1000 ms to be sure we catch it.
                    fun applyZOrder(root: android.view.View) {
                        if (root is android.view.SurfaceView) {
                            root.setZOrderMediaOverlay(true)
                            // Explicitly request RGBA_8888 (32-bit with 8-bit alpha).
                            // PixelFormat.TRANSPARENT (-2) is a system hint that some Adreno
                            // drivers interpret as opaque, whereas RGBA_8888 (1) is unambiguous:
                            // SurfaceFlinger receives an alpha-capable layer and composites
                            // Unity transparently over the camera SurfaceView.
                            root.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
                            Log.d("ARScreen", "ZOrder+Format: setZOrderMediaOverlay + TRANSLUCENT ✅ on ${root.javaClass.simpleName}")
                        }
                        if (root is android.view.ViewGroup) {
                            for (i in 0 until root.childCount) applyZOrder(root.getChildAt(i))
                        }
                    }

                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    applyZOrder(unityView)                       // immediate (may be empty)
                    handler.postDelayed({ applyZOrder(unityView) }, 300L)   // after Unity init
                    handler.postDelayed({ applyZOrder(unityView) }, 1000L)  // safety retry

                    // Request focus so Unity receives touch / input events
                    unityView.isFocusableInTouchMode = true
                    unityView.requestFocus()

                    Log.d("ARScreen", "Unity view attached (class=${unityView.javaClass.simpleName})")
                    unityView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── LAYER 3: 2D AR Overlay (top) ─────────────────────────────────────
        // Canvas-based dots/lines drawn on top of everything.
        // Ring = yellow circle at finger position
        // Necklace = yellow line across jaw + pendant dot
        if (trackingActive) {
            AndroidView(
                factory = { ctx ->
                    AROverlayView(ctx).apply {
                        overlayView = this
                        setJewelryType(jewelry.type)

                        mediaPipeHelper?.onFaceDetected = { result ->
                            // i-update yung 2D overlay landmarks para sa necklace at earring
                            updateFaceLandmarks(result)
                            // i-send din sa Unity para sa 3D jewelry overlay
                            when (jewelry.type.lowercase()) {
                                "necklace", "earring" -> UnityBridge.sendNecklacePosition(result)
                            }
                        }

                        mediaPipeHelper?.onHandDetected = { result ->
                            // i-update yung 2D hand overlay para sa ring at bracelet
                            updateHandLandmarks(result)
                            // i-send yung ring finger position sa Unity para sa 3D ring
                            when (jewelry.type.lowercase()) {
                                "ring", "bracelet" -> UnityBridge.sendRingPosition(result)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Tracking Status Indicator ─────────────────────────────────────────
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
