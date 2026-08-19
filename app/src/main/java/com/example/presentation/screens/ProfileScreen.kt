package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UserRole
import com.example.presentation.components.BookoraTopBar
import com.example.presentation.viewmodel.AuthViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishPrimaryLight
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateToAuthorStudio: () -> Unit,
    onNavigateToAdminControl: () -> Unit,
    onNavigateToOrderHistory: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToAuthorFinancial: () -> Unit = {},
    onNavigateToAdminFinancial: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToUploadBook: () -> Unit = {},
    onNavigateToCreatorEarnings: () -> Unit = {},
    onNavigateToMyUploads: () -> Unit = {},
    onNavigateToAdminModeration: () -> Unit = {},
    onNavigateToAdminReviewModeration: () -> Unit = {},
    onNavigateToLogin: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .verticalScroll(rememberScrollState())
    ) {
        BookoraTopBar(
            title = "Account & Profile",
            subtitle = "Manage reading preferences and ecosystem roles"
        )

        // User Profile Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, PolishSlate100),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    color = PolishPrimaryLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PolishPrimaryIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.fullName ?: "Alex Mercer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate900
                    )
                    Text(
                        text = currentUser?.email ?: "alex.mercer@bookora.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = PolishSlate500
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = PolishPrimaryLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ROLE: ${currentUser?.role?.name ?: "READER"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimaryIndigo
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Reading Stats Row
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Reading Performance",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                icon = Icons.Default.AutoStories,
                value = "14",
                label = "Books Read",
                tint = PolishPrimaryIndigo,
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                icon = Icons.Default.LocalFireDepartment,
                value = "18 Days",
                label = "Daily Streak",
                tint = PolishAccentOrange,
                modifier = Modifier.weight(1f)
            )
            ProfileStatCard(
                icon = Icons.Default.Timer,
                value = "42.5 hrs",
                label = "Time Read",
                tint = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        // Purchases & Marketplace Section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Purchases & Marketplace",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, PolishSlate100)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "Order History & Receipts",
                    subtitle = "View completed purchases, invoices, and refunds",
                    onClick = onNavigateToOrderHistory,
                    testTag = "menu_order_history"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.CardMembership,
                    title = "Subscriptions & Memberships",
                    subtitle = "Bookora Unlimited pass & VIP tiers",
                    onClick = onNavigateToSubscriptions,
                    testTag = "menu_subscriptions"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.ShoppingBag,
                    title = "Shopping Cart",
                    subtitle = "Review and checkout queued books & bundles",
                    onClick = onNavigateToCart,
                    testTag = "menu_cart"
                )
            }
        }

        // Ecosystem Portals Section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Open Publisher & Creator Rewards (₹1/Book)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, PolishSlate100)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.AutoStories,
                    title = "Submit PDF Book (Earn ₹1)",
                    subtitle = "Upload manuscripts for moderation and publication",
                    onClick = onNavigateToUploadBook,
                    testTag = "menu_upload_book"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Creator Balance & Payouts",
                    subtitle = "View ₹1 approval rewards, ledger, and withdraw to UPI",
                    onClick = onNavigateToCreatorEarnings,
                    testTag = "menu_creator_earnings"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "My Uploaded Books",
                    subtitle = "Track submission status, review feedback & approvals",
                    onClick = onNavigateToMyUploads,
                    testTag = "menu_my_uploads"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Open Publisher Moderation",
                    subtitle = "Review pending manuscripts, credit ₹1, and approve payouts",
                    onClick = onNavigateToAdminModeration,
                    testTag = "menu_admin_moderation"
                )
            }
        }

        // Ecosystem Portals Section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ecosystem Portals",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, PolishSlate100)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Author Studio",
                    subtitle = "Publish books, manage drafts, view sales & analytics",
                    onClick = onNavigateToAuthorStudio,
                    testTag = "menu_author_studio"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Author Wallet & Royalties",
                    subtitle = "Track royalty ledger, available balance & payouts",
                    onClick = onNavigateToAuthorFinancial,
                    testTag = "menu_author_wallet"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Control Center",
                    subtitle = "Marketplace GMV, book approvals, copyright claims",
                    onClick = onNavigateToAdminControl,
                    testTag = "menu_admin_control"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.AttachMoney,
                    title = "Financial Operations Center",
                    subtitle = "Process payouts, handle refund disputes, platform take rate",
                    onClick = onNavigateToAdminFinancial,
                    testTag = "menu_admin_financial"
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.RateReview,
                    title = "Review Moderation Center",
                    subtitle = "Phase 9: Trusted reviews, flag investigation & reader badges",
                    onClick = onNavigateToAdminReviewModeration,
                    testTag = "menu_admin_review_moderation"
                )
            }
        }

        // Settings Section
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Application Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PolishSlate900,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, PolishSlate100)
        ) {
            Column {
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Reading & Display Options",
                    subtitle = "Font sizing, page layout, reading themes",
                    onClick = {}
                )
                HorizontalDivider(color = PolishSlate100)
                ProfileMenuItem(
                    icon = Icons.Default.Security,
                    title = "Security & Privacy",
                    subtitle = "Two-factor auth, session management",
                    onClick = {}
                )
            }
        }

        // Logout Button
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                authViewModel.logout()
                onNavigateToLogin()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFEE2E2),
                contentColor = Color(0xFFDC2626)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("logout_button")
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ProfileStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishSlate100)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PolishSlate900
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PolishSlate500
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = PolishSlate100,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PolishPrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PolishSlate900
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PolishSlate500
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PolishSlate400,
            modifier = Modifier.size(20.dp)
        )
    }
}
