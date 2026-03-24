package com.thesis.lumine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.ui.admin.AdminScreen
import com.thesis.lumine.ui.ar.ARScreen
import com.thesis.lumine.ui.auth.LoginScreen
import com.thesis.lumine.ui.auth.OtpVerificationScreen
import com.thesis.lumine.ui.auth.RegisterScreen
import com.thesis.lumine.ui.catalog.CatalogScreen
import com.thesis.lumine.ui.profile.EditProfileScreen
import com.thesis.lumine.ui.profile.FavoritesScreen
import com.thesis.lumine.ui.profile.ProfileSavedScreen
import com.thesis.lumine.ui.profile.ProfileScreen
import com.thesis.lumine.ui.theme.LumineAppTheme
import com.thesis.lumine.viewmodel.AuthState
import com.thesis.lumine.viewmodel.AuthViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

class MainActivity : ComponentActivity() {
    // entry point ng app — i-setup yung UI at i-enable yung edge-to-edge display
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumineAppTheme {
                LumineNavigation()
            }
        }
    }
}

// main nav composable — dito nagde-decide kung saan mag-redirect depende sa auth state
@Composable
fun LumineNavigation() {
    val navController   = rememberNavController()
    val authViewModel: AuthViewModel     = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    // i-check kung admin o regular user tapos pumunta sa tamang screen
    val startDestination = when (authState) {
        is AuthState.Success ->
            if ((authState as AuthState.Success).authResponse.isAdmin) "admin" else "catalog"
        else -> "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // login screen — after success, i-redirect sa catalog o admin depende sa role
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { isAdmin ->
                    navController.navigate(if (isAdmin) "admin" else "catalog") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // register screen — pagkatapos mag-submit, pumunta sa OTP verification
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onOtpSent = { navController.navigate("otp-verify") },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // OTP screen — i-verify yung code tapos redirect sa tamang home screen
        composable("otp-verify") {
            OtpVerificationScreen(
                viewModel = authViewModel,
                onVerified = { isAdmin ->
                    navController.navigate(if (isAdmin) "admin" else "catalog") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // catalog screen — i-encode yung jewelry as JSON para ma-pass sa AR screen
        composable("catalog") {
            CatalogScreen(
                profileViewModel = profileViewModel,
                onProfileClicked = { navController.navigate("profile") },
                onJewelrySelected = { jewelry ->
                    val jewelryJson = Gson().toJson(jewelry)
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("ar/$encoded")
                }
            )
        }

        // profile screen — pag nag-logout, i-clear lahat at balik sa login
        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                onEditProfile   = { navController.navigate("edit-profile") },
                onViewFavorites = { navController.navigate("favorites") },
                onBackToCatalog = { navController.navigate("catalog") { popUpTo("profile") { inclusive = true } } },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // edit profile — pag na-save, mag-navigate sa success screen
        composable("edit-profile") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onBack  = { navController.popBackStack() },
                onSaved = { navController.navigate("profile-saved") { popUpTo("edit-profile") { inclusive = true } } }
            )
        }

        // profile saved confirmation screen — babalik sa profile after nito
        composable("profile-saved") {
            ProfileSavedScreen(
                onBackToProfile = {
                    navController.navigate("profile") { popUpTo("profile") { inclusive = true } }
                }
            )
        }

        // favorites screen — pag nag-click ng item, i-encode tapos pumunta sa AR
        composable("favorites") {
            FavoritesScreen(
                profileViewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onJewelrySelected = { jewelry ->
                    val jewelryJson = Gson().toJson(jewelry)
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("ar/$encoded")
                }
            )
        }

        // AR screen — i-decode yung jewelry JSON galing sa nav args tapos i-pass sa ARScreen
        composable(
            route = "ar/{jewelry}",
            arguments = listOf(navArgument("jewelry") { type = NavType.StringType })
        ) { backStackEntry ->
            val jewelryJson = backStackEntry.arguments?.getString("jewelry") ?: ""
            val decoded = java.net.URLDecoder.decode(jewelryJson, "UTF-8")
            val jewelry = Gson().fromJson(decoded, Jewelry::class.java)
            ARScreen(
                jewelry = jewelry,
                onBack  = { navController.popBackStack() }
            )
        }

        // admin screen — pag nag-back, i-logout tapos balik sa login
        composable("admin") {
            AdminScreen(
                profileViewModel = profileViewModel,
                onBack = {
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo("admin") { inclusive = true } }
                }
            )
        }
    }
}
