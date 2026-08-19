package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.BookSortOption
import com.example.domain.model.UiState
import com.example.presentation.components.BookCard
import com.example.presentation.components.BookoraTopBar
import com.example.presentation.components.CategoryChip
import com.example.presentation.viewmodel.ExploreViewModel
import com.example.presentation.viewmodel.SemanticSearchViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@Composable
fun ExploreScreen(
    exploreViewModel: ExploreViewModel,
    semanticSearchViewModel: SemanticSearchViewModel? = null,
    onNavigateToBookDetails: (String) -> Unit
) {
    val query by exploreViewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by exploreViewModel.filter.collectAsStateWithLifecycle()
    val categoriesState by exploreViewModel.categories.collectAsStateWithLifecycle()
    val booksState by exploreViewModel.exploreBooks.collectAsStateWithLifecycle()
    val semanticResultsState by (semanticSearchViewModel?.searchResults?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<UiState<List<com.example.domain.model.SemanticSearchResult>>>(UiState.Empty) })

    var isSemanticMode by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        BookoraTopBar(
            title = "Explore Catalog",
            subtitle = if (isSemanticMode) "AI Semantic Discovery enabled" else "Search by title, author, category or topics",
            actions = {
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("sort_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Options",
                            tint = PolishPrimaryIndigo
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        BookSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (filter.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                            color = if (filter.sortOption == option) PolishPrimaryIndigo else PolishSlate900
                                        )
                                    )
                                },
                                onClick = {
                                    exploreViewModel.selectSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = {
                exploreViewModel.onSearchQueryChanged(it)
                if (isSemanticMode && it.length >= 3) {
                    semanticSearchViewModel?.performSemanticSearch(it)
                }
            },
            placeholder = {
                Text(
                    if (isSemanticMode) "Describe what you want to learn or read about..." else "Search clean architecture, AI, habits, design...",
                    color = PolishSlate400
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isSemanticMode) Icons.Default.AutoAwesome else Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isSemanticMode) PolishAccentOrange else PolishPrimaryIndigo
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        exploreViewModel.onSearchQueryChanged("")
                        semanticSearchViewModel?.clearSearch()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = PolishSlate400
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("explore_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = if (isSemanticMode) PolishAccentOrange else PolishPrimaryIndigo,
                unfocusedBorderColor = PolishSlate200
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Semantic Mode & Categories Horizontal Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Semantic Search Toggle Chip
            FilterChip(
                selected = isSemanticMode,
                onClick = {
                    isSemanticMode = !isSemanticMode
                    if (isSemanticMode && query.length >= 3) {
                        semanticSearchViewModel?.performSemanticSearch(query)
                    }
                },
                label = {
                    Text(
                        "AI Semantic Search",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSemanticMode) Color.White else PolishAccentOrange
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PolishAccentOrange,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = PolishSlate700
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isSemanticMode) PolishAccentOrange else PolishSlate200),
                modifier = Modifier.testTag("semantic_search_chip")
            )

            if (categoriesState is UiState.Success && !isSemanticMode) {
                val categories = (categoriesState as UiState.Success).data
                CategoryChip(
                    name = "All Books",
                    isSelected = filter.categoryId == null,
                    onClick = { exploreViewModel.selectCategory(null) }
                )
                categories.forEach { category ->
                    CategoryChip(
                        name = category.name,
                        isSelected = filter.categoryId == category.id,
                        onClick = { exploreViewModel.selectCategory(category.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active sort tag indicator & Results count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (val state = booksState) {
                is UiState.Success -> {
                    Text(
                        text = "${state.data.size} books found",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate500
                    )
                }
                is UiState.Empty -> {
                    Text(
                        text = "0 books found",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate500
                    )
                }
                else -> {
                    Text(
                        text = "Searching catalog...",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate500
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PolishSlate100
            ) {
                Text(
                    text = "Sorted by: ${filter.sortOption.label}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PolishSlate700,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Book Grid / State Render
        when (val state = booksState) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error Loading Books",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { exploreViewModel.resetFilters() }) {
                            Text("Reset Filters")
                        }
                    }
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
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = PolishSlate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No books found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different keyword or removing category filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { exploreViewModel.resetFilters() }) {
                            Text(
                                text = "Clear All Filters",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                        }
                    }
                }
            }
            is UiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.data) { book ->
                        BookCard(
                            book = book,
                            onClick = { onNavigateToBookDetails(book.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
