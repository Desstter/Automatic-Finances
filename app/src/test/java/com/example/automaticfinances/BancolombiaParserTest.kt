package com.example.automaticfinances

import com.example.automaticfinances.data.parse.BancolombiaParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Comprehensive unit tests for BancolombiaParser
 * Tests critical SMS parsing functionality for production readiness
 */
class BancolombiaParserTest {

    @Before
    fun setup() {
        // Parser uses repositories, but for unit testing we focus on parsing logic
        // Integration tests would test the full repository integration
    }

    @Test
    fun parse_bancolombiaCompra_success(): Unit = runBlocking {
        val sms = "Bancolombia: Compraste $25.500 en RAPPI con tu T.Cred *1234, el 19/08/2024 a las 16:30"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should parse valid Bancolombia purchase SMS", result)
        result?.let { tx ->
            assertEquals("COMPRA", tx.type)
            assertEquals(2550000L, tx.amountCents) // $25,500 = 2,550,000 centavos
            assertEquals("RAPPI", tx.description)
            assertEquals("1234", tx.srcLast4)
            assertEquals("COP", tx.currency)
            assertFalse("Should not be income", tx.isIncome)
        }
    }

    @Test
    fun parse_bancolombiaTransferencia_success(): Unit = runBlocking {
        val sms = "Bancolombia: Transferiste $100.000 desde tu cuenta *5678 a la cuenta *9999 el 20/08/2024 a las 10:15"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should parse valid Bancolombia transfer SMS", result)
        result?.let { tx ->
            assertEquals("TRANSFERENCIA", tx.type)
            assertEquals(10000000L, tx.amountCents) // $100,000 = 10,000,000 centavos
            assertTrue("Should contain transfer description", tx.description.contains("*9999"))
            assertEquals("5678", tx.srcLast4)
            assertEquals("9999", tx.dstLast4)
        }
    }

    @Test
    fun parse_bancolombiaRetiro_success(): Unit = runBlocking {
        val sms = "Bancolombia: Retiraste $50.000,00 en METR_LA70_1 de tu T.Deb *6045 el 19/08/2024 a las 16:11"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should parse valid ATM withdrawal SMS", result)
        result?.let { tx ->
            assertEquals("RETIRO", tx.type)
            assertEquals(5000000L, tx.amountCents) // $50,000 = 5,000,000 centavos
            assertTrue("Should contain ATM description", tx.description.contains("METR_LA70_1"))
            assertEquals("6045", tx.srcLast4)
        }
    }

    @Test
    fun parse_nequiRetiroEnCajero_success(): Unit = runBlocking {
        // Push de Nequi: el listener ensambla cuerpo + título. El texto NO contiene "Nequi"
        // (eso es solo el nombre de la app), así que esto valida el detector genérico de retiro.
        val push = "Sacaste $200000  Retiro en Cajero"

        val result = BancolombiaParser.tryParse(push)

        assertNotNull("Should parse a cardless ATM withdrawal push", result)
        result?.let { tx ->
            assertEquals("RETIRO", tx.type)
            assertEquals(20_000_000L, tx.amountCents) // $200,000 = 20,000,000 centavos
        }
    }

    @Test
    fun parse_bancolombiaIngreso_nomina_success(): Unit = runBlocking {
        val sms = "Bancolombia: Recibiste un pago de Nómina de EMPRESA XYZ por $2.500.000 en tu cuenta de Ahorros el 30/08/2024 a las 08:00"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should parse salary income SMS", result)
        result?.let { tx ->
            assertEquals("INGRESO", tx.type)
            assertEquals(250000000L, tx.amountCents) // $2,500,000 = 250,000,000 centavos
            assertTrue("Should contain employer name", tx.description.contains("EMPRESA XYZ"))
            assertTrue("Should be marked as income", tx.isIncome)
        }
    }

