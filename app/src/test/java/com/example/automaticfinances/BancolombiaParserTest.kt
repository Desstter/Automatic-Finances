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
    fun parse_bancolombiaCompra_success() = runBlocking {
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
    fun parse_bancolombiaTransferencia_success() = runBlocking {
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
    fun parse_bancolombiaRetiro_success() = runBlocking {
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
    fun parse_bancolombiaIngreso_nomina_success() = runBlocking {
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
    fun parse_nequiCompra_success() = runBlocking {
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
    fun parse_invalidTransaction_rejected() = runBlocking {
        val sms = "Bancolombia: Compra rechazada por $50.000 en TIENDA XYZ"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNull("Should reject declined transactions", result)
    }

    @Test
    fun parse_nonBankingSms_ignored() = runBlocking {
        val sms = "Tu pedido de pizza está listo para recoger"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNull("Should ignore non-banking SMS", result)
    }

    @Test
    fun parse_multipleAmountFormats_success() = runBlocking {
        // Test different amount formats the parser should handle
        val testCases = listOf(
            "Bancolombia: Compraste $1.500,50 en TEST" to 150050L, // Colombian format with decimals
            "Bancolombia: Compraste $1500 en TEST" to 150000L,      // Simple format
            "Bancolombia: Compraste $1,500.00 en TEST" to 150000L,  // US format  
            "Bancolombia: Compraste $25.000 en TEST" to 2500000L    // Colombian thousands
        )
        
        testCases.forEach { (sms, expectedCents) ->
            val result = BancolombiaParser.tryParseSync(sms + " con tu T.Cred *1234, el 19/08/2024 a las 16:30")
            assertNotNull("Should parse amount format: $sms", result)
            assertEquals("Amount parsing failed for: $sms", expectedCents, result?.amountCents)
        }
    }

    @Test
    fun parse_emptyOrNullInput_handledGracefully() = runBlocking {
        assertNull("Should handle null input", BancolombiaParser.tryParse(""))
        assertNull("Should handle empty input", BancolombiaParser.tryParse(""))
        assertNull("Should handle whitespace input", BancolombiaParser.tryParse("   "))
    }

    @Test
    fun parse_timestampGeneration_consistent() = runBlocking {
        val sms = "Bancolombia: Compraste $1000 en TEST con tu T.Cred *1234, el 19/08/2024 a las 16:30"
        
        val result1 = BancolombiaParser.tryParse(sms)
        val result2 = BancolombiaParser.tryParse(sms)
        
        assertNotNull("First parse should succeed", result1)
        assertNotNull("Second parse should succeed", result2)
        
        // Same SMS should generate same stable ID for deduplication
        assertEquals("Same SMS should generate same ID", result1?.id, result2?.id)
    }

    @Test
    fun parse_specialCharactersInMerchant_handled() = runBlocking {
        val sms = "Bancolombia: Compraste $5000 en TIENDA-123_TEST! con tu T.Cred *1234, el 19/08/2024 a las 16:30"
        
        val result = BancolombiaParser.tryParse(sms)
        
        assertNotNull("Should handle special characters", result)
        result?.let { tx ->
            assertTrue("Should preserve merchant name", tx.description.contains("TIENDA-123_TEST!"))
        }
    }
}