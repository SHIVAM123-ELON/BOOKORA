package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.Book
import com.example.domain.model.UiState
import com.example.presentation.components.BookoraTopBar
import com.example.presentation.viewmodel.WishlistViewModel
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@Composable
fun WishlistScreen(
    wishlistViewModel: WishlistViewModel,
    onNavigateToBookDetails: (String) -> Unit,
    onNavigateToExplore: () -> Unit
) {
    val wishlistState by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolishBackground)
    ) {
        BookoraTopBar(
            title = "Wishlist",
            subtitle = "Saved items with price alert monitoring"
        )

        when (val state = wishlistState) {
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
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = PolishSlate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your Wishlist is Empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PolishSlate900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Save books to receive price-drop and release notifications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PolishSlate500
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToExplore,
                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Explore Books")
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
                    items(state.data) { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wishlist_item_${book.id}"),
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
                                    model = book.coverImageUrl,
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 104.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PolishSlate100)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        color = PolishSlate900
                                    )
                                    Text(
                                        text = book.authorName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = PolishSlate500
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = book.discountedPrice,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PolishPrimaryIndigo
                                            )
                                        )
                                        if (book.discountPercentage > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = book.formattedPrice,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    textDecoration = TextDecoration.LineThrough,
                                                    color = PolishSlate400
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onNavigateToBookDetails(book.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "View Details",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        IconButton(
                                            onClick = { wishlistViewModel.removeFromWishlist(book.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = PolishSlate400,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
