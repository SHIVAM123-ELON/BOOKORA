package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.domain.model.UiState
import com.example.presentation.components.AuthorCard
import com.example.presentation.components.BookCard
import com.example.presentation.components.CategoryChip
import com.example.presentation.components.DealCard
import com.example.presentation.components.EmptySectionNotice
import com.example.presentation.components.SectionErrorState
import com.example.presentation.components.SectionHeader
import com.example.presentation.components.SectionLoadingIndicator
import com.example.presentation.components.voice.VoiceCompanionBannerCard
import com.example.presentation.viewmodel.HomeViewModel
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
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.IconButton

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToBookDetails: (String) -> Unit,
    onNavigateToReader: (String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToAuthorStudio: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToVoiceConversation: (String) -> Unit = {},
    onNavigateToScanner: () -> Unit = {}
) {
    val continueReadingItem by homeViewModel.continueReading.collectAsStateWithLifecycle()
    val trendingBooksState by homeViewModel.trendingBooks.collectAsStateWithLifecycle()
    val bestSellerBooksState by homeViewModel.bestSellerBooks.collectAsStateWithLifecycle()
    val newReleaseBooksState by homeViewModel.newReleaseBooks.collectAsStateWithLifecycle()
    val recommendedBooksState by homeViewModel.recommendedBooks.collectAsStateWithLifecycle()
    val popularAuthorsState by homeViewModel.popularAuthors.collectAsStateWithLifecycle()
    val categoriesState by homeViewModel.categories.collectAsStateWithLifecycle()
    val dealsState by homeViewModel.deals.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header - Professional Polish Style
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, PolishSlate100)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            color = PolishPrimaryIndigo
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "BOOKORA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = PolishSlate900
                        )
                    }

                    // Header Action Icons (Subscriptions, Cart, Avatar)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSubscriptions,
                            modifier = Modifier.size(36.dp).testTag("home_top_subscriptions_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardMembership,
                                contentDescription = "Subscriptions",
                                tint = PolishSlate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToCart,
                            modifier = Modifier.size(36.dp).testTag("home_top_cart_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Cart",
                                tint = PolishPrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            color = PolishPrimaryLight,
                            border = BorderStroke(2.dp, Color.White),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimaryIndigo
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar in Header
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToExplore)
                        .testTag("home_search_bar"),
                    color = PolishSlate100,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PolishSlate400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search books, authors, ISBN...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PolishSlate400,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onNavigateToScanner,
                            modifier = Modifier
                                .size(32.dp)
                                .background(PolishPrimaryIndigo.copy(alpha = 0.1f), CircleShape)
                                .testTag("home_search_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Physical Book Barcode",
                                tint = PolishPrimaryIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Live Voice Companion Banner
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            VoiceCompanionBannerCard(
                onStartVoice = { onNavigateToVoiceConversation("") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. CONTINUE READING SECTION
        if (continueReadingItem != null) {
            val item = continueReadingItem!!
            SectionHeader(
                title = "Continue Reading",
                seeAllText = "My Library",
                onSeeAllClick = onNavigateToExplore
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { onNavigateToReader(item.book.id) }
                    .testTag("continue_reading_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PolishSlate100),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.book.coverImageUrl,
                        contentDescription = item.book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 72.dp, height = 98.dp)
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
                            text = "by ${item.book.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Page ${item.lastReadPage} of ${item.book.pageCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = PolishSlate400
                            )
                            Text(
                                text = "${item.readingProgress.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PolishPrimaryIndigo
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. CATEGORIES SECTION
        SectionHeader(
            title = "Browse Categories",
            seeAllText = "View All",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = categoriesState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No categories found.")
            is UiState.Success -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.data.forEach { category ->
                        CategoryChip(
                            name = category.name,
                            isSelected = false,
                            onClick = onNavigateToExplore
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. TRENDING BOOKS SECTION
        SectionHeader(
            title = "Trending Books",
            seeAllText = "See More",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = trendingBooksState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No trending books currently.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { book ->
                        BookCard(
                            book = book,
                            onClick = { onNavigateToBookDetails(book.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AUTHOR STUDIO PROMOTIONAL CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PolishPrimaryIndigo)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Limited Time Offer",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Digital Publishing\nWorkshop 2026",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = 26.sp,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToAuthorStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("author_studio_banner_btn")
                    ) {
                        Text(
                            text = "JOIN AUTHOR STUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PolishPrimaryIndigo
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. BEST SELLERS SECTION
        SectionHeader(
            title = "Best Sellers",
            seeAllText = "View All",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = bestSellerBooksState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No best sellers available.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { book ->
                        BookCard(
                            book = book,
                            onClick = { onNavigateToBookDetails(book.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. DEALS & SPECIAL OFFERS SECTION
        SectionHeader(
            title = "Special Deals & Discounts",
            seeAllText = "See All Deals",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = dealsState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No active deals right now.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { book ->
                        DealCard(
                            book = book,
                            onClick = { onNavigateToBookDetails(book.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. AI PERSONALIZED RECOMMENDATIONS SECTION
        SectionHeader(
            title = "AI Smart Recommendations",
            seeAllText = "See More",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = recommendedBooksState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No recommendations available.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { book ->
                        Column(modifier = Modifier.width(140.dp)) {
                            BookCard(
                                book = book,
                                onClick = { onNavigateToBookDetails(book.id) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PolishPrimaryLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = PolishPrimaryIndigo,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "Top Match: ${book.categoryName}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PolishPrimaryIndigo
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. NEW RELEASES SECTION
        SectionHeader(
            title = "New Releases",
            seeAllText = "View All",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = newReleaseBooksState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No new releases.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { book ->
                        BookCard(
                            book = book,
                            onClick = { onNavigateToBookDetails(book.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 8. POPULAR AUTHORS SECTION
        SectionHeader(
            title = "Popular Authors",
            seeAllText = "See All",
            onSeeAllClick = onNavigateToExplore
        )

        when (val state = popularAuthorsState) {
            is UiState.Loading -> SectionLoadingIndicator()
            is UiState.Error -> SectionErrorState(message = state.message, onRetry = { homeViewModel.refreshAll() })
            is UiState.Empty -> EmptySectionNotice("No authors found.")
            is UiState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.data) { author ->
                        AuthorCard(
                            author = author,
                            onClick = onNavigateToExplore
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
