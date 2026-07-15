package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allResumes: Flow<List<Resume>> = appDao.getAllResumes()
    val allCoverLetters: Flow<List<CoverLetter>> = appDao.getAllCoverLetters()

    suspend fun getResumeById(id: Int): Resume? = appDao.getResumeById(id)
    fun getResumeByIdFlow(id: Int): Flow<Resume?> = appDao.getResumeByIdFlow(id)

    suspend fun insertResume(resume: Resume): Long = appDao.insertResume(resume)
    suspend fun updateResume(resume: Resume) = appDao.updateResume(resume)
    suspend fun deleteResume(resume: Resume) = appDao.deleteResume(resume)

    suspend fun getCoverLetterById(id: Int): CoverLetter? = appDao.getCoverLetterById(id)
    fun getCoverLetterByIdFlow(id: Int): Flow<CoverLetter?> = appDao.getCoverLetterByIdFlow(id)

    suspend fun insertCoverLetter(coverLetter: CoverLetter): Long = appDao.insertCoverLetter(coverLetter)
    suspend fun updateCoverLetter(coverLetter: CoverLetter) = appDao.updateCoverLetter(coverLetter)
    suspend fun deleteCoverLetter(coverLetter: CoverLetter) = appDao.deleteCoverLetter(coverLetter)
}
