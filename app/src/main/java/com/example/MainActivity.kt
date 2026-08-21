package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.core.storage.TokenManager
import com.example.data.local.BookoraDatabase
import com.example.data.local.FinancialDatabaseSeeder
import com.example.data.local.ReviewDatabaseSeeder
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.AuthorRepositoryImpl
import com.example.data.repository.BookRepositoryImpl
import com.example.data.repository.CategoryRepositoryImpl
import com.example.data.repository.LibraryRepositoryImpl
import com.example.data.repository.SearchRepositoryImpl
import com.example.data.repository.WishlistRepositoryImpl
import com.example.data.repository.financial.*
import com.example.data.repository.offline.OfflineBookRepositoryImpl
import com.example.data.repository.publisher.PublisherRepositoryImpl
import com.example.data.repository.review.ReviewRepositoryImpl
import com.example.domain.financial.PaymentRouter
import com.example.domain.publisher.PdfValidationService
import com.example.presentation.navigation.BookoraAppNavHost
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.BookDetailsViewModel
import com.example.presentation.viewmodel.ExploreViewModel
import com.example.presentation.viewmodel.HomeViewModel
import com.example.presentation.viewmodel.LibraryViewModel
import com.example.presentation.viewmodel.SearchViewModel
import com.example.presentation.viewmodel.ViewModelFactory
import com.example.presentation.viewmodel.WishlistViewModel
import com.example.presentation.viewmodel.financial.*
import com.example.ui.theme.BookoraTheme
import com.example.util.EnvironmentDiagnosticService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Log environment diagnostics to Logcat on application startup
        EnvironmentDiagnosticService.logStartupDiagnostics()

        // Clean Architecture & Dependency Injection Setup
        val database = BookoraDatabase.getInstance(applicationContext)
        val tokenManager = TokenManager(applicationContext)

        // Seed initial monetization, financial defaults and reviews
        FinancialDatabaseSeeder.seedDefaults(database)
        ReviewDatabaseSeeder.seedDefaults(database)

        val authRepository = AuthRepositoryImpl(database, tokenManager)
        val bookRepository = BookRepositoryImpl(database)
        val categoryRepository = CategoryRepositoryImpl(database)
        val authorRepository = AuthorRepositoryImpl(database)
        val searchRepository = SearchRepositoryImpl(database)
        val wishlistRepository = WishlistRepositoryImpl(database, authRepository)
        val libraryRepository = LibraryRepositoryImpl(database, authRepository)

        // Phase 5 Financial Repositories
        val entitlementRepository = EntitlementRepositoryImpl(database)
        val couponRepository = CouponRepositoryImpl(database)
        val cartRepository = CartRepositoryImpl(database, couponRepository)
        val orderRepository = OrderRepositoryImpl(database, couponRepository)

        // Resolve PaymentProvider via PaymentRouter using injected environment configuration
        val allowMockPayments = EnvironmentDiagnosticService.isMockPaymentsAllowed()
        val paymentProvider = PaymentRouter.resolvePaymentProvider(
            allowMockPayments = allowMockPayments
        )
        val paymentRepository = PaymentRepositoryImpl(
            database = database,
            paymentProvider = paymentProvider,
            entitlementRepository = entitlementRepository
        )
        val refundRepository = RefundRepositoryImpl(database = database, entitlementRepository = entitlementRepository)
        val subscriptionRepository = SubscriptionRepositoryImpl(database)
        val royaltyRepository = RoyaltyRepositoryImpl(database)
        val walletRepository = WalletRepositoryImpl(database)
        val payoutRepository = PayoutRepositoryImpl(database)
        val adminRepository = FinancialAdminRepositoryImpl(database)

        // Phase 8 Open Publisher & Rewards Repositories
        val publisherRepository = PublisherRepositoryImpl(database = database)
        val pdfValidationService = PdfValidationService(context = applicationContext)

        // Phase 9 Verified Reader & Trusted Reviews Repository
        val reviewRepository = ReviewRepositoryImpl(db = database)

        // Phase 10 Offline Room Cache Repository
        val offlineBookRepository = OfflineBookRepositoryImpl(db = database)

        val factory = ViewModelFactory(
            authRepo = authRepository,
            bookRepo = bookRepository,
            catRepo = categoryRepository,
            authorRepo = authorRepository,
            searchRepo = searchRepository,
            wishlistRepo = wishlistRepository,
            libRepo = libraryRepository,
            cartRepo = cartRepository,
            couponRepo = couponRepository,
            orderRepo = orderRepository,
            paymentRepo = paymentRepository,
            refundRepo = refundRepository,
            subRepo = subscriptionRepository,
            royaltyRepo = royaltyRepository,
            walletRepo = walletRepository,
            payoutRepo = payoutRepository,
            adminRepo = adminRepository,
            publisherRepo = publisherRepository,
            pdfValidationService = pdfValidationService,
            reviewRepo = reviewRepository,
            offlineBookRepo = offlineBookRepository,
            application = application
        )

        setContent {
            BookoraTheme {
                val navController = rememberNavController()

                val authViewModel: AuthViewModel = viewModel(factory = factory)
                val homeViewModel: HomeViewModel = viewModel(factory = factory)
                val exploreViewModel: ExploreViewModel = viewModel(factory = factory)
                val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
                val wishlistViewModel: WishlistViewModel = viewModel(factory = factory)
                val bookDetailsViewModel: BookDetailsViewModel = viewModel(factory = factory)

                BookoraAppNavHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    exploreViewModel = exploreViewModel,
                    libraryViewModel = libraryViewModel,
                    wishlistViewModel = wishlistViewModel,
                    bookDetailsViewModel = bookDetailsViewModel,
                    factory = factory
                )
            }
        }
    }
}
