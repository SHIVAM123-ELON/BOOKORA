package com.example.presentation.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.presentation.screens.AdminScreen
import com.example.presentation.screens.AuthorStudioScreen
import com.example.presentation.screens.BookDetailsScreen
import com.example.presentation.screens.ExploreScreen
import com.example.presentation.screens.HomeScreen
import com.example.presentation.screens.LibraryScreen
import com.example.presentation.screens.LoginScreen
import com.example.presentation.screens.ProfileScreen
import com.example.presentation.screens.ReaderScreen
import com.example.presentation.screens.RegisterScreen
import com.example.presentation.screens.WishlistScreen
import com.example.presentation.screens.financial.*
import com.example.presentation.screens.publisher.*
import com.example.presentation.screens.review.*
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.BookDetailsViewModel
import com.example.presentation.viewmodel.ExploreViewModel
import com.example.presentation.viewmodel.HomeViewModel
import com.example.presentation.viewmodel.LibraryViewModel
import com.example.presentation.viewmodel.ViewModelFactory
import com.example.presentation.viewmodel.WishlistViewModel
import com.example.presentation.viewmodel.financial.*
import com.example.presentation.viewmodel.publisher.*
import com.example.presentation.viewmodel.review.*
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryContainer
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate400

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home, "bottom_nav_home"),
    BottomNavItem("Explore", Screen.Explore.route, Icons.Default.Explore, "bottom_nav_explore"),
    BottomNavItem("Library", Screen.Library.route, Icons.Default.AutoStories, "bottom_nav_library"),
    BottomNavItem("Wishlist", Screen.Wishlist.route, Icons.Default.Favorite, "bottom_nav_wishlist"),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person, "bottom_nav_profile")
)

