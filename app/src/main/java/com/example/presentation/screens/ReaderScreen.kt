package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UiState
import com.example.presentation.components.ai.AiAssistantBottomSheet
import com.example.presentation.components.ai.AiStudyModeBottomSheet
import com.example.presentation.components.ai.AiSummaryBottomSheet
import com.example.presentation.viewmodel.AiReadingAssistantViewModel
import com.example.presentation.viewmodel.AiSummaryViewModel
import com.example.presentation.viewmodel.BookDetailsViewModel
import com.example.presentation.viewmodel.StudyModeViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookDetailsViewModel: BookDetailsViewModel,
    aiReadingAssistantViewModel: AiReadingAssistantViewModel? = null,
    aiSummaryViewModel: AiSummaryViewModel? = null,
    studyModeViewModel: StudyModeViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val bookState by bookDetailsViewModel.getBookState(bookId).collectAsStateWithLifecycle()
    val isWishlisted by bookDetailsViewModel.isInWishlist(bookId).collectAsStateWithLifecycle()
    val readingProgress by bookDetailsViewModel.getReadingProgress(bookId).collectAsStateWithLifecycle()

    val book = (bookState as? UiState.Success)?.data
    val totalPages = book?.pageCount ?: 350
    var currentPage by remember(readingProgress) {
        mutableIntStateOf(readingProgress?.currentPage ?: 1)
    }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var readerThemeIndex by remember { mutableIntStateOf(0) } // 0: Cream, 1: Sepia, 2: Dark
    var isBookmarked by remember { mutableStateOf(false) }

    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var showStudyModeSheet by remember { mutableStateOf(false) }
    var showChapterSummarySheet by remember { mutableStateOf(false) }

    val chapterTitle = book?.subtitle ?: "Chapter 1: Foundational Principles"
    val chapterContent = """
The central objective of software craftsmanship and architecture is to decouple core business logic from delivery mechanisms, databases, and third-party frameworks.

When architecture is designed cleanly:
1. The business domain model remains agnostic of external dependencies.
2. Repositories abstract persistence engines, whether SQLite/Room, Cloud Datastores, or in-memory caches.
3. Use cases orchestrate application-specific rules with strict unidirectional data flow.

By creating boundary interfaces between UI presentation layers, domain use cases, and data sources, the codebase achieves maximum testability and maintainability over decades of enterprise evolution.

"Good architecture makes the system easy to understand, easy to develop, easy to maintain, and easy to deploy."
— Robert C. Martin (Uncle Bob)

Key Architectural Axioms:
• Independent of Frameworks: The architecture does not depend on the existence of some library of feature-laden software.
• Testable: The business rules can be tested without the UI, Database, Web Server, or any other external element.
• Independent of UI: The UI can change easily without changing the rest of the system.
• Independent of Database: You can swap out PostgreSQL for Mongo, CouchDB, or Bigtable. Your business rules are not bound to the database.
    """.trimIndent()

    val creamBg = Color(0xFFFAF8F5)
    val sepiaBg = Color(0xFFF4ECD8)
    val darkBg = Color(0xFF1E293B)

    val currentBgColor = when (readerThemeIndex) {
        1 -> sepiaBg
        2 -> darkBg
        else -> creamBg
    }

    val currentTextColor = when (readerThemeIndex) {
        2 -> Color(0xFFE2E8F0)
        else -> Color(0xFF1E293B)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(currentBgColor)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Reading Book",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = currentTextColor
                        )
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = currentTextColor.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentTextColor
                        )
                    }
                },
                actions = {
                    // AI Assistant Action
                    IconButton(
                        onClick = { showAiAssistantSheet = true },
                        modifier = Modifier.testTag("top_ai_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = PolishPrimaryIndigo
                        )
                    }

                    // Study Mode Action
                    IconButton(
                        onClick = { showStudyModeSheet = true },
                        modifier = Modifier.testTag("top_study_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Study Mode",
                            tint = PolishPrimaryIndigo
                        )
                    }

                    // Chapter Summary Action
                    IconButton(
                        onClick = { showChapterSummarySheet = true },
                        modifier = Modifier.testTag("top_chapter_summary_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Chapter Summary",
                            tint = PolishAccentOrange
                        )
                    }

                    IconButton(onClick = { isBookmarked = !isBookmarked }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) PolishAccentOrange else currentTextColor
                        )
                    }
                    IconButton(
                        onClick = {
                            readerThemeIndex = (readerThemeIndex + 1) % 3
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "Theme",
                            tint = currentTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = currentBgColor)
            )

            // Reading Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = chapterContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.6f).sp,
                        fontFamily = FontFamily.Serif,
                        color = currentTextColor
                    )
                )
            }

            // Bottom Reader Navigation Bar & Progress
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = currentBgColor,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page $currentPage of $totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = currentTextColor
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (fontSize > 14f) fontSize -= 2f }
                            ) {
                                Text(
                                    "A-",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = currentTextColor
                                    )
                                )
                            }
                            IconButton(
                                onClick = { if (fontSize < 28f) fontSize += 2f }
                            ) {
                                Text(
                                    "A+",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = currentTextColor
                                    )
                                )
                            }
                        }
                    }

                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = {
                            currentPage = it.toInt()
                            bookDetailsViewModel.updateProgress(bookId, currentPage, totalPages)
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = PolishPrimaryIndigo,
                            activeTrackColor = PolishPrimaryIndigo
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Floating AI Reading Assistant Button
        ExtendedFloatingActionButton(
            onClick = { showAiAssistantSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .testTag("floating_ai_assistant_btn"),
            containerColor = PolishPrimaryIndigo,
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            text = {
                Text(
                    text = "Ask AI Assistant",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        )

        // AI Assistant Sheet
        if (showAiAssistantSheet && aiReadingAssistantViewModel != null) {
            AiAssistantBottomSheet(
                userId = "current_user",
                bookId = bookId,
                bookTitle = book?.title ?: "Bookora Manuscript",
                chapterTitle = chapterTitle,
                viewModel = aiReadingAssistantViewModel,
                onDismiss = { showAiAssistantSheet = false }
            )
        }

        // AI Study Mode Sheet
        if (showStudyModeSheet && studyModeViewModel != null) {
            AiStudyModeBottomSheet(
                userId = "current_user",
                bookId = bookId,
                bookTitle = book?.title ?: "Bookora Manuscript",
                chapterTitle = chapterTitle,
                chapterContent = chapterContent,
                viewModel = studyModeViewModel,
                onDismiss = { showStudyModeSheet = false }
            )
        }

        // AI Chapter Summary Sheet
        if (showChapterSummarySheet && aiSummaryViewModel != null) {
            AiSummaryBottomSheet(
                userId = "current_user",
                bookId = bookId,
                bookTitle = book?.title ?: "Bookora Manuscript",
                chapterTitle = chapterTitle,
                chapterContent = chapterContent,
                viewModel = aiSummaryViewModel,
                onDismiss = { showChapterSummarySheet = false }
            )
        }
    }
}

