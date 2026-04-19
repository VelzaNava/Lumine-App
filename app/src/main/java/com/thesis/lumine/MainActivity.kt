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
import com.thesis.lumine.ui.auth.LandingScreen
import com.thesis.lumine.ui.auth.LoginScreen
import com.thesis.lumine.ui.auth.OtpVerificationScreen
import com.thesis.lumine.ui.auth.RegisterScreen
import com.thesis.lumine.ui.catalog.CatalogScreen
import com.thesis.lumine.ui.catalog.ProductDetailScreen
import com.thesis.lumine.ui.profile.EditProfileScreen
import com.thesis.lumine.ui.profile.FavoritesScreen
import com.thesis.lumine.ui.profile.ProfileSavedScreen
import com.thesis.lumine.ui.profile.ProfileScreen
import com.thesis.lumine.ui.theme.LumineAppTheme
import com.thesis.lumine.viewmodel.AuthState
import com.thesis.lumine.viewmodel.AuthViewModel
import com.thesis.lumine.viewmodel.ProfileViewModel

class MainActivity : ComponentActivity() {
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

@Composable
fun LumineNavigation() {
    val navController    = rememberNavController()
    val authViewModel: AuthViewModel       = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val favoriteIds by profileViewModel.favoriteIds.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Success ->
            if ((authState as AuthState.Success).authResponse.isAdmin) "admin" else "catalog"
        else -> "landing"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // landing
        composable("landing") {
            LandingScreen(
                onLoginClick  = { navController.navigate("login") },
                onSignUpClick = { navController.navigate("register") }
            )
        }

        // login
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

        // register
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onOtpSent = { navController.navigate("otp-verify") },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // OTP verify — back goes to landing (not register) so user starts fresh
        composable("otp-verify") {
            OtpVerificationScreen(
                viewModel = authViewModel,
                onVerified = { isAdmin ->
                    navController.navigate(if (isAdmin) "admin" else "catalog") {
                        popUpTo("landing") { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigate("landing") {
                        popUpTo("landing") { inclusive = true }
                    }
                }
            )
        }

        // catalog — clicking a card goes to product detail, not directly to AR
        composable("catalog") {
            CatalogScreen(
                profileViewModel  = profileViewModel,
                onProfileClicked  = { navController.navigate("profile") },
                onJewelrySelected = { jewelry ->
                    val jewelryJson = Gson().toJson(jewelry)
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("product-detail/$encoded")
                }
            )
        }

        // product detail — between catalog and AR
        composable(
            route = "product-detail/{jewelry}",
            arguments = listOf(navArgument("jewelry") { type = NavType.StringType })
        ) { backStackEntry ->
            val jewelryJson = backStackEntry.arguments?.getString("jewelry") ?: ""
            val decoded = java.net.URLDecoder.decode(jewelryJson, "UTF-8")
            val jewelry = Gson().fromJson(decoded, Jewelry::class.java)
            val isFav = jewelry.id in favoriteIds

            ProductDetailScreen(
                jewelry          = jewelry,
                isFavorite       = isFav,
                onToggleFavorite = { profileViewModel.toggleFavorite(jewelry.id) },
                onTryOn          = {
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("ar/$encoded")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // profile
        composable("profile") {
            ProfileScreen(
                viewModel       = profileViewModel,
                onEditProfile   = { navController.navigate("edit-profile") },
                onViewFavorites = { navController.navigate("favorites") },
                onBackToCatalog = { navController.navigate("catalog") { popUpTo("profile") { inclusive = true } } },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("landing") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("edit-profile") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onBack  = { navController.popBackStack() },
                onSaved = { navController.navigate("profile-saved") { popUpTo("edit-profile") { inclusive = true } } }
            )
        }

        composable("profile-saved") {
            ProfileSavedScreen(
                onBackToProfile = {
                    navController.navigate("profile") { popUpTo("profile") { inclusive = true } }
                }
            )
        }

        // favorites — clicking goes to product detail
        composable("favorites") {
            FavoritesScreen(
                profileViewModel = profileViewModel,
                onBack           = { navController.popBackStack() },
                onJewelrySelected = { jewelry ->
                    val jewelryJson = Gson().toJson(jewelry)
                    val encoded = java.net.URLEncoder.encode(jewelryJson, "UTF-8")
                    navController.navigate("product-detail/$encoded")
                }
            )
        }

        // AR screen
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

        // admin
        composable("admin") {
            AdminScreen(
                profileViewModel = profileViewModel,
                onBack = {
                    authViewModel.logout()
                    navController.navigate("landing") { popUpTo("admin") { inclusive = true } }
                }
            )
        }
    }
}