@Composable
fun BookoraAppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    exploreViewModel: ExploreViewModel,
    libraryViewModel: LibraryViewModel,
    wishlistViewModel: WishlistViewModel,
    bookDetailsViewModel: BookDetailsViewModel,
    factory: ViewModelFactory,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomNavVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Library.route,
        Screen.Wishlist.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PolishBackground,
        bottomBar = {
            if (isBottomNavVisible) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, PolishSlate100)
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PolishPrimaryIndigo,
                                    selectedTextColor = PolishPrimaryIndigo,
                                    unselectedIconColor = PolishSlate400,
                                    unselectedTextColor = PolishSlate400,
                                    indicatorColor = PolishPrimaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth routes
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = {}
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }

            // Main App Tabs
            composable(Screen.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onNavigateToBookDetails = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    },
                    onNavigateToReader = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    },
                    onNavigateToExplore = {
                        navController.navigate(Screen.Explore.route)
                    },
                    onNavigateToAuthorStudio = {
                        navController.navigate(Screen.AuthorStudio.route)
                    },
                    onNavigateToCart = {
                        navController.navigate(Screen.Cart.route)
                    },
                    onNavigateToSubscriptions = {
                        navController.navigate(Screen.Subscriptions.route)
                    }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    exploreViewModel = exploreViewModel,
                    onNavigateToBookDetails = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    onNavigateToReader = { bookId ->
                        navController.navigate(Screen.Reader.createRoute(bookId))
                    },
                    onNavigateToExplore = {
                        navController.navigate(Screen.Explore.route)
                    }
                )
            }

            composable(Screen.Wishlist.route) {
                WishlistScreen(
                    wishlistViewModel = wishlistViewModel,
                    onNavigateToBookDetails = { bookId ->
                        navController.navigate(Screen.BookDetails.createRoute(bookId))
                    },
                    onNavigateToExplore = {
                        navController.navigate(Screen.Explore.route)
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onNavigateToAuthorStudio = { navController.navigate(Screen.AuthorStudio.route) },
                    onNavigateToAdminControl = { navController.navigate(Screen.AdminControl.route) },
                    onNavigateToOrderHistory = { navController.navigate(Screen.OrderHistory.route) },
                    onNavigateToSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                    onNavigateToAuthorFinancial = { navController.navigate(Screen.AuthorFinancial.route) },
                    onNavigateToAdminFinancial = { navController.navigate(Screen.AdminFinancial.route) },
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                    onNavigateToUploadBook = { navController.navigate(Screen.UploadBook.route) },
                    onNavigateToCreatorEarnings = { navController.navigate(Screen.CreatorEarnings.route) },
                    onNavigateToMyUploads = { navController.navigate(Screen.MyUploads.route) },
                    onNavigateToAdminModeration = { navController.navigate(Screen.AdminModeration.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }

            // Book Details
            composable(
                route = Screen.BookDetails.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                BookDetailsScreen(
                    bookId = bookId,
                    bookDetailsViewModel = bookDetailsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReader = { id ->
                        navController.navigate(Screen.Reader.createRoute(id))
                    },
                    onNavigateToCheckout = { id ->
                        navController.navigate(Screen.Checkout.createRoute(bookId = id))
                    },
                    onNavigateToCart = {
                        navController.navigate(Screen.Cart.route)
                    }
                )
            }

            // Reader
            composable(
                route = Screen.Reader.route,
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                ReaderScreen(
                    bookId = bookId,
                    bookDetailsViewModel = bookDetailsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Author Studio
            composable(Screen.AuthorStudio.route) {
                AuthorStudioScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFinancials = { navController.navigate(Screen.AuthorFinancial.route) }
                )
            }

            // Admin Control
            composable(Screen.AdminControl.route) {
                AdminScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFinancialOperations = { navController.navigate(Screen.AdminFinancial.route) }
                )
            }

            // ==========================================
            // PHASE 5 FINANCIAL & MONETIZATION ROUTES
            // ==========================================

            // Shopping Cart
            composable(Screen.Cart.route) {
                val cartViewModel: CartViewModel = viewModel(factory = factory)
                CartScreen(
                    viewModel = cartViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCheckout = {
                        navController.navigate(Screen.Checkout.createRoute())
                    },
                    onNavigateToBrowse = {
                        navController.navigate(Screen.Explore.route)
                    }
                )
            }

            // Checkout & Payment
            composable(
                route = Screen.Checkout.route,
                arguments = listOf(
                    navArgument("bookId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("couponCode") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val checkoutViewModel: CheckoutViewModel = viewModel(factory = factory)
                val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                val couponCode = backStackEntry.arguments?.getString("couponCode") ?: ""

                LaunchedEffect(bookId, couponCode) {
                    if (bookId.isNotBlank()) {
                        checkoutViewModel.prepareBuyNowOrder(bookId, couponCode.ifBlank { null })
                    } else {
                        checkoutViewModel.prepareCartOrder(couponCode.ifBlank { null })
                    }
                }

                CheckoutScreen(
                    viewModel = checkoutViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToOrderHistory = {
                        navController.navigate(Screen.OrderHistory.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            // Order History & Receipts
            composable(Screen.OrderHistory.route) {
                val orderHistoryViewModel: OrderHistoryViewModel = viewModel(factory = factory)
                OrderHistoryScreen(
                    viewModel = orderHistoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Subscriptions & Memberships
            composable(Screen.Subscriptions.route) {
                val subscriptionViewModel: SubscriptionViewModel = viewModel(factory = factory)
                SubscriptionsScreen(
                    viewModel = subscriptionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Author Financials, Royalties & Wallet
            composable(Screen.AuthorFinancial.route) {
                val authorEarningsViewModel: AuthorEarningsViewModel = viewModel(factory = factory)
                AuthorFinancialScreen(
                    viewModel = authorEarningsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Admin Financial Center
            composable(Screen.AdminFinancial.route) {
                val adminFinancialViewModel: AdminFinancialViewModel = viewModel(factory = factory)
                AdminFinancialScreen(
                    viewModel = adminFinancialViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Phase 8: Open Publisher & Creator ₹1 Reward System
            composable(Screen.UploadBook.route) {
                val uploadBookViewModel: UploadBookViewModel = viewModel(factory = factory)
                UploadBookScreen(
                    viewModel = uploadBookViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEarnings = { navController.navigate(Screen.CreatorEarnings.route) },
                    onNavigateToMyUploads = { navController.navigate(Screen.MyUploads.route) }
                )
            }

            composable(Screen.CreatorEarnings.route) {
                val creatorEarningsViewModel: CreatorEarningsViewModel = viewModel(factory = factory)
                CreatorEarningsScreen(
                    viewModel = creatorEarningsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpload = { navController.navigate(Screen.UploadBook.route) }
                )
            }

            composable(Screen.MyUploads.route) {
                val creatorEarningsViewModel: CreatorEarningsViewModel = viewModel(factory = factory)
                MyUploadsScreen(
                    viewModel = creatorEarningsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToUpload = { navController.navigate(Screen.UploadBook.route) }
                )
            }

            composable(Screen.AdminModeration.route) {
                val adminModerationViewModel: AdminModerationViewModel = viewModel(factory = factory)
                AdminModerationScreen(
                    viewModel = adminModerationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

