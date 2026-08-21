package com.example.presentation.components.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.review.BookReview
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewCard(
    review: BookReview,
    currentUserId: String,
    onToggleHelpful: () -> Unit,
    onReport: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAuthorOfReview = review.userId == currentUserId
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishSlate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: User avatar, name, verification badge, date & menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (!review.userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = review.userAvatarUrl,
                            contentDescription = review.userName,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = PolishPrimaryLight,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = review.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimaryIndigo
                                    )
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = review.userName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishSlate900
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isAuthorOfReview) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PolishPrimaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "You",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PolishPrimaryIndigo
                                        ),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Badge Row
                        if (review.isVerifiedReader) {
                            VerifiedReaderBadge()
                        } else {
                            UnverifiedReviewerBadge()
                        }
                    }
                }

                // Options Menu / Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(review.createdAt))
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishSlate400
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Review options",
                                tint = PolishSlate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isAuthorOfReview) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Review") },
                                        onClick = {
                                            showMenu = false
                                            onEdit()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Review", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Report Review") },
                                    onClick = {
                                        showMenu = false
                                        onReport()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rating Stars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i <= review.rating) PolishAccentOrange else PolishSlate200,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (review.isEdited) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(edited)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = PolishSlate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Text
            if (review.title.isNotBlank()) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PolishSlate900
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 21.sp,
                    color = PolishSlate700
                )
            )

            // Author Reply (if exists)
            if (!review.authorReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PolishSlate100,
                    border = BorderStroke(1.dp, PolishSlate200)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = null,
                                tint = PolishPrimaryIndigo,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Author's Response",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.authorReply,
                            style = MaterialTheme.typography.bodySmall.copy(color = PolishSlate700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Helpful Vote Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (review.isHelpfulByCurrentUser) PolishPrimaryLight else PolishSlate100,
                    border = BorderStroke(
                        1.dp,
                        if (review.isHelpfulByCurrentUser) PolishPrimaryIndigo.copy(alpha = 0.4f) else PolishSlate200
                    ),
                    modifier = Modifier.testTag("helpful_button_${review.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = !isAuthorOfReview) { onToggleHelpful() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (review.isHelpfulByCurrentUser) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                            contentDescription = "Helpful",
                            tint = if (review.isHelpfulByCurrentUser) PolishPrimaryIndigo else PolishSlate500,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (review.helpfulCount > 0) "Helpful (${review.helpfulCount})" else "Helpful",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (review.isHelpfulByCurrentUser) FontWeight.Bold else FontWeight.Medium,
                                color = if (review.isHelpfulByCurrentUser) PolishPrimaryIndigo else PolishSlate700
                            )
                        )
                    }
                }

                if (!isAuthorOfReview) {
                    TextButton(
                        onClick = onReport,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "Report",
                            style = MaterialTheme.typography.labelSmall.copy(color = PolishSlate400)
                        )
                    }
                }
            }
        }
    }
}
