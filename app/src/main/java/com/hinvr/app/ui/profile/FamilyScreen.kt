package com.hinvr.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hinvr.app.R
import com.hinvr.app.data.Family
import com.hinvr.app.data.FamilyMemberRow
import com.hinvr.app.data.Relation
import com.hinvr.app.data.attempt
import com.hinvr.app.i18n.userMessage
import com.hinvr.app.navigation.LocalSessionRepository
import com.hinvr.app.ui.theme.Atmosphere
import com.hinvr.app.ui.components.FilterChip
import com.hinvr.app.ui.components.HinvrBackground
import com.hinvr.app.ui.components.HinvrGoldOutlineButton
import com.hinvr.app.ui.components.HinvrPrimaryButton
import com.hinvr.app.ui.components.IvoryCard
import com.hinvr.app.ui.components.SabhaSearchField
import com.hinvr.app.ui.components.SabhaTopBar
import com.hinvr.app.ui.theme.HinvrTheme
import com.hinvr.app.ui.theme.HinvrTypography
import kotlinx.coroutines.launch

/**
 * The people who come with the member: a name and a relation each, up to what the plan
 * carries. They have no login; the desk sees them with the member's check-in.
 */
@Composable
fun FamilyScreen(onBack: () -> Unit, onPlans: () -> Unit) {
    val session = LocalSessionRepository.current
    val context = LocalContext.current
    val colors = HinvrTheme.colors
    val scope = rememberCoroutineScope()
    val snap by session.snapshot.collectAsStateWithLifecycle()
    var members by remember { mutableStateOf<List<FamilyMemberRow>?>(null) }
    var limits by remember { mutableStateOf<Map<String, Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    // The id being edited; "" for a new one; null when the form is closed.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var relation by rememberSaveable { mutableStateOf(Relation.Mother.wire) }
    var confirming by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        attempt { session.familyMembers() }
            .onSuccess {
                members = it
                error = null
            }
            .onFailure { error = context.userMessage(it.message) }
    }
    LaunchedEffect(Unit) {
        limits = session.familyLimits()
        reload()
    }

    val limit = Family.limitFor(snap.tier, limits)
    val list = members
    val count = list?.size ?: 0

    fun save() {
        val id = editing ?: return
        val clean = Family.cleanName(name)
        if (clean.isEmpty()) return
        busy = true
        scope.launch {
            attempt {
                if (id.isEmpty()) {
                    session.addFamilyMember(clean, Relation.of(relation))
                } else {
                    session.updateFamilyMember(id, clean, Relation.of(relation))
                }
            }.onSuccess {
                editing = null
                name = ""
                reload()
            }.onFailure { error = context.userMessage(it.message) }
            busy = false
        }
    }

    HinvrBackground(atmosphere = Atmosphere.Sabha) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            SabhaTopBar(title = stringResource(R.string.family_title), onBack = onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(stringResource(R.string.family_body), style = HinvrTypography.bodyLarge, color = colors.inkMuted)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (limit == 0) stringResource(R.string.family_none_on_plan) else stringResource(R.string.family_usage, count, limit),
                    style = HinvrTypography.titleMedium,
                    color = colors.gold,
                )
                Spacer(Modifier.height(18.dp))

                when {
                    list == null && error == null -> Text(stringResource(R.string.opening), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    list != null && list.isEmpty() && editing == null ->
                        Text(stringResource(R.string.family_empty), style = HinvrTypography.bodyMedium, color = colors.inkMuted)
                    list != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val covered = Family.covered(list, limit).map { it.id }.toSet()
                        list.forEach { member ->
                            IvoryCard {
                                Text(member.name, style = HinvrTypography.titleMedium, color = colors.ink)
                                Text(
                                    stringResource(Relation.of(member.relation).label) +
                                        if (member.id in covered) "" else " · " + stringResource(R.string.family_not_covered),
                                    style = HinvrTypography.bodyMedium,
                                    color = colors.inkMuted,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Text(
                                        stringResource(R.string.family_edit),
                                        style = HinvrTypography.titleMedium,
                                        color = colors.vermillion,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clickable(role = Role.Button) {
                                                editing = member.id
                                                name = member.name
                                                relation = member.relation
                                            }
                                            .padding(vertical = 12.dp),
                                    )
                                    Text(
                                        stringResource(if (confirming == member.id) R.string.family_remove_confirm else R.string.family_remove),
                                        style = HinvrTypography.titleMedium,
                                        color = colors.vermillion,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clickable(role = Role.Button, enabled = !busy) {
                                                if (confirming != member.id) {
                                                    confirming = member.id
                                                    return@clickable
                                                }
                                                confirming = null
                                                busy = true
                                                scope.launch {
                                                    attempt { session.removeFamilyMember(member.id) }
                                                        .onFailure { error = context.userMessage(it.message) }
                                                    reload()
                                                    busy = false
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (editing != null) {
                    SabhaSearchField(name, { name = it.take(Family.NameMax) }, stringResource(R.string.family_name_hint))
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Relation.entries.forEach { option ->
                            FilterChip(stringResource(option.label), relation == option.wire) { relation = option.wire }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HinvrPrimaryButton(
                        text = stringResource(R.string.family_save),
                        enabled = !busy && Family.cleanName(name).isNotEmpty(),
                        onClick = { save() },
                    )
                    Spacer(Modifier.height(8.dp))
                    HinvrGoldOutlineButton(stringResource(R.string.cancel), onClick = {
                        editing = null
                        name = ""
                    })
                } else if (list != null && Family.canAdd(count, limit)) {
                    HinvrPrimaryButton(
                        text = stringResource(R.string.family_add),
                        enabled = !busy,
                        onClick = {
                            editing = ""
                            name = ""
                            relation = Relation.Mother.wire
                        },
                    )
                } else if (list != null) {
                    Text(
                        stringResource(if (limit == 0) R.string.family_upgrade_none else R.string.family_upgrade_full),
                        style = HinvrTypography.bodyMedium,
                        color = colors.inkMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    HinvrGoldOutlineButton(stringResource(R.string.family_see_plans), onClick = onPlans)
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = HinvrTypography.bodyMedium, color = colors.vermillion)
                }
            }
        }
    }
}
