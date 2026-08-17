package com.example.presentation.screens.financial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.domain.model.financial.Money
import com.example.presentation.viewmodel.financial.AuthorEarningsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorFinancialScreen(
    viewModel: AuthorEarningsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var payoutDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Author Royalties & Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Wallet Balance Cards
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "AVAILABLE FOR PAYOUT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.wallet?.availableMoney?.formatted ?: "₹0.00",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Pending Settlement", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(state.wallet?.pendingMoney?.formatted ?: "₹0.00", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Lifetime Earned", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(state.wallet?.lifetimeEarnedMoney?.formatted ?: "₹0.00", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Lifetime Paid", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(state.wallet?.lifetimePaidMoney?.formatted ?: "₹0.00", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { payoutDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("request_payout_btn")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Payout to Bank / UPI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Summary Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.summary?.totalSalesCount ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gross Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money(state.summary?.grossEarningsMinor ?: 0L).formatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Platform Fee (20%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money(state.summary?.platformFeesMinor ?: 0L).formatted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Payout Requests Section
            if (state.payoutRequests.isNotEmpty()) {
                item {
                    Text("Payout Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(state.payoutRequests) { req ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(req.requestedAt))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.amountMoney.formatted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("$dateStr • ${req.payoutAccountMasked}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (req.status.name) {
                                    "PAID" -> Color(0xFFE8F5E9)
                                    "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color(0xFFFFF3E0)
                                }
                            ) {
                                Text(
                                    req.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (req.status.name) {
                                        "PAID" -> Color(0xFF2E7D32)
                                        "REJECTED" -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFFE65100)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Royalty Ledger Entries
            item {
                Text("Author Royalty Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (state.ledger.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No royalty records yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.ledger, key = { it.id }) { ledgerItem ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ledgerItem.createdAt))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ledgerItem.bookTitleSnapshot, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text("$dateStr • Order: ${ledgerItem.orderId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Gross: ${ledgerItem.grossMoney.formatted} | Fee (20%): ${ledgerItem.platformFeeMoney.formatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    ledgerItem.royaltyMoney.formatted,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (ledgerItem.royaltyAmountMinor < 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                                Text(ledgerItem.status.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Payout Request Modal
        if (payoutDialogOpen) {
            AlertDialog(
                onDismissRequest = { payoutDialogOpen = false },
                title = { Text("Request Royalty Payout", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Minimum payout threshold is ₹1,000.00. Payouts are reviewed and settled directly to your registered bank account or UPI VPA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.payoutAmountInput,
                            onValueChange = { viewModel.onPayoutAmountChanged(it) },
                            label = { Text("Amount (in Rupees)") },
                            prefix = { Text("₹") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.payoutAccountInput,
                            onValueChange = { viewModel.onPayoutAccountChanged(it) },
                            label = { Text("Payout UPI / Bank Account") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.submitPayoutRequest()
                            payoutDialogOpen = false
                        }
                    ) {
                        Text("Confirm Payout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { payoutDialogOpen = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
