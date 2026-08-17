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
import com.example.presentation.viewmodel.financial.AdminFinanceTab
import com.example.presentation.viewmodel.financial.AdminFinancialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinancialScreen(
    viewModel: AdminFinancialViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Marketplace Financial Center", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = state.activeTab.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                AdminFinanceTab.values().forEach { tab ->
                    Tab(
                        selected = state.activeTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                tab.name.replace("_", " ").lowercase().capitalize(Locale.ROOT),
                                fontWeight = if (state.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Contents
            when (state.activeTab) {
                AdminFinanceTab.OVERVIEW -> {
                    AdminOverviewContent(state = state)
                }
                AdminFinanceTab.PAYOUTS -> {
                    AdminPayoutsContent(
                        state = state,
                        onApprove = { viewModel.approvePayout(it) },
                        onReject = { viewModel.rejectPayout(it) }
                    )
                }
                AdminFinanceTab.REFUNDS -> {
                    AdminRefundsContent(
                        state = state,
                        onApproveRefund = { viewModel.approveRefund(it) }
                    )
                }
                AdminFinanceTab.SETTINGS -> {
                    AdminSettingsContent(
                        state = state,
                        onUpdateCommission = { viewModel.updateCommissionRate(it) }
                    )
                }
                AdminFinanceTab.AUDIT_LOGS -> {
                    AdminAuditLogsContent(state = state)
                }
                else -> {
                    AdminOverviewContent(state = state)
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewContent(state: com.example.presentation.viewmodel.financial.AdminFinancialUiState) {
    val metrics = state.metrics
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOTAL GROSS MARKETPLACE VOLUME (GMV)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Money(metrics?.grossSalesMinor ?: 0L).formatted,
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
                            Text("Platform Net (20%)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(Money(metrics?.platformRevenueMinor ?: 0L).formatted, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Author Earnings", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(Money(metrics?.authorEarningsMinor ?: 0L).formatted, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Active Subscribers", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text("${metrics?.activeSubscriptionsCount ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Orders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${metrics?.totalOrdersCount ?: 0}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Refunds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Money(metrics?.totalRefundsMinor ?: 0L).formatted, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Text("Recent Marketplace Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(state.recentOrders.take(5), key = { it.id }) { order ->
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
                    Column {
                        Text(order.id, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("User: ${order.userId} • Items: ${order.items.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(order.totalMoney.formatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(order.status.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPayoutsContent(
    state: com.example.presentation.viewmodel.financial.AdminFinancialUiState,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Author Payout Approvals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (state.payoutRequests.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No payout requests found.")
                }
            }
        } else {
            items(state.payoutRequests) { payout ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(payout.amountMoney.formatted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Author: ${payout.authorId} • ${payout.payoutAccountMasked}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (payout.status.name) {
                                    "PAID" -> Color(0xFFE8F5E9)
                                    "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color(0xFFFFF3E0)
                                }
                            ) {
                                Text(
                                    payout.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (payout.status.name) {
                                        "PAID" -> Color(0xFF2E7D32)
                                        "REJECTED" -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFFE65100)
                                    }
                                )
                            }
                        }

                        if (payout.status.name == "REQUESTED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onReject(payout.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reject & Refund")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { onApprove(payout.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve & Settle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRefundsContent(
    state: com.example.presentation.viewmodel.financial.AdminFinancialUiState,
    onApproveRefund: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Order Refund Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (state.refunds.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No refund requests.")
                }
            }
        } else {
            items(state.refunds) { refund ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(refund.amountMoney.formatted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Order: ${refund.orderId} • Reason: ${refund.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(refund.status.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        if (refund.status.name == "REQUESTED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onApproveRefund(refund.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Approve Refund, Revoke Access & Reverse Royalty")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsContent(
    state: com.example.presentation.viewmodel.financial.AdminFinancialUiState,
    onUpdateCommission: (Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Marketplace Economics & Commission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Default Platform Take-Rate (Commission)", fontWeight = FontWeight.Bold)
                Text(
                    "Current Platform Share: ${(state.settings.defaultPlatformCommissionRate * 100).toInt()}% • Author Net: ${(100 - state.settings.defaultPlatformCommissionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.15, 0.20, 0.25, 0.30).forEach { rate ->
                        val selected = state.settings.defaultPlatformCommissionRate == rate
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateCommission(rate) },
                            label = { Text("${(rate * 100).toInt()}%") }
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Payout & Policy Guardrails", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Minimum Payout Threshold: ₹1,000.00", style = MaterialTheme.typography.bodySmall)
                Text("• Settlement Schedule: Weekly on Monday", style = MaterialTheme.typography.bodySmall)
                Text("• Refund Access Rule: ${state.settings.refundEntitlementPolicy}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AdminAuditLogsContent(state: com.example.presentation.viewmodel.financial.AdminFinancialUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Financial Audit Trail & Risk Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(state.auditLogs) { log ->
            val dateStr = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Actor: ${log.actor} • Entity: ${log.entity} (${log.entityId})", style = MaterialTheme.typography.labelSmall)
                    Text("Metadata: ${log.metadata}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
