package com.example.rankup.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

enum class Gender { MALE, FEMALE, OTHER, UNKNOWN, MIXED }
enum class AgeGroup { JUNIOR_U18, SENIOR_18_45, VETERAN_45PLUS }
enum class ScoringType { GOALS, POINTS, SETS, TIME }
enum class CompetitionType { LEAGUE, TOURNAMENT, CASCADE, KNOCKOUT }
enum class CompetitionStatus { SCHEDULED, ONGOING, COMPLETED, CANCELLED }
enum class ParticipantType { INDIVIDUAL, TEAM }
enum class FieldType { INDOOR, OUTDOOR }
enum class SurfaceType { GRASS, TURF, HARDCOURT, CLAY }
enum class MatchStatus { SCHEDULED, CONFIRMED, INPROGRESS, COMPLETED, CANCELLED }
enum class TeamSide { HOME, AWAY, TEAM1, TEAM2 }
enum class ConfirmationStatus { PENDING, CONFIRMED, REJECTED, TENTATIVE }
enum class TeamAssignment { TEAM1, TEAM2, UNASSIGNED }
enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, TENTATIVE }
enum class WinnerSide { TEAM1, TEAM2, TIE, NONE }

@Entity
data class User(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val username: String,
    val email: String,
    val hashedPassword: String,
    val gender: Gender,
    val ageGroup: AgeGroup,
    val isCoach: Boolean,
    val isAdmin: Boolean,
    val city: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMatch: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity
data class Sport(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String,
    val maxPlayersPerTeam: Int,
    val minPlayersForMatch: Int,
    val scoringType: ScoringType,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = Sport::class, parentColumns = ["id"], childColumns = ["sportId"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["captainId"])
    ]
)
data class Team(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val sportId: UUID,
    val captainId: UUID,
    val gender: Gender,
    val ageGroup: AgeGroup,
    val city: String,
    val totalPoints: Int = 0,
    val lastMatch: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    foreignKeys = [
        ForeignKey(entity = Sport::class, parentColumns = ["id"], childColumns = ["sportId"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["competitionManagerId"])
    ]
)
data class Competition(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val sportId: UUID,
    val competitionManagerId: UUID,
    val startDate: Long,
    val endDate: Long,
    val type: CompetitionType,
    val status: CompetitionStatus,
    val participantType: ParticipantType,
    val participantCount: Int = 0,
    val maxParticipants: Int,
    val competitionRules: String,
    val winnerUserId: UUID? = null,
    val winnerTeamId: UUID? = null,
    val fieldId: UUID? = null,
    val location: String? = null,
    val city: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity
data class TeamMember(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val teamId: UUID,
    val userId: UUID,
    val joinedAt: Long = System.currentTimeMillis(),
    val leftAt: Long? = null,
    val isActive: Boolean = true
)

@Entity
data class Field(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val address: String,
    val city: String,
    val sportId: UUID,
    val capacity: Int,
    val type: FieldType,
    val surfaceType: SurfaceType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity
data class Match(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val sportId: UUID,
    val matchType: ParticipantType,
    val competitionId: UUID? = null,
    val scheduledDate: Long,
    val fieldId: UUID? = null,
    val location: String? = null,
    val city: String? = null,
    val status: MatchStatus,
    val createdByUserId: UUID,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