    @Test
    fun parse_nequiCompra_success(): Unit = runBlocking {
        val sms = "Nequi: Pagaste $15.000 en UBER TRIP"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should parse Nequi payment SMS", result)
        result?.let { tx ->
            assertEquals("COMPRA", tx.type)
            assertEquals(1500000L, tx.amountCents) // $15,000 = 1,500,000 centavos
            assertEquals("UBER TRIP", tx.description)
            assertEquals("NEQU", tx.srcLast4)
            assertEquals("notif:nequi", tx.source)
        }
    }

    @Test
    fun parse_invalidTransaction_rejected(): Unit = runBlocking {
        val sms = "Bancolombia: Compra rechazada por $50.000 en TIENDA XYZ"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNull("Should reject declined transactions", result)
    }

    @Test
    fun parse_nonBankingSms_ignored(): Unit = runBlocking {
        val sms = "Tu pedido de pizza está listo para recoger"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNull("Should ignore non-banking SMS", result)
    }

    @Test
    fun parse_multipleAmountFormats_success(): Unit = runBlocking {
        // Test different amount formats the parser should handle
        val testCases = listOf(
            "Bancolombia: Compraste $1.500,50 en TEST" to 150050L, // Colombian format with decimals
            "Bancolombia: Compraste $1500 en TEST" to 150000L,      // Simple format
            "Bancolombia: Compraste $1,500.00 en TEST" to 150000L,  // US format  
            "Bancolombia: Compraste $25.000 en TEST" to 2500000L    // Colombian thousands
        )
        
        testCases.forEach { (sms, expectedCents) ->
            val result = BancolombiaParser.tryParse(sms + " con tu T.Cred *1234, el 19/08/2024 a las 16:30")
            assertNotNull("Should parse amount format: $sms", result)
            assertEquals("Amount parsing failed for: $sms", expectedCents, result?.amountCents)
        }
    }

    @Test
    fun parse_emptyOrNullInput_handledGracefully(): Unit = runBlocking {
        assertNull("Should handle null input", BancolombiaParser.tryParse(""))
        assertNull("Should handle empty input", BancolombiaParser.tryParse(""))
        assertNull("Should handle whitespace input", BancolombiaParser.tryParse("   "))
    }

    @Test
    fun parse_timestampGeneration_consistent(): Unit = runBlocking {
        val sms = "Bancolombia: Compraste $1000 en TEST con tu T.Cred *1234, el 19/08/2024 a las 16:30"
        
        val result1 = BancolombiaParser.tryParse(sms)
        val result2 = BancolombiaParser.tryParse(sms)
        
        assertNotNull("First parse should succeed", result1)
        assertNotNull("Second parse should succeed", result2)
        
        // Same SMS should generate same stable ID for deduplication
        assertEquals("Same SMS should generate same ID", result1?.id, result2?.id)
    }

    @Test
    fun parse_nequiCompra_fullMerchantName_notTruncated(): Unit = runBlocking {
        // Regression: the old `\ben\s+(.+?)\b` regex captured only the first word ("UBER").
        val sms = "Nequi: Pagaste $15.000 en UBER TRIP BOGOTA"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse Nequi payment", result)
        assertEquals("Should capture the full merchant name", "UBER TRIP BOGOTA", result?.description)
    }

    @Test
    fun parse_nequiIngreso_fullSenderName_notTruncated(): Unit = runBlocking {
        val sms = "Nequi: Recibiste $50.000 de JUAN CARLOS PEREZ"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse Nequi income", result)
        assertTrue("Should be income", result!!.isIncome)
        assertEquals("Recibido de JUAN CARLOS PEREZ", result.description)
    }

    @Test
    fun parse_merchantStopsAtStructuralMarker(): Unit = runBlocking {
        // Merchant capture must stop before trailing structured info ("el dd/mm...").
        val sms = "DaviPlata: Compraste $8.000 en TIENDA DON JOSE el 01/02/2024"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull(result)
        assertEquals("TIENDA DON JOSE", result?.description)
    }

    @Test
    fun parse_isPure_leavesCategoryAndAccountUnresolved(): Unit = runBlocking {
        // The parser must not resolve category/account (done downstream by the use case).
        val sms = "Bancolombia: Compraste $25.500 en RAPPI con tu T.Cred *1234, el 19/08/2024 a las 16:30"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull(result)
        assertNull("categoryId must be resolved downstream", result?.categoryId)
        assertNull("accountId must be resolved downstream", result?.accountId)
    }

