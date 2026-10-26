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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SlotCard(slot: Int) {
    val index = RingSlots.shown[slot]
    if (!Ring.showsCards || index !in 0 until Ring.deckSize) return
    when (Ring.stage) {
        Stage.Splash, Stage.Pair, Stage.Credits, Stage.Profile, Stage.Mandir -> return
        Stage.Menu -> MenuCard(MenuChoices[index], index)
        Stage.States -> Ring.states.getOrNull(index)?.let { RingCardContent(it, index, isState = true) }
        Stage.Live -> Ring.mandirsInState.getOrNull(index)?.let { RingCardContent(it, index, isState = false) }
        Stage.Tour -> TourCard(TourChoices[index], index)
    }
}

@Composable
private fun CardFrame(cardIndex: Int, highlight: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val centered = Ring.index == cardIndex
    val borderWidth by animateDpAsState(
        targetValue = if (centered) 4.dp else 1.dp,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "card-border",
    )
    val borderColor by animateColorAsState(
        targetValue = if (highlight) Gold else Gold.copy(alpha = if (centered) 0.9f else 0.35f),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "card-gold",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(PanelShape)
            .background(Stone)
            .border(width = borderWidth, color = borderColor, shape = PanelShape)
            // The only way a card is chosen: a trigger or pinch on the card itself.
            .clickable { Ring.focus(cardIndex) },
        content = content,
    )
}

@Composable
fun RingCardContent(card: RingCard, cardIndex: Int, isState: Boolean) {
    val centered = Ring.index == cardIndex
    val selected = centered && card.mandirId != null && Ring.selectedId == card.mandirId
    CardFrame(cardIndex, highlight = selected) {
        val photo = card.photo
        if (photo != null) {
            Image(
                painter = painterResource(photo),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1f to InkVeil,
                        ),
                    ),
            )
        } else {
            TypographicArt(card.title)
        }
        if (isState) {
            if (card.live == LiveState.Live) LiveChip(Modifier.align(Alignment.TopStart))
        } else if (card.hasStream) {
            Box(Modifier.align(Alignment.TopStart).padding(14.dp)) { StreamBadge(card.live, size = 15.sp) }
        }
        BackChip(stringResource(if (isState) R.string.chip_menu else R.string.chip_states))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Text(card.place, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
            if (photo != null) {
                Text(card.title, color = Cream, fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 32.sp)
            }
            if (selected && !card.hasStream) {
                Text(
                    stringResource(R.string.card_no_stream),
                    color = CreamMuted,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** For a mandir with no licensed photo: stone, a gold hairline and its name. Never another temple's picture. */
@Composable
private fun BoxScope.TypographicArt(title: String) {
    Box(
        Modifier
            .matchParentSize()
            .padding(18.dp)
            .border(1.dp, Gold.copy(alpha = 0.55f), PanelShape),
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(1.5.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))),
        )
        Text(
            title,
            color = Cream,
            fontFamily = FontFamily.Serif,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        Box(
            Modifier
                .width(56.dp)
                .height(1.5.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent))),
        )
    }
}

@Composable
private fun MenuCard(choice: MenuChoice, cardIndex: Int) {
    CardFrame(cardIndex) {
        PhotoWithShade(choice.photo, stringResource(choice.title))
        when (choice.badge) {
            ChoiceBadge.Live -> Text(
                stringResource(R.string.badge_darshan),
                color = Cream,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(14.dp)
                    .background(InkShade, PillShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
            ChoiceBadge.Vr360 -> GoldChip(stringResource(R.string.badge_recorded_360), Modifier.align(Alignment.TopCenter))
            ChoiceBadge.Sanctum -> GoldChip(stringResource(R.string.badge_sanctum), Modifier.align(Alignment.TopCenter))
        }
        CardTitle(stringResource(choice.place), stringResource(choice.title))
    }
}

@Composable
private fun TourCard(choice: HomeChoice, cardIndex: Int) {
    CardFrame(cardIndex) {
        PhotoWithShade(choice.photo, choice.title)
        GoldChip(stringResource(R.string.badge_recorded_360), Modifier.align(Alignment.TopStart))
        BackChip(stringResource(R.string.chip_menu))
        CardTitle(choice.place, choice.title)
    }
}

@Composable
private fun BoxScope.PhotoWithShade(photo: Int, description: String) {
    Image(
        painter = painterResource(photo),
        contentDescription = description,
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
                    1f to InkVeil,
                ),
            ),
    )
}

@Composable
private fun BoxScope.CardTitle(place: String, title: String) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(18.dp),
    ) {
        Text(place, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
        Text(title, color = Cream, fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 32.sp)
    }
}

@Composable
private fun GoldChip(label: String, modifier: Modifier) {
    Text(
        label,
        color = Ink,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.3.sp,
        modifier = modifier
            .padding(14.dp)
            .background(Gold, PillShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun LiveChip(modifier: Modifier) {
    Box(modifier.padding(14.dp)) { StreamBadge(LiveState.Live, size = 15.sp) }
}

@Composable
private fun BoxScope.BackChip(label: String) {
    Text(
        label,
        color = Ink,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(PillShape)
            .background(Gold)
            .clickable { Ring.back() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
