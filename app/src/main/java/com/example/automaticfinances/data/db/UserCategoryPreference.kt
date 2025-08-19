package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "user_category_preferences",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["merchantKey"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class UserCategoryPreference(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantKey: String,          // Merchant normalizado (lowercase, sin espacios extras)
    val categoryId: Long,             // FK a Category
    val confidence: Float = 1.0f,     // 0.0 - 1.0, confianza en la preferencia
    val frequency: Int = 1,           // Cuántas veces el usuario eligió esta categoría para este merchant
    val lastUsed: Long = System.currentTimeMillis(), // Timestamp de última vez usada
    val source: String = "user",      // "user" | "auto" | "suggestion"
    val isActive: Boolean = true      // Para soft delete si el usuario la rechaza
)

// Data class para análisis y reportes
data class CategoryAccuracy(
    val categoryId: Long,
    val categoryName: String,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val accuracy: Float
)

// Data class para sugerencias inteligentes
data class CategorySuggestion(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val confidence: Float,
    val reason: String,              // "Aprendizaje anterior" | "Palabras clave" | "Patrón similar"
    val merchantKey: String
)