    @Test
    fun parse_sameNotification_sameStableId_forDeduplication(): Unit = runBlocking {
        val sms = "Nequi: Pagaste $15.000 en UBER TRIP"
        val postTime = 1_700_000_000_000L

        val a = BancolombiaParser.tryParse(sms, postTime)
        val b = BancolombiaParser.tryParse(sms, postTime)

        assertNotNull(a)
        assertEquals("Re-delivered notification must yield the same id", a?.id, b?.id)
    }

    @Test
    fun parse_specialCharactersInMerchant_handled(): Unit = runBlocking {
        val sms = "Bancolombia: Compraste $5000 en TIENDA-123_TEST! con tu T.Cred *1234, el 19/08/2024 a las 16:30"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should handle special characters", result)
        result?.let { tx ->
            assertTrue("Should preserve merchant name", tx.description.contains("TIENDA-123_TEST!"))
        }
    }

    @Test
    fun parse_bancolombiaPagaste_treatedAsCompra(): Unit = runBlocking {
        // Bancolombia also uses "Pagaste" (not just "Compraste") for card debits — must parse the
        // same way, otherwise these payments were silently dropped.
        val sms = "Bancolombia: Pagaste $40.000 en EXITO con tu T.Deb *1234 el 19/08/2024 a las 10:00"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse Bancolombia 'Pagaste' as a purchase", result)
        result?.let { tx ->
            assertEquals("COMPRA", tx.type)
            assertEquals(4000000L, tx.amountCents)
            assertEquals("EXITO", tx.description)
            assertEquals("1234", tx.srcLast4)
            assertFalse(tx.isIncome)
        }
    }

    @Test
    fun parse_nuPago_treatedAsCompra(): Unit = runBlocking {
        val sms = "Nu: Pagaste $30.000 en STARBUCKS"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse a Nu payment", result)
        result?.let { tx ->
            assertEquals("COMPRA", tx.type)
            assertEquals(3000000L, tx.amountCents)
            assertEquals("STARBUCKS", tx.description)
            assertEquals("notif:nu", tx.source)
            assertFalse(tx.isIncome)
        }
    }

    @Test
    fun parse_nuRecibiste_treatedAsIngreso(): Unit = runBlocking {
        val sms = "Nu: Recibiste $80.000 de MARIA LOPEZ"

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse a Nu received transfer", result)
        result?.let { tx ->
            assertEquals("INGRESO", tx.type)
            assertEquals(8000000L, tx.amountCents)
            assertTrue("Should be income", tx.isIncome)
            assertEquals("Recibido de MARIA LOPEZ", tx.description)
            assertEquals("notif:nu", tx.source)
        }
    }

    @Test
    fun parse_bancolombiaCompra_nuevoFormato_fechaAntesQueTarjeta(): Unit = runBlocking {
        // New Bancolombia SMS format: date comes before card info
        // "Compraste COP{amount} en {merchant}, el {date} a las {time}. Esta compra esta asociada a T.Cred *{last4}."
        val sms = "Bancolombia: Compraste COP62,00 en Google CLOUD 4VT9CH, el 01/06/2026 a las 12:38. Esta compra esta asociada a T.Cred *9335. Si tienes dudas, encuentranos aqui: 01800931987. Siempre contigo."

        val result = BancolombiaParser.tryParse(sms)

        assertNotNull("Should parse new Bancolombia SMS format", result)
        result?.let { tx ->
            assertEquals("COMPRA", tx.type)
            assertEquals(6200L, tx.amountCents) // COP 62,00 = 6200 centavos
            assertEquals("Google CLOUD 4VT9CH", tx.description)
            assertEquals("9335", tx.srcLast4)
            assertEquals("COP", tx.currency)
            assertFalse("Should not be income", tx.isIncome)
            assertNull("categoryId must be null (resolved downstream)", tx.categoryId)
        }
    }
}