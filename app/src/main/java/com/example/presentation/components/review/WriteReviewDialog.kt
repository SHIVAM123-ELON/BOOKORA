package com.example.presentation.components.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.review.BookReview
import com.example.domain.model.review.ReviewEligibility
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewDialog(
    bookTitle: String,
    eligibility: ReviewEligibility?,
    existingReview: BookReview? = null,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, title: String, reviewText: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(existingReview?.rating ?: 5) }
    var title by remember { mutableStateOf(existingReview?.title ?: "") }
    var reviewText by remember { mutableStateOf(existingReview?.reviewText ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isEditing = existingReview != null
    val charCount = reviewText.trim().length
    val minChars = 10
    val maxChars = 2000
    val isValid = charCount in minChars..maxChars && title.trim().length >= 3

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("write_review_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditing) "Edit Your Review" else "Write a Review",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishSlate900
                            )
                        )
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PolishSlate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Eligibility & Verification Status Banner
                if (eligibility != null) {
                    if (eligibility.isVerifiedReader) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Verified Reader Badge Eligible",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    )
                                    Text(
                                        text = "Your review will be badged as a Verified Reader based on your active entitlement and ${"%.0f".format(eligibility.readingPercent)}% reading progress.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = Color(0xFF065F46)
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Community Review",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                    )
                                    Text(
                                        text = if (eligibility.hasEntitlement) {
                                            "Read at least 10% or 5 minutes (Current: ${"%.0f".format(eligibility.readingPercent)}%) to earn the Verified Reader badge."
                                        } else {
                                            "You can leave a community review. Readers with verified purchases receive a Verified Reader badge."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Star Rating Picker
                Text(
                    text = "Overall Rating",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PolishSlate900
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(44.dp).testTag("star_rating_$i")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$i Stars",
                                tint = if (i <= rating) PolishAccentOrange else PolishSlate200,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Review Headline/Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorText = null
                    },
                    label = { Text("Headline / Summary") },
                    placeholder = { Text("e.g. Masterpiece on modern architecture") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("review_headline_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishPrimaryIndigo,
                        unfocusedBorderColor = PolishSlate200
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Review Body
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = {
                        if (it.length <= maxChars) {
                            reviewText = it
                            errorText = null
                        }
                    },
                    label = { Text("Your Review") },
                    placeholder = { Text("Share what you enjoyed, who should read this book, and key takeaways...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("review_body_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PolishPrimaryIndigo,
                        unfocusedBorderColor = PolishSlate200
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Character Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (charCount < minChars) {
                        Text(
                            text = "Minimum $minChars characters required",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDC2626)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = "$charCount / $maxChars",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (charCount < minChars) Color(0xFFDC2626) else PolishSlate400
                    )
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PolishSlate200)
                    ) {
                        Text("Cancel", color = PolishSlate700)
                    }

                    Button(
                        onClick = {
                            if (title.trim().length < 3) {
                                errorText = "Headline must be at least 3 characters."
                                return@Button
                            }
                            if (charCount < minChars) {
                                errorText = "Review must be at least $minChars characters."
                                return@Button
                            }
                            onSubmit(rating, title.trim(), reviewText.trim())
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f).testTag("submit_review_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PolishPrimaryIndigo,
                            disabledContainerColor = PolishSlate200
                        )
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Post Review",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
