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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.Book
import com.example.domain.model.UiState
import com.example.presentation.components.BookCard
import com.example.presentation.components.SectionHeader
import com.example.presentation.viewmodel.BookDetailsViewModel
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: String,
    bookDetailsViewModel: BookDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReader: (String) -> Unit,
    onNavigateToCheckout: (String) -> Unit = {},
    onNavigateToCart: (String) -> Unit = {}
) {
    val bookState by bookDetailsViewModel.getBookState(bookId).collectAsStateWithLifecycle()
    val isWishlisted by bookDetailsViewModel.isInWishlist(bookId).collectAsStateWithLifecycle()
    val isEntitled by bookDetailsViewModel.isBookEntitled(bookId).collectAsStateWithLifecycle()
    val readingProgress by bookDetailsViewModel.getReadingProgress(bookId).collectAsStateWithLifecycle()
    var showAiSummarySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        TopAppBar(
            title = {},
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
                IconButton(onClick = { bookDetailsViewModel.toggleWishlist(bookId) }) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else PolishSlate700
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PolishBackground)
        )

        when (val state = bookState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PolishPrimaryIndigo)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = PolishSlate500)
                }
            }
            is UiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Book not found", color = PolishSlate500)
                }
            }
            is UiState.Success -> {
                val currentBook = state.data
                val similarBooks by bookDetailsViewModel.getSimilarBooks(currentBook.categoryId, currentBook.id)
                    .collectAsStateWithLifecycle()
                val authorBooks by bookDetailsViewModel.getAuthorBooks(currentBook.authorId, currentBook.id)
                    .collectAsStateWithLifecycle()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Book Cover in Elevated Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            border = BorderStroke(1.dp, PolishSlate100)
                        ) {
                            AsyncImage(
                                model = currentBook.coverImageUrl,
                                contentDescription = currentBook.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(255.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Subtitle
                    Text(
                        text = currentBook.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PolishSlate900
                    )

                    if (currentBook.subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentBook.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PolishSlate500
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "By ${currentBook.authorName}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PolishPrimaryIndigo
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Specs Card (Rating, Pages, Language)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PolishSlate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "★",
                                        color = PolishAccentOrange,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.2f", currentBook.averageRating),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PolishSlate900
                                    )
                                }
                                Text(
                                    text = "${currentBook.totalReviews} reviews",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate400
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${currentBook.pageCount}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PolishSlate900
                                )
                                Text(
                                    text = "Pages",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate400
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentBook.language,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PolishSlate900
                                )
                                Text(
                                    text = "Language",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate400
                                )
                            }
                        }
                    }

                    // Reading progress banner if entitled
                    if (isEntitled && readingProgress != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PolishPrimaryLight.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, PolishPrimaryIndigo.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Reading Progress",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PolishPrimaryIndigo
                                        )
                                    )
                                    Text(
                                        text = "Page ${readingProgress!!.currentPage} of ${readingProgress!!.totalPages} (${readingProgress!!.percentage.toInt()}%)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = PolishSlate900
                                    )
                                }

                                Button(
                                    onClick = { onNavigateToReader(currentBook.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // BOOKORA AI Summary Highlight Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { showAiSummarySheet = true }
                            .testTag("ai_summary_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PolishPrimaryLight.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, PolishPrimaryIndigo.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
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
                                        text = "BOOKORA AI Summary",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PolishPrimaryIndigo
                                        )
                                    )
                                    Text(
                                        text = "Key ideas, topics & actionable takeaways",
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
                                    text = "View",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimaryIndigo
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description Section
                    Text(
                        text = "About this Book",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PolishSlate900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentBook.description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = PolishSlate700
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tags / ISBN
                    if (currentBook.isbn.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PolishSlate100
                            ) {
                                Text(
                                    text = "ISBN: ${currentBook.isbn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PolishSlate100
                            ) {
                                Text(
                                    text = currentBook.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PolishSlate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // More by this Author
                    if (authorBooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "More by ${currentBook.authorName}")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(authorBooks) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onNavigateToReader(book.id) }
                                )
                            }
                        }
                    }

                    // Similar Books
                    if (similarBooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        SectionHeader(title = "Similar Books")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(similarBooks) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onNavigateToReader(book.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Sticky Action Bottom Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White,
                    border = BorderStroke(1.dp, PolishSlate100)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Marketplace Price",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolishSlate400
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentBook.discountedPrice,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PolishPrimaryIndigo
                                    )
                                )
                                if (currentBook.discountPercentage > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentBook.formattedPrice,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.LineThrough,
                                            color = PolishSlate400
                                        )
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { onNavigateToReader(currentBook.id) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PolishSlate200)
                            ) {
                                Text("Preview", color = PolishSlate700)
                            }

                            if (isEntitled) {
                                Button(
                                    onClick = { onNavigateToReader(currentBook.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("read_now_button")
                                ) {
                                    Text(
                                        "Read Now",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onNavigateToCheckout(currentBook.id)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("buy_book_button")
                                ) {
                                    Text(
                                        "Buy Now",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showAiSummarySheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showAiSummarySheet = false },
                        sheetState = sheetState,
                        containerColor = Color.White,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
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
                                        shape = CircleShape,
                                        color = PolishPrimaryLight,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PolishPrimaryIndigo,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Executive AI Brief",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PolishSlate900
                                        )
                                        Text(
                                            text = currentBook.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PolishSlate500,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(onClick = { showAiSummarySheet = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = PolishSlate500
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Key Core Takeaways",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishSlate900
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = PolishPrimaryLight.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, PolishSlate100)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = PolishAccentOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = currentBook.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PolishSlate900,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Recommended Audience & Topics",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PolishSlate900
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentBook.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PolishSlate100,
                                        border = BorderStroke(1.dp, PolishSlate200)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = PolishPrimaryIndigo
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showAiSummarySheet = false },
                                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Got It", style = MaterialTheme.typography.labelLarge)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
