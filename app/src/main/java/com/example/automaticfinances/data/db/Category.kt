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
    val isActive: Boolean = true,        // Para soft delete
    val isIncome: Boolean = false        // true para ingresos, false para gastos
)

// Categorías predefinidas
object DefaultCategories {
    val list = listOf(
        // Categorías de gastos
        Category(name = "Comida obligatoria", color = "#4CAF50", icon = "🍽️", isDefault = true, isIncome = false),
        Category(name = "Arriendo", color = "#2196F3", icon = "🏠", isDefault = true, isIncome = false),
        Category(name = "Salud", color = "#F44336", icon = "🏥", isDefault = true, isIncome = false),
        Category(name = "Comida por fuera", color = "#FF9800", icon = "🍔", isDefault = true, isIncome = false),
        Category(name = "Gasolina", color = "#795548", icon = "⛽", isDefault = true, isIncome = false),
        Category(name = "Transporte", color = "#607D8B", icon = "🚗", isDefault = true, isIncome = false),
        Category(name = "Entretenimiento", color = "#9C27B0", icon = "🎬", isDefault = true, isIncome = false),
        Category(name = "Ropa", color = "#E91E63", icon = "👕", isDefault = true, isIncome = false),
        Category(name = "Servicios", color = "#3F51B5", icon = "🔧", isDefault = true, isIncome = false),
        Category(name = "Otros gastos", color = "#9E9E9E", icon = "📦", isDefault = true, isIncome = false),
        
        // Categorías de ingresos
        Category(name = "Salario", color = "#4CAF50", icon = "💰", isDefault = true, isIncome = true),
        Category(name = "Bonos", color = "#2196F3", icon = "🎁", isDefault = true, isIncome = true),
        Category(name = "Venta personal", color = "#FF9800", icon = "🛒", isDefault = true, isIncome = true),
        Category(name = "Regalo", color = "#E91E63", icon = "🎀", isDefault = true, isIncome = true),
        Category(name = "Subsidio", color = "#9C27B0", icon = "🏛️", isDefault = true, isIncome = true),
        Category(name = "Otros ingresos", color = "#607D8B", icon = "💸", isDefault = true, isIncome = true)
    )
}