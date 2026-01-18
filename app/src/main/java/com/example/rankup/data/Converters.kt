package com.example.rankup.data

import androidx.room.TypeConverter
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromGender(value: Gender): String = value.name

    @TypeConverter
    fun toGender(value: String): Gender = Gender.valueOf(value)

    @TypeConverter
    fun fromAgeGroup(value: AgeGroup): String = value.name

    @TypeConverter
    fun toAgeGroup(value: String): AgeGroup = AgeGroup.valueOf(value)

    @TypeConverter
    fun fromScoringType(value: ScoringType): String = value.name

    @TypeConverter
    fun toScoringType(value: String): ScoringType = ScoringType.valueOf(value)

    @TypeConverter
    fun fromCompetitionType(value: CompetitionType): String = value.name

    @TypeConverter
    fun toCompetitionType(value: String): CompetitionType = CompetitionType.valueOf(value)

    @TypeConverter
    fun fromCompetitionStatus(value: CompetitionStatus): String = value.name

    @TypeConverter
    fun toCompetitionStatus(value: String): CompetitionStatus = CompetitionStatus.valueOf(value)

    @TypeConverter
    fun fromParticipantType(value: ParticipantType): String = value.name

    @TypeConverter
    fun toParticipantType(value: String): ParticipantType = ParticipantType.valueOf(value)

    @TypeConverter
    fun fromFieldType(value: FieldType): String = value.name

    @TypeConverter
    fun toFieldType(value: String): FieldType = FieldType.valueOf(value)

    @TypeConverter
    fun fromSurfaceType(value: SurfaceType): String = value.name

    @TypeConverter
    fun toSurfaceType(value: String): SurfaceType = SurfaceType.valueOf(value)

    @TypeConverter
    fun fromMatchStatus(value: MatchStatus): String = value.name

    @TypeConverter
    fun toMatchStatus(value: String): MatchStatus = MatchStatus.valueOf(value)
}
