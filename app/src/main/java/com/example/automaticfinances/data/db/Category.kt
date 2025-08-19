package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val name: String,                    // "Comida obligatoria", "Arriendo", etc.
    val color: String,                   // Color hex: "#FF5722"
    val icon: String,                    // Icono emoji o nombre: "🍔", "🏠", "⛽"
    val isDefault: Boolean = false,      // Categorías predefinidas
    val isActive: Boolean = true         // Para soft delete
)

// Categorías predefinidas
object DefaultCategories {
    val list = listOf(
        Category(name = "Comida obligatoria", color = "#4CAF50", icon = "🍽️", isDefault = true),
        Category(name = "Arriendo", color = "#2196F3", icon = "🏠", isDefault = true),
        Category(name = "Salud", color = "#F44336", icon = "🏥", isDefault = true),
        Category(name = "Comida por fuera", color = "#FF9800", icon = "🍔", isDefault = true),
        Category(name = "Gasolina", color = "#795548", icon = "⛽", isDefault = true),
        Category(name = "Transporte", color = "#607D8B", icon = "🚗", isDefault = true),
        Category(name = "Entretenimiento", color = "#9C27B0", icon = "🎬", isDefault = true),
        Category(name = "Ropa", color = "#E91E63", icon = "👕", isDefault = true),
        Category(name = "Servicios", color = "#3F51B5", icon = "🔧", isDefault = true),
        Category(name = "Otros", color = "#9E9E9E", icon = "📦", isDefault = true)
    )
}