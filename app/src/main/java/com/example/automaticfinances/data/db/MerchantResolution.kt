package com.example.automaticfinances.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Mapeo de merchants de gateway (PAYU*, MERCPAGO*) a nombres reales de comercio.
 *
 * Ejemplos:
 * - "PAYU*FONYOU" → "Recargas Telefónicas"
 * - "MERCPAGO*MERCADOLIBRE" → "MercadoLibre"
 * - "BOLD*VFS COLOMBIA SA" → "VFS Global (Visas)"
 *
 * Cuando se detecta un gateway merchant, el sistema:
 * 1. Busca si ya existe un mapping en esta tabla
 * 2. Si existe, usa el realMerchant y categoryId sugerido
 * 3. Si no existe, pregunta al usuario una vez y guarda el mapping
 * 4. Futuras transacciones del mismo gateway se resuelven automáticamente
 */
@Entity(
    tableName = "merchant_resolutions",
    indices = [Index(value = ["gatewayMerchant"], unique = true)]
)
data class MerchantResolution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Merchant tal como aparece en el SMS/email (gateway name).
     * Ejemplos: "PAYU*FONYOU", "MERCPAGO*MERCADOLIBRE", "BOLD*VFS"
     * Normalizado a mayúsculas sin espacios extras.
     */
    val gatewayMerchant: String,

    /**
     * Nombre real del comercio proporcionado por el usuario o pre-poblado.
     * Ejemplos: "Recargas Telefónicas", "MercadoLibre", "VFS Global (Visas)"
     */
    val realMerchant: String,

    /**
     * ID de la categoría sugerida para este merchant.
     * Puede ser null si no hay sugerencia de categoría.
     */
    val suggestedCategoryId: Long? = null,

    /**
     * Indica si este mapping fue pre-poblado (true) o creado por el usuario (false).
     * Los mappings pre-poblados son para merchants colombianos comunes.
     */
    val isPrePopulated: Boolean = false,

    /**
     * Contador de veces que se ha usado este mapping.
     * Incrementa cada vez que una transacción coincide con este gateway merchant.
     */
    val usageCount: Int = 0,

    /**
     * Timestamp de creación del mapping (epoch millis).
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Timestamp de último uso (epoch millis).
     */
    val lastUsedAt: Long? = null
)

/**
 * Merchants colombianos comunes pre-poblados en la base de datos.
 */
object DefaultMerchantResolutions {
    fun getList(categoryMap: Map<String, Long>): List<MerchantResolution> {
        return listOf(
            // Recargas y Telefonía
            MerchantResolution(
                gatewayMerchant = "PAYU*FONYOU",
                realMerchant = "Recargas Telefónicas",
                suggestedCategoryId = categoryMap["Recargas y Telefonía"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*RECARGAS",
                realMerchant = "Recargas Telefónicas",
                suggestedCategoryId = categoryMap["Recargas y Telefonía"],
                isPrePopulated = true
            ),

            // Entretenimiento
            MerchantResolution(
                gatewayMerchant = "PAYU*CINEMARK",
                realMerchant = "Cinemark",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*CINE COLOMBIA",
                realMerchant = "Cine Colombia",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*PROCINAL",
                realMerchant = "Procinal",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),

            // Comida por fuera
            MerchantResolution(
                gatewayMerchant = "PAYU*RAPPI",
                realMerchant = "Rappi",
                suggestedCategoryId = categoryMap["Comida por fuera"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*UBER EATS",
                realMerchant = "Uber Eats",
                suggestedCategoryId = categoryMap["Comida por fuera"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*DOMICILIOS",
                realMerchant = "Domicilios.com",
                suggestedCategoryId = categoryMap["Comida por fuera"],
                isPrePopulated = true
            ),

            // Compras Online
            MerchantResolution(
                gatewayMerchant = "MERCPAGO*MERCADOLIBRE",
                realMerchant = "MercadoLibre",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "MERCADOPAGO*MERCADOLIBRE",
                realMerchant = "MercadoLibre",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*MERCADOLIBRE",
                realMerchant = "MercadoLibre",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*AMAZON",
                realMerchant = "Amazon",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*ALIEXPRESS",
                realMerchant = "AliExpress",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*SHEIN",
                realMerchant = "Shein",
                suggestedCategoryId = categoryMap["Compras Online"],
                isPrePopulated = true
            ),

            // Trámites
            MerchantResolution(
                gatewayMerchant = "BOLD*VFS",
                realMerchant = "VFS Global (Visas)",
                suggestedCategoryId = categoryMap["Trámites"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "BOLD*VFS COLOMBIA SA",
                realMerchant = "VFS Global (Visas)",
                suggestedCategoryId = categoryMap["Trámites"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*VFS",
                realMerchant = "VFS Global (Visas)",
                suggestedCategoryId = categoryMap["Trámites"],
                isPrePopulated = true
            ),

            // Servicios / Streaming
            MerchantResolution(
                gatewayMerchant = "PAYU*NETFLIX",
                realMerchant = "Netflix",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*SPOTIFY",
                realMerchant = "Spotify",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*DISNEY PLUS",
                realMerchant = "Disney+",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*HBO MAX",
                realMerchant = "HBO Max",
                suggestedCategoryId = categoryMap["Entretenimiento"],
                isPrePopulated = true
            ),

            // Transporte
            MerchantResolution(
                gatewayMerchant = "PAYU*UBER",
                realMerchant = "Uber",
                suggestedCategoryId = categoryMap["Transporte"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*BEAT",
                realMerchant = "Beat",
                suggestedCategoryId = categoryMap["Transporte"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*DIDI",
                realMerchant = "DiDi",
                suggestedCategoryId = categoryMap["Transporte"],
                isPrePopulated = true
            ),

            // Ropa
            MerchantResolution(
                gatewayMerchant = "PAYU*ZARA",
                realMerchant = "Zara",
                suggestedCategoryId = categoryMap["Ropa"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*H&M",
                realMerchant = "H&M",
                suggestedCategoryId = categoryMap["Ropa"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*NIKE",
                realMerchant = "Nike",
                suggestedCategoryId = categoryMap["Ropa"],
                isPrePopulated = true
            ),
            MerchantResolution(
                gatewayMerchant = "PAYU*ADIDAS",
                realMerchant = "Adidas",
                suggestedCategoryId = categoryMap["Ropa"],
                isPrePopulated = true
            )
        )
    }
}
