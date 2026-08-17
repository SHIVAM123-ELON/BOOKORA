package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.LibraryItem
import com.example.domain.model.UiState
import com.example.presentation.components.BookoraTopBar
import com.example.presentation.viewmodel.LibraryViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    onNavigateToReader: (String) -> Unit,
    onNavigateToExplore: () -> Unit
) {
    val selectedTab by libraryViewModel.selectedTab.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.libraryItems.collectAsStateWithLifecycle()
    val tabs = listOf(
        "ALL" to "All Books",
        "READING" to "Reading",
        "DOWNLOADED" to "Downloaded",
        "COMPLETED" to "Completed"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        BookoraTopBar(
            title = "My Library",
            subtitle = "Your purchased & entitled digital collection"
        )

        // Filter Tabs
        SecondaryTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
            containerColor = Color.White,
            contentColor = PolishPrimaryIndigo,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { (tabKey, tabTitle) ->
                val isSelected = selectedTab == tabKey
                Tab(
                    selected = isSelected,
                    onClick = { libraryViewModel.selectTab(tabKey) },
                    text = {
                        Text(
                            text = tabTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PolishPrimaryIndigo else PolishSlate500
                            )
                        )
                    }
                )
            }
        }

        when (val state = libraryState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PolishPrimaryIndigo)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = PolishSlate500,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = PolishSlate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No books found in this tab",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore the marketplace to discover books to add to your library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToExplore,
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Browse Marketplace")
                        }
                    }
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.data) { item ->
                        LibraryBookItem(
                            item = item,
                            onReadClick = { onNavigateToReader(item.book.id) },
                            onToggleDownload = { libraryViewModel.toggleDownload(item.book.id, item.isDownloaded) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryBookItem(
    item: LibraryItem,
    onReadClick: () -> Unit,
    onToggleDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onReadClick)
            .testTag("library_item_${item.book.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PolishSlate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.book.coverImageUrl,
                contentDescription = item.book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 76.dp, height = 104.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PolishSlate100)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PolishSlate900
                )

                Text(
                    text = item.book.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = PolishSlate500
                )

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { item.readingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PolishPrimaryIndigo,
                    trackColor = PolishSlate100
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${item.lastReadPage} / ${item.book.pageCount} (${item.readingProgress.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimaryIndigo
                        )
                    )

                    IconButton(
                        onClick = onToggleDownload,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                            contentDescription = if (item.isDownloaded) "Downloaded" else "Download",
                            tint = if (item.isDownloaded) PolishPrimaryIndigo else PolishSlate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
