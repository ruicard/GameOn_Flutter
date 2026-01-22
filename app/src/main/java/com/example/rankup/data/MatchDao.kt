package com.example.rankup.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match)

    @Query("SELECT * FROM Match ORDER BY scheduledDate ASC")
    fun getAllMatches(): Flow<List<Match>>

    @Query("SELECT * FROM Match WHERE createdByUserId = :userId ORDER BY scheduledDate ASC")
    fun getMatchesByUser(userId: UUID): Flow<List<Match>>
}
