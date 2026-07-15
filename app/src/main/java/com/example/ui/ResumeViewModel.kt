package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.callGeminiApi
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CoverLetter
import com.example.data.Education
import com.example.data.JsonHelpers
import com.example.data.Resume
import com.example.data.WorkExperience
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResumeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    val allResumes: StateFlow<List<Resume>>
    val allCoverLetters: StateFlow<List<CoverLetter>>

    private val _currentResume = MutableStateFlow<Resume?>(null)
    val currentResume: StateFlow<Resume?> = _currentResume.asStateFlow()

    private val _currentCoverLetter = MutableStateFlow<CoverLetter?>(null)
    val currentCoverLetter: StateFlow<CoverLetter?> = _currentCoverLetter.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    init {
        val appDao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(appDao)

        allResumes = repository.allResumes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCoverLetters = repository.allCoverLetters.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Pre-populate mock data if empty
        viewModelScope.launch {
            repository.allResumes.collect { list ->
                if (list.isEmpty()) {
                    createMockData()
                }
            }
        }
    }

    private suspend fun createMockData() {
        val mockExperiences = listOf(
            WorkExperience(
                jobTitle = "Senior Android Engineer",
                company = "TechCorp",
                startDate = "2022-03",
                endDate = "Present",
                description = "Lead a team of 4 engineers building the flagship M3 consumer application. Refactored old XML layout codebase to 100% Jetpack Compose, improving UI rendering speed by 35%. Integrated offline Room database sync reducing offline data loss to zero."
            ),
            WorkExperience(
                jobTitle = "Android Developer",
                company = "NextGen Solutions",
                startDate = "2020-01",
                endDate = "2022-02",
                description = "Developed key modular features for Android e-commerce application. Optimized image cache pipelines using Coil, saving average customer mobile data usage by 20%."
            )
        )

        val mockEducations = listOf(
            Education(
                degree = "B.S. Computer Science",
                school = "Skyward Tech University",
                gradDate = "2019-06"
            )
        )

        val resumeId = repository.insertResume(
            Resume(
                title = "Senior Mobile Engineer Resume",
                fullName = "Alex Chun",
                email = "alex.chun@techcorp.com",
                phone = "+1 (555) 019-2834",
                website = "github.com/alexchun",
                summary = "Passionate and results-driven Senior Mobile Engineer with over 6 years of experience specializing in high-performance Android development. Proven track record of spearheading Compose migrations, optimizing local SQLite/Room data architectures, and engineering low-latency API integrations. Ambitious developer looking to leverage expertise in robust system architecture.",
                skills = "Kotlin, Jetpack Compose, Android SDK, Room DB, Coroutines, Flow, Retrofit, MVVM Architecture, Git, CI/CD",
                experienceJson = JsonHelpers.serializeExperiences(mockExperiences),
                educationJson = JsonHelpers.serializeEducation(mockEducations),
                templateId = "executive",
                lastUpdated = System.currentTimeMillis()
            )
        )

        repository.insertCoverLetter(
            CoverLetter(
                resumeId = resumeId.toInt(),
                jobTitle = "Lead Mobile Architect",
                companyName = "GlobalFin",
                jobDescription = "Looking for an expert developer who has built highly scalable Jetpack Compose architectures with deep knowledge of local storage and API design.",
                recipientName = "Samantha Sterling, Hiring Director",
                content = """
                    July 15, 2026
                    
                    Samantha Sterling, Hiring Director
                    GlobalFin Corp
                    100 Financial Way
                    New York, NY 10005
                    
                    Dear Ms. Sterling,
                    
                    I am writing to express my strong interest in the Lead Mobile Architect position at GlobalFin. With over six years of dedicated experience engineering robust, high-performance Android solutions and leading critical technical migrations, I am confident in my ability to elevate your mobile banking ecosystem.
                    
                    In my current role as Senior Android Engineer at TechCorp, I lead a team of four developers managing our flagship Material 3 consumer application. I spearheaded our complete migration to Jetpack Compose, which resulted in a 35% improvement in rendering speeds and substantially reduced our codebase size. Furthermore, my expertise in configuring advanced Room database architectures and synchronization layers directly aligns with GlobalFin's mandate for highly available, secure, offline-capable mobile architectures.
                    
                    Prior to this, at NextGen Solutions, I engineered advanced data and image caching pipelines using modern reactive patterns, achieving a 20% reduction in bandwidth consumption and ensuring seamless user experiences even in low-connectivity areas. 
                    
                    I am highly impressed by GlobalFin's commitment to delivering secure, cutting-edge financial services. My deep proficiency with Kotlin, modern architecture patterns (MVVM, Clean Architecture), and high-reliability local caching positions me perfectly to architect and deliver your next generation of mobile experiences.
                    
                    Thank you for your time and consideration. I welcome the opportunity to discuss how my technical leadership and mobile development expertise can drive success for GlobalFin's ambitious engineering goals.
                    
                    Sincerely,
                    
                    Alex Chun
                """.trimIndent(),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // --- Resume Actions ---
    fun selectResume(resume: Resume?) {
        _currentResume.value = resume
    }

    fun updateCurrentResumeState(updater: (Resume) -> Resume) {
        _currentResume.value?.let { current ->
            _currentResume.value = updater(current)
        }
    }

    fun saveCurrentResume() {
        val resume = _currentResume.value ?: return
        viewModelScope.launch {
            val updatedResume = resume.copy(lastUpdated = System.currentTimeMillis())
            if (updatedResume.id == 0) {
                val newId = repository.insertResume(updatedResume)
                _currentResume.value = updatedResume.copy(id = newId.toInt())
            } else {
                repository.updateResume(updatedResume)
                _currentResume.value = updatedResume
            }
        }
    }

    fun createNewResume(title: String = "Untitled Resume") {
        val newResume = Resume(
            title = title,
            fullName = "",
            email = "",
            phone = "",
            website = "",
            summary = "",
            skills = "",
            experienceJson = "[]",
            educationJson = "[]",
            templateId = "executive",
            lastUpdated = System.currentTimeMillis()
        )
        _currentResume.value = newResume
    }

    fun deleteResume(resume: Resume) {
        viewModelScope.launch {
            repository.deleteResume(resume)
            if (_currentResume.value?.id == resume.id) {
                _currentResume.value = null
            }
        }
    }

    fun duplicateResume(resume: Resume) {
        viewModelScope.launch {
            val duplicated = resume.copy(
                id = 0,
                title = "${resume.title} (Copy)",
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertResume(duplicated)
        }
    }

    // --- Cover Letter Actions ---
    fun selectCoverLetter(coverLetter: CoverLetter?) {
        _currentCoverLetter.value = coverLetter
    }

    fun updateCurrentCoverLetterState(updater: (CoverLetter) -> CoverLetter) {
        _currentCoverLetter.value?.let { current ->
            _currentCoverLetter.value = updater(current)
        }
    }

    fun saveCurrentCoverLetter() {
        val letter = _currentCoverLetter.value ?: return
        viewModelScope.launch {
            val updatedLetter = letter.copy(lastUpdated = System.currentTimeMillis())
            if (updatedLetter.id == 0) {
                val newId = repository.insertCoverLetter(updatedLetter)
                _currentCoverLetter.value = updatedLetter.copy(id = newId.toInt())
            } else {
                repository.updateCoverLetter(updatedLetter)
                _currentCoverLetter.value = updatedLetter
            }
        }
    }

    fun createNewCoverLetter(resumeId: Int) {
        val newLetter = CoverLetter(
            resumeId = resumeId,
            jobTitle = "",
            companyName = "",
            jobDescription = "",
            recipientName = "",
            content = "",
            lastUpdated = System.currentTimeMillis()
        )
        _currentCoverLetter.value = newLetter
    }

    fun deleteCoverLetter(coverLetter: CoverLetter) {
        viewModelScope.launch {
            repository.deleteCoverLetter(coverLetter)
            if (_currentCoverLetter.value?.id == coverLetter.id) {
                _currentCoverLetter.value = null
            }
        }
    }

    // --- AI Operations ---
    fun clearStatusMessage() {
        _aiStatusMessage.value = null
    }

    fun polishSummary(summaryText: String, onComplete: (String) -> Unit) {
        if (summaryText.isBlank()) return
        _isGenerating.value = true
        _aiStatusMessage.value = "Polishing summary..."

        viewModelScope.launch {
            val prompt = """
                You are a professional resume writer and career coach. Please polish the following professional summary to make it compelling, action-oriented, keyword-optimized for ATS systems, and highly impressive to recruiters. Return ONLY the polished summary text. Do not include any intro, outro, headers, quotes, or markdown wrappers.
                
                Here is the summary to polish:
                $summaryText
            """.trimIndent()

            val result = callGeminiApi(prompt)
            _isGenerating.value = false

            if (result.startsWith("Error")) {
                _aiStatusMessage.value = result
            } else {
                _aiStatusMessage.value = "Summary polished successfully!"
                onComplete(result.trim())
            }
        }
    }

    fun polishExperienceBullet(bulletText: String, onComplete: (String) -> Unit) {
        if (bulletText.isBlank()) return
        _isGenerating.value = true
        _aiStatusMessage.value = "Polishing bullet point..."

        viewModelScope.launch {
            val prompt = """
                You are an expert resume writer. Please rewrite the following work experience description or bullet point to use strong action verbs, quantifiable metrics, and industry keywords. Keep it concise, professional, and ATS-optimized. Return ONLY the polished text. Do not include intro, outro, or quotes.
                
                Here is the text to polish:
                $bulletText
            """.trimIndent()

            val result = callGeminiApi(prompt)
            _isGenerating.value = false

            if (result.startsWith("Error")) {
                _aiStatusMessage.value = result
            } else {
                _aiStatusMessage.value = "Bullet point polished!"
                onComplete(result.trim())
            }
        }
    }

    fun generateCoverLetter(resume: Resume, jobTitle: String, companyName: String, jobDescription: String, recipientName: String, onComplete: (String) -> Unit) {
        _isGenerating.value = true
        _aiStatusMessage.value = "Generating cover letter magic..."

        viewModelScope.launch {
            val experiences = JsonHelpers.parseExperiences(resume.experienceJson)
            val expText = experiences.joinToString("\n\n") { exp ->
                "${exp.jobTitle} at ${exp.company} (${exp.startDate} to ${exp.endDate}):\n${exp.description}"
            }

            val prompt = """
                You are an expert career counselor and professional writer. Generate a highly persuasive, tailored, and modern cover letter for the following role:
                
                Job Title: $jobTitle
                Company Name: $companyName
                Job Description:
                $jobDescription
                
                Use details from this candidate's resume to make the cover letter personalized and high-impact:
                Candidate Name: ${resume.fullName.ifBlank { "Alex Chun" }}
                Email: ${resume.email.ifBlank { "alex.chun@techcorp.com" }}
                Phone: ${resume.phone.ifBlank { "+1 (555) 019-2834" }}
                Website: ${resume.website.ifBlank { "github.com/alexchun" }}
                Professional Summary: ${resume.summary}
                Work Experience:
                ${expText.ifBlank { "No detailed experience provided yet. Emphasize fast learning and ambition." }}
                Skills: ${resume.skills}
                
                Maintain a professional, ambitious, and highly confident tone.
                Address the letter to '${recipientName.ifBlank { "Hiring Manager" }}'.
                Format the letter with a date at the top, a professional greeting, 3-4 structured body paragraphs highlighting matching strengths, and a professional sign-off.
                Return ONLY the cover letter text itself. Do NOT include markdown wrappers, notes, or explanations outside of the letter.
            """.trimIndent()

            val result = callGeminiApi(prompt)
            _isGenerating.value = false

            if (result.startsWith("Error")) {
                _aiStatusMessage.value = result
            } else {
                _aiStatusMessage.value = "Cover letter generated successfully!"
                onComplete(result.trim())
            }
        }
    }
}
