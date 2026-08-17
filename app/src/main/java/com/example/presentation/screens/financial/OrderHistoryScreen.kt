package com.example.presentation.screens.financial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import coil.compose.AsyncImage
import com.example.domain.model.financial.Order
import com.example.domain.model.financial.OrderStatus
import com.example.presentation.viewmodel.financial.OrderHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var refundDialogOpen by remember { mutableStateOf(false) }
    var refundReason by remember { mutableStateOf("") }
    var targetOrderIdForRefund by remember { mutableStateOf("") }

    LaunchedEffect(state.refundSuccessMessage, state.errorMessage) {
        state.refundSuccessMessage?.let {
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
                title = { Text("Order History & Receipts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.orders.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Orders Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Your purchased books and receipts will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onViewReceipt = { viewModel.selectOrder(order) },
                        onRequestRefund = {
                            targetOrderIdForRefund = order.id
                            refundReason = "Purchased by mistake"
                            refundDialogOpen = true
                        }
                    )
                }
            }
        }

        // Receipt Details Dialog
        state.selectedOrder?.let { order ->
            ReceiptDialog(
                order = order,
                onDismiss = { viewModel.selectOrder(null) }
            )
        }

        // Refund Request Dialog
        if (refundDialogOpen) {
            AlertDialog(
                onDismissRequest = { refundDialogOpen = false },
                title = { Text("Request Order Refund", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Per marketplace policy, approved refunds immediately revoke digital library access and reverse associated author royalties.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = refundReason,
                            onValueChange = { refundReason = it },
                            label = { Text("Reason for Refund") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.requestRefund(targetOrderIdForRefund, refundReason)
                            refundDialogOpen = false
                        }
                    ) {
                        Text("Submit Refund Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { refundDialogOpen = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onViewReceipt: () -> Unit,
    onRequestRefund: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    }

    val (statusColor, statusBg) = when (order.status) {
        OrderStatus.PAID, OrderStatus.COMPLETED -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
        OrderStatus.PAYMENT_PENDING, OrderStatus.PENDING -> Color(0xFFF57C00) to Color(0xFFFFF3E0)
        OrderStatus.REFUND_PENDING, OrderStatus.REFUNDED -> Color(0xFFC2185B) to Color(0xFFFCE4EC)
        OrderStatus.CANCELLED, OrderStatus.FAILED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable { onViewReceipt() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        order.id,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        order.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            order.items.take(2).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.coverUrlSnapshot,
                        contentDescription = item.titleSnapshot,
                        modifier = Modifier
                            .size(35.dp, 48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        item.titleSnapshot,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(item.finalPriceMoney.formatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            if (order.items.size > 2) {
                Text(
                    "+ ${order.items.size - 2} more item(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total: ${order.totalMoney.formatted}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row {
                    if (order.isPaid) {
                        TextButton(onClick = onRequestRefund) {
                            Text("Request Refund", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("View Receipt", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Official Tax Receipt", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order ID", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.id, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.status.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Text("Purchased Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.titleSnapshot, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text(item.finalPriceMoney.formatted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                    Text(order.subtotalMoney.formatted, style = MaterialTheme.typography.bodySmall)
                }
                if (order.discountMinor > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text("-${order.discountMoney.formatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GST & Taxes (Exempt)", style = MaterialTheme.typography.bodySmall)
                    Text("₹0.00", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid", fontWeight = FontWeight.Bold)
                    Text(order.totalMoney.formatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
