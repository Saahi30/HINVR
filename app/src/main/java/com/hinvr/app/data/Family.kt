package com.hinvr.app.data

import androidx.annotation.StringRes
import com.hinvr.app.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Someone who comes with the member: a name and a relation, no login of their own. */
@Serializable
data class FamilyMemberRow(
    val id: String,
    val name: String,
    val relation: String,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class FamilyMemberInsert(
    @SerialName("user_id") val userId: String,
    val name: String,
    val relation: String,
)

@Serializable
data class FamilyMemberUpdate(val name: String, val relation: String)

const val FamilyLimitMessage = "Your plan has no room for more family."

enum class Relation(val wire: String, @StringRes val label: Int) {
    Mother("mother", R.string.relation_mother),
    Father("father", R.string.relation_father),
    Spouse("spouse", R.string.relation_spouse),
    Son("son", R.string.relation_son),
    Daughter("daughter", R.string.relation_daughter),
    Grandmother("grandmother", R.string.relation_grandmother),
    Grandfather("grandfather", R.string.relation_grandfather),
    Sibling("sibling", R.string.relation_sibling),
    Other("other", R.string.relation_other),
    ;

    companion object {
        fun of(wire: String): Relation = entries.firstOrNull { it.wire == wire } ?: Other
    }
}

object Family {
    const val NameMax = 60

    /** What each plan carries when the server hasn't said (app_settings.family_limits). */
    val DefaultLimits = mapOf(
        MembershipTier.None to 0,
        MembershipTier.Darshan to 0,
        MembershipTier.Gold to 2,
        MembershipTier.Platinum to 4,
        MembershipTier.Nri to 4,
    )

    fun limitFor(tier: MembershipTier, limits: Map<String, Int>?): Int =
        limits?.get(tier.name) ?: DefaultLimits[tier] ?: 0

    fun canAdd(count: Int, limit: Int): Boolean = count < limit

    /** The people the plan covers today: the first [limit] added. A downgrade keeps the rest listed but not admitted. */
    fun covered(members: List<FamilyMemberRow>, limit: Int): List<FamilyMemberRow> =
        members.sortedBy { it.createdAt }.take(limit.coerceAtLeast(0))

    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(NameMax)
}
