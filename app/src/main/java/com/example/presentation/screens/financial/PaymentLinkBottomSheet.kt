package com.example.presentation.screens.financial

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.financial.RazorpayBackendService
import com.example.domain.financial.RazorpayConfig
import com.example.domain.model.financial.PaymentLinkDeliveryMethod
import com.example.domain.model.financial.PaymentLinkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentLinkBottomSheet(
    orderId: String,
    booksSummary: String,
    amountFormatted: String,
    amountMinor: Long,
    bookIds: List<String>,
    initialDeliveryMethod: PaymentLinkDeliveryMethod = PaymentLinkDeliveryMethod.WHATSAPP,
    createdLinkResponse: RazorpayBackendService.CreatePaymentLinkResponse? = null,
    isGenerating: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onGenerateLink: (
        method: PaymentLinkDeliveryMethod,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        expiryHours: Int
    ) -> Unit,
    onSimulatePaid: (paymentLinkId: String) -> Unit,
    onPaymentSuccessDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedMethod by remember { mutableStateOf(initialDeliveryMethod) }
    var customerName by remember { mutableStateOf("Reader") }
    var customerPhone by remember { mutableStateOf("+919876543210") }
    var customerEmail by remember { mutableStateOf("reader@bookora.app") }
    var selectedExpiryHours by remember { mutableIntStateOf(24) }

    var isSettlingPayment by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00BAF2).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFF00BAF2),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Razorpay Payment Link",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Share via WhatsApp, SMS, Email or Link",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order & Amount Badge Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Order Item(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            booksSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            amountFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00BAF2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner if any
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (createdLinkResponse == null) {
                // ============================================================
                // STAGE 1: CONFIGURE & GENERATE PAYMENT LINK
                // ============================================================

                Text(
                    "Choose Delivery Channel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Channel Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChannelChoiceChip(
                        title = "WhatsApp",
                        icon = Icons.Default.Chat,
                        selectedColor = Color(0xFF25D366),
                        isSelected = selectedMethod == PaymentLinkDeliveryMethod.WHATSAPP,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = PaymentLinkDeliveryMethod.WHATSAPP }
                    )
                    ChannelChoiceChip(
                        title = "SMS",
                        icon = Icons.Default.Sms,
                        selectedColor = Color(0xFF2563EB),
                        isSelected = selectedMethod == PaymentLinkDeliveryMethod.SMS,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = PaymentLinkDeliveryMethod.SMS }
                    )
                    ChannelChoiceChip(
                        title = "Email",
                        icon = Icons.Default.Email,
                        selectedColor = Color(0xFFEA4335),
                        isSelected = selectedMethod == PaymentLinkDeliveryMethod.EMAIL,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = PaymentLinkDeliveryMethod.EMAIL }
                    )
                    ChannelChoiceChip(
                        title = "Direct",
                        icon = Icons.Default.Link,
                        selectedColor = Color(0xFF00BAF2),
                        isSelected = selectedMethod == PaymentLinkDeliveryMethod.DIRECT_LINK,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = PaymentLinkDeliveryMethod.DIRECT_LINK }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient Details
                Text(
                    "Customer / Recipient Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_link_customer_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedMethod == PaymentLinkDeliveryMethod.WHATSAPP || selectedMethod == PaymentLinkDeliveryMethod.SMS) {
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Mobile Number (with +91)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_link_customer_phone")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedMethod == PaymentLinkDeliveryMethod.EMAIL || selectedMethod == PaymentLinkDeliveryMethod.DIRECT_LINK) {
                    OutlinedTextField(
                        value = customerEmail,
                        onValueChange = { customerEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_link_customer_email")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Link Expiry Option
                Text(
                    "Link Validity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("24 Hours", 24),
                        Pair("48 Hours", 48),
                        Pair("7 Days", 168)
                    ).forEach { (label, hours) ->
                        FilterChip(
                            selected = selectedExpiryHours == hours,
                            onClick = { selectedExpiryHours = hours },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Generate Button
                Button(
                    onClick = {
                        onGenerateLink(
                            selectedMethod,
                            customerName,
                            customerEmail,
                            customerPhone,
                            selectedExpiryHours
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_payment_link_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedMethod) {
                            PaymentLinkDeliveryMethod.WHATSAPP -> Color(0xFF25D366)
                            PaymentLinkDeliveryMethod.SMS -> Color(0xFF2563EB)
                            PaymentLinkDeliveryMethod.EMAIL -> Color(0xFFEA4335)
                            PaymentLinkDeliveryMethod.COPY_LINK,
                            PaymentLinkDeliveryMethod.DIRECT_LINK -> Color(0xFF00BAF2)
                        }
                    ),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Razorpay Payment Link...", color = Color.White)
                    } else {
                        Icon(
                            when (selectedMethod) {
                                PaymentLinkDeliveryMethod.WHATSAPP -> Icons.Default.Chat
                                PaymentLinkDeliveryMethod.SMS -> Icons.Default.Sms
                                PaymentLinkDeliveryMethod.EMAIL -> Icons.Default.Email
                                PaymentLinkDeliveryMethod.COPY_LINK,
                                PaymentLinkDeliveryMethod.DIRECT_LINK -> Icons.Default.Link
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Generate Link for ${selectedMethod.name.replace("_", " ")}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

            } else {
                // ============================================================
                // STAGE 2: PAYMENT LINK READY & ACTIONS
                // ============================================================

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00BAF2).copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFF00BAF2)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Razorpay Payment Link Ready",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF0C2340)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF16A34A).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    createdLinkResponse.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // URL display + Copy Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    createdLinkResponse.paymentLinkUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF00BAF2),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, "Payment Link", createdLinkResponse.paymentLinkUrl)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Link",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Link ID: ${createdLinkResponse.razorpayPaymentLinkId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Send or Share Link",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Channel Action Buttons Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. WhatsApp Button
                    Button(
                        onClick = {
                            shareToWhatsApp(
                                context = context,
                                phone = createdLinkResponse.customerPhone,
                                message = createdLinkResponse.shareMessage
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 2. SMS Button
                    Button(
                        onClick = {
                            sendSms(
                                context = context,
                                phone = createdLinkResponse.customerPhone,
                                body = createdLinkResponse.smsText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_sms_btn")
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via SMS", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 3. Email Button
                    Button(
                        onClick = {
                            sendEmail(
                                context = context,
                                email = createdLinkResponse.customerEmail,
                                subject = createdLinkResponse.emailSubject,
                                body = createdLinkResponse.emailBody
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_email_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send via Email", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // 4. Copy Full Message
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(context, "Bookora Payment Details", createdLinkResponse.shareMessage)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("copy_full_message_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Full Notification Message", fontWeight = FontWeight.SemiBold)
                    }

                    // 5. Merchant Razorpay.me Page Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RazorpayConfig.RAZORPAY_ME_PAGE_URL))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("merchant_razorpay_me_btn")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF00BAF2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Merchant Page (${RazorpayConfig.RAZORPAY_ME_HANDLE})", fontWeight = FontWeight.SemiBold, color = Color(0xFF00BAF2))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Webhook / Payment Simulation Banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Live Webhook & Payment Verification",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Simulates when the customer completes payment through the link. The server verifies the signature, updates the order to PAID, and permanently unlocks books in My Library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isSettlingPayment = true
                                onSimulatePaid(createdLinkResponse.paymentLinkId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("simulate_payment_link_paid_btn"),
                            enabled = !isSettlingPayment
                        ) {
                            if (isSettlingPayment) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying Server Webhook...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Payment & Settle Order", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChannelChoiceChip(
    title: String,
    icon: ImageVector,
    selectedColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.5.dp, if (isSelected) selectedColor else Color.Transparent),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}

private fun shareToWhatsApp(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to standard share sheet
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Razorpay Payment Link"))
    }
}

private fun sendSms(context: Context, phone: String, body: String) {
    try {
        val uri = Uri.parse("smsto:${phone.trim()}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, body)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Payment Link via SMS"))
    }
}

private fun sendEmail(context: Context, email: String, subject: String, body: String) {
    try {
        val uri = Uri.parse("mailto:${email.trim()}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            type = "message/rfc822"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Payment Link via Email"))
    }
}
