package com.example.presentation.screens.publisher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.domain.model.publisher.BookSubmission
import com.example.domain.model.publisher.CreatorPayoutStatus
import com.example.domain.model.publisher.SubmissionStatus
import com.example.presentation.viewmodel.publisher.AdminModerationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    viewModel: AdminModerationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Publisher Moderation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Submissions (${state.submissions.count { it.status == SubmissionStatus.PENDING_REVIEW }})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Payouts (${state.payoutRequests.count { it.status == CreatorPayoutStatus.REQUESTED }})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Copyright Reports (${state.copyrightReports.count { !it.isResolved }})") }
                )
            }

            if (state.successMessage != null) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(state.successMessage!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Submissions Moderation Queue
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (state.submissions.isEmpty()) {
                            item {
                                Text("No submissions in queue.", modifier = Modifier.padding(16.dp))
                            }
                        } else {
                            items(state.submissions) { sub ->
                                SubmissionCard(
                                    submission = sub,
                                    onClick = { viewModel.onSelectSubmission(sub) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Payout Management Queue
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (state.payoutRequests.isEmpty()) {
                            item { Text("No payout requests.", modifier = Modifier.padding(16.dp)) }
                        } else {
                            items(state.payoutRequests) { req ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(req.formattedAmount, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text(req.status.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("User ID: ${req.userId}", style = MaterialTheme.typography.bodySmall)
                                        Text("UPI Target: ${req.upiId}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                                        if (req.status == CreatorPayoutStatus.REQUESTED) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { viewModel.updatePayoutRequest(req.id, CreatorPayoutStatus.PAID) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                                ) {
                                                    Text("Approve & Mark Paid")
                                                }
                                                OutlinedButton(
                                                    onClick = { viewModel.updatePayoutRequest(req.id, CreatorPayoutStatus.REJECTED) }
                                                ) {
                                                    Text("Reject & Refund")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Copyright Reports Queue
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (state.copyrightReports.isEmpty()) {
                            item { Text("No copyright infringement reports filed.", modifier = Modifier.padding(16.dp)) }
                        } else {
                            items(state.copyrightReports) { cr ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Reported Submission: ${cr.submissionId}", fontWeight = FontWeight.Bold)
                                        Text("Reason: ${cr.reason}", style = MaterialTheme.typography.bodySmall)
                                        Text("Reporter: ${cr.reporterEmail}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Proof: ${cr.proofDetails}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Submission Review Modal Dialog
        if (state.selectedSubmission != null) {
            val sub = state.selectedSubmission!!
            AlertDialog(
                onDismissRequest = { viewModel.onSelectSubmission(null) },
                title = { Text("Review: ${sub.title}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Author: ${sub.authorName} • Language: ${sub.language}", style = MaterialTheme.typography.bodySmall)
                        Text("Uploader: ${sub.uploaderName} (${sub.uploaderEmail})", style = MaterialTheme.typography.bodySmall)
                        Text("Pages: ${sub.pdfPageCount} • Size: ${sub.pdfFileSizeBytes / 1024} KB", style = MaterialTheme.typography.bodySmall)
                        Text("SHA-256: ${sub.pdfSha256Hash}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Legal Declaration Accepted: ${if (sub.copyrightDeclarationAccepted) "YES (v${sub.copyrightDeclarationVersion})" else "NO"}", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Description:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(sub.description.ifBlank { "No synopsis provided." }, style = MaterialTheme.typography.bodySmall)

                        OutlinedTextField(
                            value = state.feedbackText,
                            onValueChange = { viewModel.onFeedbackChanged(it) },
                            label = { Text("Moderator Feedback / Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.reviewSelectedSubmission(SubmissionStatus.APPROVED) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        enabled = !state.isReviewing
                    ) {
                        Text("Approve & Award ₹1")
                    }
                },
                dismissButton = {
                    Row {
                        OutlinedButton(
                            onClick = { viewModel.reviewSelectedSubmission(SubmissionStatus.REJECTED) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reject")
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = { viewModel.onSelectSubmission(null) }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SubmissionCard(
    submission: BookSubmission,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(submission.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Surface(
                    color = when (submission.status) {
                        SubmissionStatus.APPROVED -> Color(0xFF2E7D32)
                        SubmissionStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> Color(0xFFE65100)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        submission.status.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("By ${submission.authorName} • Uploader: ${submission.uploaderName}", style = MaterialTheme.typography.bodySmall)
            Text("${submission.pdfPageCount} Pages • ${(submission.pdfFileSizeBytes / 1024)} KB • SHA: ${submission.pdfSha256Hash.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
