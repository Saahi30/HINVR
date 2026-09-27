package com.hinvr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hinvr.app.data.MemberPlace
import com.hinvr.app.data.readyPlaces
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography

private val PlaceNames = listOf("Home", "Parents' home")

@Composable
fun MemberPlacesEditor(
    places: List<MemberPlace>,
    onChange: (List<MemberPlace>) -> Unit,
    onLocated: (city: String) -> Unit,
    onError: (String) -> Unit,
    allowAdd: Boolean = true,
) {
    val colors = HinvrTheme.colors
    val shown = if (allowAdd) places else places.take(1)
    Column {
        Text(if (allowAdd) "Places" else "Your address", style = HinvrTypography.labelLarge, color = colors.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            if (allowAdd) {
                "Name each one. Home, your parents' home, or any other place."
            } else {
                "One place for now. You can add more from your profile, or when you book."
            },
            style = HinvrTypography.bodyMedium,
            color = colors.inkMuted,
        )
        shown.forEachIndexed { index, place ->
            Spacer(Modifier.height(16.dp))
            PlaceField(
                value = place.label,
                onValueChange = { label ->
                    onChange(shown.replace(index, place.copy(label = label)))
                },
                label = "Name",
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceNames.forEach { suggestion ->
                    HinvrTextButton(
                        text = suggestion,
                        onClick = { onChange(shown.replace(index, place.copy(label = suggestion))) },
                    )
                }
            }
            PlaceField(
                value = place.address,
                onValueChange = { address ->
                    onChange(shown.replace(index, place.copy(address = address)))
                },
                label = "Address",
                singleLine = false,
            )
            UseMyLocationButton(
                onPlace = { found ->
                    onChange(shown.replace(index, place.copy(address = found.address)))
                    onLocated(found.city)
                },
                onError = onError,
            )
            if (allowAdd && shown.size > 1) {
                HinvrTextButton(
                    text = "Remove this place",
                    onClick = { onChange(shown.filterIndexed { i, _ -> i != index }) },
                )
            }
        }
        if (allowAdd) {
            Spacer(Modifier.height(8.dp))
            HinvrTextButton(
                text = "Add another place",
                onClick = { onChange(shown + MemberPlace()) },
            )
        }
    }
}

private fun List<MemberPlace>.replace(index: Int, place: MemberPlace): List<MemberPlace> =
    mapIndexed { i, item -> if (i == index) place else item }

@Composable
private fun PlaceField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    val colors = HinvrTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        label = { Text(label) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.ivory,
            unfocusedContainerColor = colors.ivory,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = colors.gold,
            focusedLabelColor = colors.gold,
            unfocusedLabelColor = colors.inkMuted,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
        ),
    )
}

@Composable
fun ServicePlacePicker(
    places: List<MemberPlace>,
    selected: MemberPlace?,
    onSelect: (MemberPlace) -> Unit,
    onSaveNew: (MemberPlace) -> Unit,
    onLocated: (city: String) -> Unit,
    onError: (String) -> Unit,
) {
    val colors = HinvrTheme.colors
    var adding by remember(places.isEmpty()) { mutableStateOf(places.isEmpty()) }
    var draft by remember { mutableStateOf(MemberPlace()) }
    Column {
        Text("Where", style = HinvrTypography.labelLarge, color = colors.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Choose a saved place, or add one for this booking.",
            style = HinvrTypography.bodyMedium,
            color = colors.inkMuted,
        )
        Spacer(Modifier.height(12.dp))
        places.forEach { place ->
            val on = place == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) colors.ivory else Color.Transparent)
                    .border(1.dp, if (on) colors.gold else colors.ink.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                    .clickable { onSelect(place) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(place.label.ifBlank { "Place" }, style = HinvrTypography.labelLarge, color = colors.ink)
                Text(place.address, style = HinvrTypography.bodyMedium, color = colors.inkMuted)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (!adding) {
            HinvrTextButton(text = "Add another place", onClick = { adding = true })
        } else {
            PlaceField(
                value = draft.label,
                onValueChange = { draft = draft.copy(label = it) },
                label = "Name",
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceNames.forEach { suggestion ->
                    HinvrTextButton(
                        text = suggestion,
                        onClick = { draft = draft.copy(label = suggestion) },
                    )
                }
            }
            PlaceField(
                value = draft.address,
                onValueChange = { draft = draft.copy(address = it) },
                label = "Address",
                singleLine = false,
            )
            UseMyLocationButton(
                onPlace = { found ->
                    draft = draft.copy(address = found.address)
                    onLocated(found.city)
                },
                onError = onError,
            )
            val ready = listOf(draft).readyPlaces()?.singleOrNull()
            HinvrPrimaryButton(
                text = "Save this place",
                enabled = ready != null,
                onClick = {
                    val place = ready ?: return@HinvrPrimaryButton
                    onSaveNew(place)
                    draft = MemberPlace()
                    adding = false
                },
            )
            if (places.isNotEmpty()) {
                HinvrTextButton(text = "Cancel", onClick = { adding = false })
            }
        }
    }
}
