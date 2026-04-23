package com.example.semestra.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ExamEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ExamEvent)

    @Update
    suspend fun update(event: ExamEvent)

    @Delete
    suspend fun delete(event: ExamEvent)

    @Query("DELETE FROM exam_events WHERE event_id = :eventId")
    suspend fun deleteById(eventId: String): Int

    @Query("SELECT * FROM exam_events ORDER BY event_date ASC")
    suspend fun getAllEvents(): List<ExamEvent>

    @Query(
        """
        SELECT * FROM exam_events
        WHERE user_id = :userId AND status = 'SAVED'
        ORDER BY event_date ASC
        """
    )
    fun observeSavedForUser(userId: String): Flow<List<ExamEvent>>

    @Query(
        """
        SELECT * FROM exam_events
        WHERE user_id = :userId
          AND status = 'SAVED'
          AND event_date >= :startInclusive
          AND event_date < :endExclusive
        ORDER BY event_date ASC
        """
    )
    fun observeSavedInRange(userId: String, startInclusive: Long, endExclusive: Long): Flow<List<ExamEvent>>

    @Query(
        """
        SELECT * FROM exam_events
        WHERE user_id = :userId
          AND status = 'SAVED'
          AND event_date >= :startInclusive
          AND event_date < :endExclusive
        ORDER BY event_date ASC
        """
    )
    suspend fun getSavedInRange(userId: String, startInclusive: Long, endExclusive: Long): List<ExamEvent>

    @Query(
        """
        SELECT * FROM exam_events
        WHERE syllabus_id = :syllabusId AND status = 'RAW'
        ORDER BY event_date ASC
        """
    )
    suspend fun getRawBySyllabus(syllabusId: String): List<ExamEvent>

    @Query(
        """
        UPDATE exam_events SET status = :toSaved
        WHERE syllabus_id = :syllabusId AND status = :fromRaw
        """
    )
    suspend fun markRawSavedForSyllabus(
        syllabusId: String,
        fromRaw: EventStatus,
        toSaved: EventStatus
    ): Int

    @Query(
        """
        SELECT * FROM exam_events
        WHERE user_id = :userId AND status = 'SAVED' AND synced = 0
        ORDER BY event_date ASC
        """
    )
    suspend fun getUnsyncedSaved(userId: String): List<ExamEvent>

    @Query("DELETE FROM exam_events WHERE syllabus_id = :syllabusId AND status = :rawStatus")
    suspend fun deleteRawForSyllabus(syllabusId: String, rawStatus: EventStatus): Int

    @Query("UPDATE exam_events SET synced = 1, google_event_id = :googleEventId WHERE event_id = :eventId")
    suspend fun markSynced(eventId: String, googleEventId: String): Int

    @Query("DELETE FROM exam_events")
    suspend fun clearAll()
}
