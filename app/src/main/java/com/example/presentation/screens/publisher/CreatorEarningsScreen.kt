package com.example.presentation.screens.publisher

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.publisher.BookSubmission
import com.example.domain.model.publisher.SubmissionStatus
import com.example.presentation.viewmodel.publisher.CreatorEarningsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorEarningsScreen(
    viewModel: CreatorEarningsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val balance = state.balance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Wallet & Rewards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToUpload) {
                        Icon(Icons.Default.Add, contentDescription = "Upload Book")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Main Balance Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Available Reward Balance",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "₹1 / Approved Book",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            balance?.formattedAvailable ?: "₹0.00",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Lifetime Earned: ${balance?.formattedLifetime ?: "₹0.00"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (balance?.isFrozen == true) {
                                Text(
                                    "Wallet Locked",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { viewModel.openWithdrawalDialog() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("request_payout_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = (balance?.availableBalanceMinor ?: 0L) >= 5000L && balance?.isFrozen != true
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Withdraw to UPI (Min ₹50)")
                        }
                    }
                }
            }

            // Book Metrics Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Approved",
                        count = balance?.totalApprovedBooks ?: 0,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "In Review",
                        count = balance?.totalPendingBooks ?: 0,
                        color = Color(0xFFE65100),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Rejected",
                        count = balance?.totalRejectedBooks ?: 0,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Success or Info Notices
            if (state.withdrawalSuccessMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.withdrawalSuccessMessage!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Payout Requests Section
            if (state.payoutRequests.isNotEmpty()) {
                item {
                    Text(
                        "Recent Payout Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.payoutRequests) { req ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.formattedAmount, fontWeight = FontWeight.Bold)
                                Text("UPI: ${req.upiId}", style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(
                                color = when (req.status.name) {
                                    "PAID" -> Color(0xFF2E7D32)
                                    "REJECTED", "FAILED" -> MaterialTheme.colorScheme.error
                                    else -> Color(0xFFE65100)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    req.status.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Reward Transaction Ledger Section
            item {
                Text(
                    "Reward Transaction Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions yet. Publish verified books to earn ₹1 rewards!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.transactions) { tx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Idempotency: ${tx.idempotencyKey.take(18)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                tx.formattedAmount,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.amountMinor >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Withdrawal Request Modal
        if (state.isWithdrawalDialogOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.closeWithdrawalDialog() },
                title = { Text("Request UPI Withdrawal", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Available: ${balance?.formattedAvailable} (Min ₹50)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = state.withdrawalAmountText,
                            onValueChange = { viewModel.onWithdrawalAmountChanged(it) },
                            label = { Text("Amount (₹)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.upiIdText,
                            onValueChange = { viewModel.onUpiIdChanged(it) },
                            label = { Text("UPI ID (e.g. name@okhdfcbank)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.errorMessage != null) {
                            Text(
                                state.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.submitWithdrawalRequest() },
                        enabled = !state.isRequestingWithdrawal
                    ) {
                        Text(if (state.isRequestingWithdrawal) "Submitting..." else "Submit Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeWithdrawalDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
