package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishPrimaryLight
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.presentation.components.ai.AuthorAiAssistantDialog
import com.example.presentation.viewmodel.AuthorAiAssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorStudioScreen(
    authorAiAssistantViewModel: AuthorAiAssistantViewModel? = null,
    onNavigateToFinancials: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Author & Publisher Studio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PolishSlate900
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PolishSlate900
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showAiDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Writing Assistant",
                        tint = PolishPrimaryIndigo
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBackground)
        )

        // Revenue Overview
        Column(modifier = Modifier.padding(20.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PolishSlate100),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Lifetime Royalty Earnings",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = PolishSlate500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$12,480.50",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimaryIndigo
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = PolishPrimaryLight
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Units Sold",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate500
                                )
                                Text(
                                    text = "3,420 copies",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimaryIndigo
                                    )
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Avg Rating",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate500
                                )
                                Text(
                                    text = "4.92 / 5.0",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishAccentOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action: AI Studio Assistant Card Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showAiDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PolishPrimaryLight.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, PolishPrimaryIndigo.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PolishPrimaryIndigo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Author AI Writing Copilot",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                            Text(
                                text = "Generate blurbs, subtitles & tags with Gemini AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishSlate700
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimaryIndigo
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Publish New Manuscript
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit New Manuscript",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Catalog & Manuscripts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PolishSlate900
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Catalog Items
            ManuscriptCard(
                title = "Clean Architecture for Mobile Systems",
                status = "PUBLISHED",
                statusColor = Color(0xFF10B981),
                salesCount = "2,140 sold",
                revenue = "$7,490.00"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ManuscriptCard(
                title = "Distributed Systems in Practice",
                status = "UNDER REVIEW",
                statusColor = PolishAccentOrange,
                salesCount = "Pre-order queue",
                revenue = "--"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ManuscriptCard(
                title = "Reactive State Modeling",
                status = "DRAFT",
                statusColor = PolishSlate400,
                salesCount = "Unpublished",
                revenue = "--"
            )
        }

        if (showAiDialog && authorAiAssistantViewModel != null) {
            AuthorAiAssistantDialog(
                userId = "author_1",
                initialTitle = "Mobile System Architecture",
                viewModel = authorAiAssistantViewModel,
                onApplySuggestion = { /* apply suggestion */ },
                onDismiss = { showAiDialog = false }
            )
        }
    }
}

@Composable
fun ManuscriptCard(
    title: String,
    status: String,
    statusColor: Color,
    salesCount: String,
    revenue: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishSlate100)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = PolishSlate900
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = salesCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = PolishSlate500
                    )
                }
            }

            Text(
                text = revenue,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PolishPrimaryIndigo
                )
            )
        }
    }
}
