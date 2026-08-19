package com.example.presentation.components.review

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.review.RatingSummary
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate200
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate500
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900

@Composable
fun RatingDistributionBar(
    ratingSummary: RatingSummary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Big Rating Score and Count
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(100.dp)
        ) {
            Text(
                text = if (ratingSummary.totalReviews > 0) ratingSummary.formattedAverage else "0.0",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = PolishSlate900,
                    fontSize = 38.sp
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val filledStars = ratingSummary.averageRating.toInt()
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i <= filledStars) PolishAccentOrange else PolishSlate200,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${ratingSummary.totalReviews} ${if (ratingSummary.totalReviews == 1) "review" else "reviews"}",
                style = MaterialTheme.typography.labelSmall,
                color = PolishSlate500
            )

            if (ratingSummary.verifiedReviewsCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "${ratingSummary.verifiedReviewsCount} verified",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    )
                }
            }
        }

        // 5 to 1 Star Breakdown Bars
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            RatingBarRow(starLabel = "5★", count = ratingSummary.fiveStarCount, fraction = ratingSummary.fiveStarPercent)
            RatingBarRow(starLabel = "4★", count = ratingSummary.fourStarCount, fraction = ratingSummary.fourStarPercent)
            RatingBarRow(starLabel = "3★", count = ratingSummary.threeStarCount, fraction = ratingSummary.threeStarPercent)
            RatingBarRow(starLabel = "2★", count = ratingSummary.twoStarCount, fraction = ratingSummary.twoStarPercent)
            RatingBarRow(starLabel = "1★", count = ratingSummary.oneStarCount, fraction = ratingSummary.oneStarPercent)
        }
    }
}

@Composable
private fun RatingBarRow(
    starLabel: String,
    count: Int,
    fraction: Float
) {
    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "rating_bar")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = starLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = PolishSlate700,
            modifier = Modifier.width(20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PolishSlate200)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedFraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(PolishPrimaryIndigo)
            )
        }

        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = PolishSlate400,
            modifier = Modifier.width(22.dp)
        )
    }
}
