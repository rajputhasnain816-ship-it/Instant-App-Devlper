package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkExperience(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class Education(
    val id: String = java.util.UUID.randomUUID().toString(),
    val degree: String = "",
    val school: String = "",
    val gradDate: String = ""
)

@Entity(tableName = "resumes")
data class Resume(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val summary: String = "",
    val skills: String = "", // comma-separated list of skills
    val experienceJson: String = "[]", // JSON array of WorkExperience
    val educationJson: String = "[]", // JSON array of Education
    val templateId: String = "executive", // executive, emerald, cosmic, stark
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "cover_letters")
data class CoverLetter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeId: Int = 0,
    val jobTitle: String = "",
    val companyName: String = "",
    val jobDescription: String = "",
    val recipientName: String = "",
    val content: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
