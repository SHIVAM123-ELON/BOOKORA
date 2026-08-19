package com.example.presentation.screens.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.presentation.components.review.*
import com.example.presentation.viewmodel.review.BookReviewsViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReviewsSection(
    bookId: String,
    bookTitle: String,
    viewModel: BookReviewsViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(bookId) {
        viewModel.setBookId(bookId)
    }

    val ratingSummary by viewModel.getRatingSummary(bookId).collectAsStateWithLifecycle()
    val reviewsState by viewModel.getReviews(bookId).collectAsStateWithLifecycle()
    val userReview by viewModel.getUserReview(bookId).collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val eligibility by viewModel.eligibilityState.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var showWriteDialog by remember { mutableStateOf(false) }
    var reviewToEdit by remember { mutableStateOf<BookReview?>(null) }
    var reviewToReport by remember { mutableStateOf<String?>(null) }
    var reviewToDelete by remember { mutableStateOf<String?>(null) }
    var statusSnackbarMessage by remember { mutableStateOf<String?>(null) }

    val actionState by viewModel.reviewActionState.collectAsStateWithLifecycle()

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            statusSnackbarMessage = (actionState as UiState.Success<String>).data
            viewModel.resetActionState()
        } else if (actionState is UiState.Error) {
            statusSnackbarMessage = (actionState as UiState.Error).message
            viewModel.resetActionState()
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ratings & Reviews",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PolishSlate900
                    )
                )
                Text(
                    text = "Authentic reviews from verified readers",
                    style = MaterialTheme.typography.bodySmall,
                    color = PolishSlate500
                )
            }

            if (eligibility?.isAuthor != true && userReview == null) {
                Button(
                    onClick = {
                        viewModel.refreshEligibility(bookId)
                        reviewToEdit = null
                        showWriteDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("write_review_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Write Review",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Authoritative Rating Distribution Breakdown
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, PolishSlate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RatingDistributionBar(ratingSummary = ratingSummary)
            }
        }

        // Display current user's review if they already submitted one
        if (userReview != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Review",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PolishSlate900
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReviewCard(
                review = userReview!!,
                currentUserId = currentUserId,
                onToggleHelpful = { /* own review cannot be voted */ },
                onReport = { /* own review cannot be reported */ },
                onEdit = {
                    reviewToEdit = userReview
                    showWriteDialog = true
                },
                onDelete = {
                    reviewToDelete = userReview?.id
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort Options Horizontal Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PolishSlate500
            )

            ReviewSortOption.values().forEach { option ->
                val isSelected = (sortOption == option)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSortOption(option) },
                    label = {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
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
                    modifier = Modifier.testTag("sort_chip_${option.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Reviews List
        when (val state = reviewsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PolishPrimaryIndigo, modifier = Modifier.size(32.dp))
                }
            }
            is UiState.Empty -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PolishSlate100,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = PolishSlate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No reviews yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate700
                        )
                        Text(
                            text = "Be the first verified reader to review this book!",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )
                    }
                }
            }
            is UiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is UiState.Success -> {
                val reviews = state.data
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    reviews.forEach { rev ->
                        // Don't duplicate user review if already displayed above
                        if (userReview == null || rev.id != userReview?.id) {
                            ReviewCard(
                                review = rev,
                                currentUserId = currentUserId,
                                onToggleHelpful = { viewModel.toggleHelpfulVote(rev.id) },
                                onReport = { reviewToReport = rev.id },
                                onEdit = if (rev.userId == currentUserId) {
                                    {
                                        reviewToEdit = rev
                                        showWriteDialog = true
                                    }
                                } else null,
                                onDelete = if (rev.userId == currentUserId) {
                                    { reviewToDelete = rev.id }
                                } else null
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    // Write / Edit Dialog
    if (showWriteDialog) {
        WriteReviewDialog(
            bookTitle = bookTitle,
            eligibility = eligibility,
            existingReview = reviewToEdit,
            onDismiss = { showWriteDialog = false },
            onSubmit = { rating, title, reviewText ->
                if (reviewToEdit != null) {
                    viewModel.updateReview(
                        reviewId = reviewToEdit!!.id,
                        rating = rating,
                        title = title,
                        reviewText = reviewText,
                        onSuccess = { showWriteDialog = false }
                    )
                } else {
                    viewModel.submitReview(
                        bookId = bookId,
                        rating = rating,
                        title = title,
                        reviewText = reviewText,
                        onSuccess = { showWriteDialog = false }
                    )
                }
            }
        )
    }

    // Report Dialog
    if (reviewToReport != null) {
        ReviewReportDialog(
            reviewId = reviewToReport!!,
            onDismiss = { reviewToReport = null },
            onSubmitReport = { reason, details ->
                viewModel.reportReview(
                    reviewId = reviewToReport!!,
                    reason = reason,
                    details = details
                ) { _, msg ->
                    statusSnackbarMessage = msg
                    reviewToReport = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (reviewToDelete != null) {
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to remove your review? This will also update the book's aggregate rating.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(reviewToDelete!!) {
                            reviewToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (statusSnackbarMessage != null) {
        LaunchedEffect(statusSnackbarMessage) {
            kotlinx.coroutines.delay(3500)
            statusSnackbarMessage = null
        }
    }
}
