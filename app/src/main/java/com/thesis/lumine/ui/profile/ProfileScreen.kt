package com.thesis.lumine.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.thesis.lumine.utils.SessionManager
import com.thesis.lumine.viewmodel.ProfileViewModel

// profile screen — ipakita yung user info at mga options para sa account
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onViewFavorites: () -> Unit,
    onBackToCatalog: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context       = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val profile       by viewModel.profile.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()

    // i-load yung profile data pagka-open ng screen
    LaunchedEffect(Unit) { viewModel.loadProfile() }

    // image picker launcher — mag-oopen ng gallery picker pag pinayagan na
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    // i-check yung tamang permission depende sa Android version
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    // permission launcher — pag granted, i-launch yung image picker agad
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imageLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackToCatalog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Catalog")
                    }
                }
            )
        }
    ) { padding ->
        // pag loading pa yung profile, ipakita muna yung spinner
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // avatar section — clickable para mag-upload ng bagong photo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable { permissionLauncher.launch(permission) },
                contentAlignment = Alignment.Center
            ) {
                if (!profile.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                } else {
                    // default avatar — person icon pag wala pang photo
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // camera badge na naka-overlay sa lower-right ng avatar
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Tap to change photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = profile.username.ifBlank { "(Username)" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // i-build yung full name — i-combine lang kung may laman
            val fullName = listOf(profile.firstName, profile.lastName)
                .filter { it.isNotBlank() }.joinToString(" ")
            if (fullName.isNotBlank())
                Text(text = fullName, style = MaterialTheme.typography.bodyLarge)

            // kunin yung email galing sa session — hindi nasa profile response
            val email = sessionManager.getUserEmail() ?: ""
            if (email.isNotBlank())
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            if (!profile.mobileNumber.isNullOrBlank())
                Text(
                    text = profile.mobileNumber!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Edit") }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onViewFavorites,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("View Favorites") }

            Spacer(Modifier.weight(1f))

            // logout button — red design para malinaw na destructive action ito
            Surface(
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, Color(0xFFD32F2F)),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onLogout,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(3.dp)
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
