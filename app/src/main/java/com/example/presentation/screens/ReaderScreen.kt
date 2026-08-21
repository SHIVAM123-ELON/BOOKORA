package com.example.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.example.domain.model.offline.CachedChapter
import com.example.domain.model.offline.DownloadStatus
import com.example.domain.model.offline.OfflineDownloadProgress
import com.example.presentation.components.ai.AiAssistantBottomSheet
import com.example.presentation.components.ai.AiStudyModeBottomSheet
import com.example.presentation.components.ai.AiSummaryBottomSheet
import com.example.presentation.viewmodel.AiReadingAssistantViewModel
import com.example.presentation.viewmodel.AiSummaryViewModel
import com.example.presentation.viewmodel.BookDetailsViewModel
import com.example.presentation.viewmodel.StudyModeViewModel
import com.example.presentation.viewmodel.offline.OfflineReaderViewModel
import com.example.presentation.viewmodel.review.ActiveReadingSessionState
import com.example.presentation.viewmodel.review.ReadingSessionViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900
import com.example.ui.theme.PolishSuccess
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookDetailsViewModel: BookDetailsViewModel,
    readingSessionViewModel: ReadingSessionViewModel? = null,
    offlineReaderViewModel: OfflineReaderViewModel? = null,
    aiReadingAssistantViewModel: AiReadingAssistantViewModel? = null,
    aiSummaryViewModel: AiSummaryViewModel? = null,
    studyModeViewModel: StudyModeViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToVoiceConversation: (String) -> Unit = {}
) {
    val bookState by bookDetailsViewModel.getBookState(bookId).collectAsStateWithLifecycle()
    val isWishlisted by bookDetailsViewModel.isInWishlist(bookId).collectAsStateWithLifecycle()
    val readingProgress by bookDetailsViewModel.getReadingProgress(bookId).collectAsStateWithLifecycle()
    val activeSession by (readingSessionViewModel?.activeSession ?: MutableStateFlow(ActiveReadingSessionState())).collectAsStateWithLifecycle()
    val readerVerification by (readingSessionViewModel?.readerVerification ?: MutableStateFlow(null)).collectAsStateWithLifecycle()

    // Offline Cache States from Room Database
    LaunchedEffect(bookId) {
        offlineReaderViewModel?.initializeForBook(bookId)
    }

    val cachedBook by (offlineReaderViewModel?.cachedBook ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val chapters by (offlineReaderViewModel?.chapters ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val currentChapter by (offlineReaderViewModel?.currentChapter ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val isBookCached by (offlineReaderViewModel?.isBookCached ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val downloadProgress by (offlineReaderViewModel?.downloadProgress ?: MutableStateFlow(OfflineDownloadProgress())).collectAsStateWithLifecycle()
    val isOfflineSimulated by (offlineReaderViewModel?.isOfflineSimulated ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val selectedChapterIndex by (offlineReaderViewModel?.selectedChapterIndex ?: MutableStateFlow(1)).collectAsStateWithLifecycle()

    val book = (bookState as? UiState.Success)?.data
    val totalPages = book?.pageCount ?: cachedBook?.totalPages ?: 350
    var currentPage by remember(readingProgress) {
        mutableIntStateOf(readingProgress?.currentPage ?: 1)
    }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var readerThemeIndex by remember { mutableIntStateOf(0) } // 0: Cream, 1: Sepia, 2: Dark
    var isBookmarked by remember { mutableStateOf(false) }
    var showChapterSelectorSheet by remember { mutableStateOf(false) }

    // Start tracking session when reader opens, and flush/stop when reader is dismissed
    DisposableEffect(bookId) {
        readingSessionViewModel?.startSession(
            bookId = bookId,
            startPage = currentPage,
            totalPages = totalPages
        )
        onDispose {
            readingSessionViewModel?.stopAndSaveSession()
        }
    }

    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var showStudyModeSheet by remember { mutableStateOf(false) }
    var showChapterSummarySheet by remember { mutableStateOf(false) }

    // Resolve chapter title & body from Room cache or fallback
    val resolvedChapterTitle = currentChapter?.chapterTitle
        ?: cachedBook?.subtitle
        ?: book?.subtitle
        ?: "Chapter $selectedChapterIndex: Core Principles"

    val resolvedChapterSubtitle = currentChapter?.chapterSubtitle ?: ""

    val resolvedContent = currentChapter?.content
        ?: cachedBook?.fullContent
        ?: """
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
                            text = book?.title ?: cachedBook?.title ?: "Reading Book",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = currentTextColor
                        )
                        Text(
                            text = resolvedChapterTitle,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
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
                    // Chapter Selector Sheet Action
                    IconButton(
                        onClick = { showChapterSelectorSheet = true },
                        modifier = Modifier.testTag("reader_chapters_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Table of Contents",
                            tint = currentTextColor
                        )
                    }

                    // Offline Download & Cache Status Action
                    IconButton(
                        onClick = {
                            if (isBookCached) {
                                offlineReaderViewModel?.removeBookFromOffline(bookId)
                            } else {
                                offlineReaderViewModel?.downloadBookForOffline(bookId)
                            }
                        },
                        modifier = Modifier.testTag("reader_offline_cache_btn")
                    ) {
                        when {
                            downloadProgress.status == DownloadStatus.DOWNLOADING ||
                            downloadProgress.status == DownloadStatus.PARSING_CHAPTERS ||
                            downloadProgress.status == DownloadStatus.CACHING_TO_ROOM -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = PolishPrimaryIndigo
                                )
                            }
                            isBookCached -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Stored in Local Room Database",
                                    tint = PolishSuccess
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Download for Offline Reading",
                                    tint = currentTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Gemini 3.1 Flash Live Voice Companion Action
                    IconButton(
                        onClick = { onNavigateToVoiceConversation(bookId) },
                        modifier = Modifier.testTag("top_voice_companion_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Voice Companion (Live API)",
                            tint = PolishPrimaryIndigo
                        )
                    }

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

            // Offline Cache Banner / Indicator
            AnimatedVisibility(
                visible = isBookCached || isOfflineSimulated,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = if (isOfflineSimulated) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isOfflineSimulated) Icons.Default.WifiOff else Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOfflineSimulated) Color(0xFFD97706) else Color(0xFF059669)
                            )
                            Text(
                                text = if (isOfflineSimulated) "Offline Mode Active • Reading from Room Cache" else "Cached in Room DB • ${chapters.size} Chapters Offline (${cachedBook?.formattedSize ?: "1.2 MB"})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isOfflineSimulated) Color(0xFFB45309) else Color(0xFF047857)
                            )
                        }

                        TextButton(
                            onClick = { offlineReaderViewModel?.toggleOfflineSimulated() },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isOfflineSimulated) "Go Online" else "Simulate Offline",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isOfflineSimulated) Color(0xFFB45309) else Color(0xFF047857)
                            )
                        }
                    }
                }
            }

            // Download in Progress Bar
            if (downloadProgress.status == DownloadStatus.DOWNLOADING ||
                downloadProgress.status == DownloadStatus.PARSING_CHAPTERS ||
                downloadProgress.status == DownloadStatus.CACHING_TO_ROOM) {
                LinearProgressIndicator(
                    progress = { downloadProgress.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = PolishPrimaryIndigo
                )
            }

            // Reading Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    if (resolvedChapterSubtitle.isNotBlank()) {
                        Text(
                            text = resolvedChapterSubtitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimaryIndigo
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = resolvedContent,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.6f).sp,
                            fontFamily = FontFamily.Serif,
                            color = currentTextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chapter Step Navigation
                    if (chapters.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = currentTextColor.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { offlineReaderViewModel?.previousChapter() },
                                enabled = selectedChapterIndex > 1,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, currentTextColor.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = currentTextColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Prev Chapter", color = currentTextColor)
                            }

                            Text(
                                text = "Chapter $selectedChapterIndex of ${chapters.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = currentTextColor.copy(alpha = 0.7f)
                            )

                            OutlinedButton(
                                onClick = { offlineReaderViewModel?.nextChapter() },
                                enabled = selectedChapterIndex < chapters.size,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, currentTextColor.copy(alpha = 0.2f))
                            ) {
                                Text("Next Chapter", color = currentTextColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = currentTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
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
                    // Live Session & Verification Tracker Bar
                    if (activeSession.isTracking) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = PolishPrimaryIndigo
                                )
                                Text(
                                    text = "Session: ${activeSession.formattedDuration}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = currentTextColor
                                )
                                if (activeSession.isPaused) {
                                    Text(
                                        text = "(Paused)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PolishAccentOrange
                                    )
                                }
                            }

                            // Verification status indicator badge
                            if (readerVerification?.isVerified == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Reader",
                                        modifier = Modifier.size(14.dp),
                                        tint = PolishSuccess
                                    )
                                    Text(
                                        text = "Verified Reader",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PolishSuccess
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = "${"%.0f".format(activeSession.currentProgressPercent)}% read",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = currentTextColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

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
                            readingSessionViewModel?.updateCurrentPage(currentPage)
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

        // Chapter Table of Contents Bottom Sheet
        if (showChapterSelectorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSelectorSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                            text = "Table of Contents",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )

                        if (isBookCached) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFECFDF5)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "Offline Cached",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (chapters.isEmpty()) {
                        Text(
                            text = "No chapter manifest downloaded. Download this book to store chapters in the Room database for full offline reading.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PolishSlate700
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                offlineReaderViewModel?.downloadBookForOffline(bookId)
                                showChapterSelectorSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download for Offline Reading")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(chapters) { ch ->
                                val isSelected = ch.chapterIndex == selectedChapterIndex
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            offlineReaderViewModel?.selectChapter(ch.chapterIndex)
                                            showChapterSelectorSheet = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFEEF2FF) else PolishSlate100
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, PolishPrimaryIndigo) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ch.chapterTitle,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PolishPrimaryIndigo else PolishSlate900
                                                )
                                            )
                                            if (!ch.chapterSubtitle.isNullOrBlank()) {
                                                Text(
                                                    text = ch.chapterSubtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PolishSlate700
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${ch.estimatedReadingMinutes} min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PolishSlate400
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // AI Assistant Sheet
        if (showAiAssistantSheet && aiReadingAssistantViewModel != null) {
            AiAssistantBottomSheet(
                userId = "current_user",
                bookId = bookId,
                bookTitle = book?.title ?: "Bookora Manuscript",
                chapterTitle = resolvedChapterTitle,
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
                chapterTitle = resolvedChapterTitle,
                chapterContent = resolvedContent,
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
                chapterTitle = resolvedChapterTitle,
                chapterContent = resolvedContent,
                viewModel = aiSummaryViewModel,
                onDismiss = { showChapterSummarySheet = false }
            )
        }
    }
}
