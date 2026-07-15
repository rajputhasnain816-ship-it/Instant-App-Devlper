package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Resume Queries ---
    @Query("SELECT * FROM resumes ORDER BY lastUpdated DESC")
    fun getAllResumes(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun getResumeById(id: Int): Resume?

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    fun getResumeByIdFlow(id: Int): Flow<Resume?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: Resume): Long

    @Update
    suspend fun updateResume(resume: Resume)

    @Delete
    suspend fun deleteResume(resume: Resume)

    // --- Cover Letter Queries ---
    @Query("SELECT * FROM cover_letters ORDER BY lastUpdated DESC")
    fun getAllCoverLetters(): Flow<List<CoverLetter>>

    @Query("SELECT * FROM cover_letters WHERE id = :id LIMIT 1")
    suspend fun getCoverLetterById(id: Int): CoverLetter?

    @Query("SELECT * FROM cover_letters WHERE id = :id LIMIT 1")
    fun getCoverLetterByIdFlow(id: Int): Flow<CoverLetter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverLetter(coverLetter: CoverLetter): Long

    @Update
    suspend fun updateCoverLetter(coverLetter: CoverLetter)

    @Delete
    suspend fun deleteCoverLetter(coverLetter: CoverLetter)
}
