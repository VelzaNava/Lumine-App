package com.thesis.lumine.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Product photos the user can swipe through, with page dots. A single photo
 * (or none) renders as a plain [JewelryImage] with no dots.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoPager(
    photos: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
    indicatorAlignment: Alignment = Alignment.TopCenter
) {
    if (photos.size <= 1) {
        JewelryImage(photos.firstOrNull(), contentDescription, modifier, iconSize)
        return
    }

    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            key = { page -> photos[page] },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            JewelryImage(
                imageUrl = photos[page],
                contentDescription = "$contentDescription, photo ${page + 1} of ${photos.size}",
                modifier = Modifier.fillMaxSize(),
                iconSize = iconSize
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(indicatorAlignment)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            repeat(photos.size) { index ->
                val selected = pagerState.currentPage == index
                val dotSize by animateDpAsState(if (selected) 8.dp else 6.dp, label = "pageDot")
                Box(
                    Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (selected) 1f else 0.55f))
                )
            }
        }
    }
}
