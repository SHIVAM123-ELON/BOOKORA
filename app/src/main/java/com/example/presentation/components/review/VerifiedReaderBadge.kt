package com.example.presentation.components.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
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
import com.example.ui.theme.PolishPrimaryIndigo

val VerifiedBadgeGreen = Color(0xFF059669)
val VerifiedBadgeBg = Color(0xFFECFDF5)
val VerifiedBadgeBorder = Color(0xFFA7F3D0)

val UnverifiedBadgeColor = Color(0xFF64748B)
val UnverifiedBadgeBg = Color(0xFFF1F5F9)
val UnverifiedBadgeBorder = Color(0xFFE2E8F0)

@Composable
fun VerifiedReaderBadge(
    modifier: Modifier = Modifier,
    showInfoDialogOnClick: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .testTag("verified_reader_badge")
            .then(
                if (showInfoDialogOnClick) Modifier.clickable { showDialog = true } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = VerifiedBadgeBg,
        border = BorderStroke(1.dp, VerifiedBadgeBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Reader",
                tint = VerifiedBadgeGreen,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Verified Reader",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    color = VerifiedBadgeGreen
                )
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = VerifiedBadgeGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Verified Reader Badge",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This badge is awarded by BOOKORA's trusted verification engine when:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VerifiedBadgeGreen,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = "The reviewer has an active, legitimate entitlement (purchase, claim, or subscription).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VerifiedBadgeGreen,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = "The reviewer has completed meaningful reading activity (at least 10% or 5 minutes of read time).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = PolishPrimaryIndigo)
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun UnverifiedReviewerBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("unverified_reviewer_badge"),
        shape = RoundedCornerShape(20.dp),
        color = UnverifiedBadgeBg,
        border = BorderStroke(1.dp, UnverifiedBadgeBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Unverified Reviewer",
                tint = UnverifiedBadgeColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "Community Review",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = UnverifiedBadgeColor
                )
            )
        }
    }
}
