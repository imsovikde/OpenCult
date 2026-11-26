package org.opencult.scale.core.database

import org.opencult.scale.core.database.entity.HouseholdProfileEntity
import kotlin.math.abs

sealed interface HouseholdMatchOutcome {
    val assignmentState: String

    data class Unique(
        val profile: HouseholdProfileEntity,
    ) : HouseholdMatchOutcome {
        override val assignmentState: String = "confirmed"
    }

    data class Ambiguous(
        val candidateProfiles: List<HouseholdProfileEntity>,
        val fallbackProfile: HouseholdProfileEntity,
    ) : HouseholdMatchOutcome {
        override val assignmentState: String = "pending_review"
    }
}

/**
 * Household weigh-in auto-matcher (INV-09).
 * Confirms automatically ONLY when exactly 1 active profile's rolling baseline weight
 * falls within its `autoMatchWindowKg` (default `2.50 kg`). Zero or 2+ matches mark
 * `pending_review` so the interactive member chooser opens cleanly.
 */
object HouseholdMatcher {
    const val MAX_HOUSEHOLD_PROFILES = 10

    fun matchProfile(
        measuredWeightKg: Double,
        activeProfiles: List<HouseholdProfileEntity>,
        rollingWeightByProfileId: Map<String, Double>,
    ): HouseholdMatchOutcome {
        require(activeProfiles.isNotEmpty()) { "At least one household profile is required before matching." }
        if (activeProfiles.size == 1 && rollingWeightByProfileId[activeProfiles.first().id] == null) {
            return HouseholdMatchOutcome.Unique(activeProfiles.first())
        }

        val matching = activeProfiles.filter { profile ->
            val baseline = rollingWeightByProfileId[profile.id] ?: return@filter false
            abs(baseline - measuredWeightKg) <= profile.autoMatchWindowKg
        }

        return when (matching.size) {
            1 -> HouseholdMatchOutcome.Unique(matching.first())
            else -> {
                val candidates = matching.ifEmpty { activeProfiles }
                val fallback = activeProfiles.firstOrNull { it.isPrimaryHcUser } ?: activeProfiles.first()
                HouseholdMatchOutcome.Ambiguous(
                    candidateProfiles = candidates,
                    fallbackProfile = fallback,
                )
            }
        }
    }
}
