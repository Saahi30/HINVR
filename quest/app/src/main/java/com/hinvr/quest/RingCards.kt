package com.hinvr.quest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Stone = Color(0xFF2A2118)
private val Cream = Color(0xFFF3E6D0)
private val CreamMuted = Color(0xFFD9C7A8)
private val Gold = Color(0xFFC9A227)
private val CardRadius = RoundedCornerShape(28.dp)

@Composable
fun SlotCard(slot: Int) {
    val index = RingSlots.shown[slot]
    if (index !in 0 until Ring.deckSize) return
    when (Ring.stage) {
        Stage.Splash, Stage.Pair, Stage.Profile -> return
        Stage.Menu -> ChoiceCard(MenuChoices[index], index)
        Stage.Live -> RingCardContent(Ring.cards[index], index)
        Stage.Tour -> ChoiceCard(TourChoices[index], index)
    }
}

@Composable
fun RingCardContent(card: RingCard, cardIndex: Int) {
    val centered = Ring.index == cardIndex
    val selected = centered && card.mandirId != null && Ring.selectedId == card.mandirId
    val borderWidth by animateDpAsState(
        targetValue = if (centered) 4.dp else 1.dp,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "card-border",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Gold else Gold.copy(alpha = if (centered) 0.85f else 0.35f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "card-gold",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardRadius)
            .background(Stone)
            .border(width = borderWidth, color = borderColor, shape = CardRadius)
            .clickable { Ring.focus(cardIndex) },
    ) {
        MandirCard(card, selected)
    }
}

@Composable
private fun ChoiceCard(choice: HomeChoice, cardIndex: Int) {
    val centered = Ring.index == cardIndex
    val borderWidth by animateDpAsState(
        targetValue = if (centered) 4.dp else 1.dp,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "choice-border",
    )
    val borderColor by animateColorAsState(
        targetValue = Gold.copy(alpha = if (centered) 0.9f else 0.35f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "choice-gold",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardRadius)
            .background(Stone)
            .border(width = borderWidth, color = borderColor, shape = CardRadius)
            .clickable { Ring.focus(cardIndex) },
    ) {
        Image(
            painter = painterResource(choice.photo),
            contentDescription = choice.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x66100B08),
                        0.4f to Color.Transparent,
                        1f to Color(0xE6100B08),
                    ),
                ),
        )
        BadgeChip(choice.badge, onTop = Ring.stage == Stage.Menu)
        if (Ring.stage == Stage.Tour) MenuChip()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Text(
                choice.place,
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )
            Text(
                choice.title,
                color = Cream,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                lineHeight = 32.sp,
            )
        }
    }
}

@Composable
private fun BoxScope.BadgeChip(badge: ChoiceBadge, onTop: Boolean) {
    when (badge) {
        ChoiceBadge.Live -> Text(
            "LIVE",
            color = Cream,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .background(Color(0xFFB3392B), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        ChoiceBadge.Vr360 -> Text(
            "VR360",
            color = Stone,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            modifier = Modifier
                .align(if (onTop) Alignment.TopCenter else Alignment.TopStart)
                .padding(top = 14.dp, start = if (onTop) 0.dp else 14.dp, end = if (onTop) 0.dp else 14.dp)
                .background(Gold, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun BoxScope.MenuChip() {
    Text(
        "Menu",
        color = Stone,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Gold)
            .clickable { Ring.back() }
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun MandirCard(card: RingCard, selected: Boolean) {
        Box(Modifier.fillMaxSize()) {
        if (card.liveUrl.isNotBlank()) {
            Text(
                "LIVE",
                color = Cream,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .background(Color(0xFFB3392B), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        MenuChip()
        card.photo?.let { photo ->
            Image(
                painter = painterResource(photo),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color(0xE6100B08),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Text(
                card.place,
                color = Gold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )
            Text(
                card.title,
                color = Cream,
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                lineHeight = 32.sp,
            )
            if (selected && card.liveUrl.isBlank()) {
                Text(
                    "No official stream",
                    color = CreamMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
