package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonHelpers {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun parseExperiences(json: String): List<WorkExperience> {
        val type = Types.newParameterizedType(List::class.java, WorkExperience::class.java)
        val adapter = moshi.adapter<List<WorkExperience>>(type)
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeExperiences(list: List<WorkExperience>): String {
        val type = Types.newParameterizedType(List::class.java, WorkExperience::class.java)
        val adapter = moshi.adapter<List<WorkExperience>>(type)
        return adapter.toJson(list)
    }

    fun parseEducation(json: String): List<Education> {
        val type = Types.newParameterizedType(List::class.java, Education::class.java)
        val adapter = moshi.adapter<List<Education>>(type)
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeEducation(list: List<Education>): String {
        val type = Types.newParameterizedType(List::class.java, Education::class.java)
        val adapter = moshi.adapter<List<Education>>(type)
        return adapter.toJson(list)
    }
}
