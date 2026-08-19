package com.example.presentation.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UiState
import com.example.domain.model.review.*
import com.example.presentation.components.review.UnverifiedReviewerBadge
import com.example.presentation.components.review.VerifiedReaderBadge
import com.example.presentation.viewmodel.review.AdminReviewModerationViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewModerationScreen(
    viewModel: AdminReviewModerationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsList.collectAsStateWithLifecycle()
    val actionState by viewModel.moderationActionState.collectAsStateWithLifecycle()

    var actionDialogReview by remember { mutableStateOf<BookReview?>(null) }
    var selectedAction by remember { mutableStateOf(ReviewAuditAction.APPROVE) }
    var reasonInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            snackbarMessage = (actionState as UiState.Success<String>).data
            viewModel.resetActionState()
        } else if (actionState is UiState.Error) {
            snackbarMessage = (actionState as UiState.Error).message
            viewModel.resetActionState()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PolishBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Review Moderation Center",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Text(
                            text = "Phase 9: Trusted Reviews & Reader Verification",
                            style = MaterialTheme.typography.labelSmall,
                            color = PolishSlate500
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PolishSlate700
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Tabs
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, PolishSlate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "All Reviews",
                        "REPORTED" to "Flagged / Reported",
                        "HIDDEN" to "Hidden / Rejected",
                        "PUBLISHED" to "Published"
                    )

                    tabs.forEach { (tabKey, label) ->
                        val isSelected = selectedTab == tabKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tabKey) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PolishPrimaryIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = PolishSlate100,
                                labelColor = PolishSlate700
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) PolishPrimaryIndigo else PolishSlate200
                            ),
                            modifier = Modifier.testTag("admin_review_tab_$tabKey")
                        )
                    }
                }
            }

            // Reviews List
            when (val state = reviewsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PolishPrimaryIndigo)
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PolishSlate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No reviews in this moderation filter",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PolishSlate700
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val reviews = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(reviews, key = { it.id }) { rev ->
                            AdminReviewItemCard(
                                review = rev,
                                onAction = { action ->
                                    actionDialogReview = rev
                                    selectedAction = action
                                    reasonInput = ""
                                }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Moderation Action Reason Dialog
    if (actionDialogReview != null) {
        AlertDialog(
            onDismissRequest = { actionDialogReview = null },
            title = {
                Text(
                    text = "Confirm Action: ${selectedAction.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to perform '${selectedAction.name}' on review by ${actionDialogReview?.userName}?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Audit Reason / Note") },
                        placeholder = { Text("e.g. Cleared after spam review, or Violation of terms") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reviewId = actionDialogReview!!.id
                        viewModel.performAction(reviewId, selectedAction, reasonInput)
                        actionDialogReview = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedAction) {
                            ReviewAuditAction.APPROVE, ReviewAuditAction.RESTORE -> Color(0xFF059669)
                            ReviewAuditAction.HIDE, ReviewAuditAction.REMOVE, ReviewAuditAction.REJECT -> Color(0xFFDC2626)
                            ReviewAuditAction.FLAG_SUSPICIOUS -> Color(0xFFD97706)
                        }
                    )
                ) {
                    Text("Apply Action")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogReview = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (snackbarMessage != null) {
        LaunchedEffect(snackbarMessage) {
            kotlinx.coroutines.delay(3500)
            snackbarMessage = null
        }
    }
}

@Composable
private fun AdminReviewItemCard(
    review: BookReview,
    onAction: (ReviewAuditAction) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishSlate200),
        modifier = Modifier.fillMaxWidth().testTag("admin_review_item_${review.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate900
                    )
                    if (review.isVerifiedReader) {
                        VerifiedReaderBadge()
                    } else {
                        UnverifiedReviewerBadge()
                    }
                }

                // Moderation Status Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (review.moderationStatus) {
                        ReviewModerationStatus.PUBLISHED -> Color(0xFFECFDF5)
                        ReviewModerationStatus.PENDING_REVIEW -> Color(0xFFFEF3C7)
                        ReviewModerationStatus.HIDDEN -> Color(0xFFF1F5F9)
                        ReviewModerationStatus.REMOVED, ReviewModerationStatus.REJECTED -> Color(0xFFFEE2E2)
                    }
                ) {
                    Text(
                        text = review.moderationStatus.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when (review.moderationStatus) {
                                ReviewModerationStatus.PUBLISHED -> Color(0xFF059669)
                                ReviewModerationStatus.PENDING_REVIEW -> Color(0xFFD97706)
                                ReviewModerationStatus.HIDDEN -> Color(0xFF475569)
                                ReviewModerationStatus.REMOVED, ReviewModerationStatus.REJECTED -> Color(0xFFDC2626)
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rating Stars & Book ID
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i <= review.rating) PolishAccentOrange else PolishSlate200,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Book: ${review.bookId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishSlate400
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (review.title.isNotBlank()) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = PolishSlate900
                )
                Spacer(modifier = Modifier.height(3.dp))
            }

            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = PolishSlate700
            )

            // Reports / Helpful metrics
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (review.reportCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${review.reportCount} ${if (review.reportCount == 1) "Report" else "Reports"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                }

                Text(
                    text = "${review.helpfulCount} helpful votes",
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishSlate500
                )

                val dateFormatted = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(review.createdAt))
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = PolishSlate400
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = PolishSlate100)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (review.moderationStatus != ReviewModerationStatus.PUBLISHED) {
                    OutlinedButton(
                        onClick = { onAction(ReviewAuditAction.APPROVE) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (review.moderationStatus == ReviewModerationStatus.PUBLISHED) {
                    OutlinedButton(
                        onClick = { onAction(ReviewAuditAction.HIDE) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hide", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedButton(
                    onClick = { onAction(ReviewAuditAction.REMOVE) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
