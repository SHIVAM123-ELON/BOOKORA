package com.example

import com.example.presentation.scanner.BarcodeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookScannerAndWishlistTest {

    @Test
    fun testIsbnNormalization() {
        val rawIsbn = "978-0-13-235088-4"
        val normalized = BarcodeParser.normalizeIsbn(rawIsbn)
        assertEquals("9780132350884", normalized)
    }

    @Test
    fun testIsbnWithSpacesNormalization() {
        val rawIsbn = "978 0 13 468599 1"
        val normalized = BarcodeParser.normalizeIsbn(rawIsbn)
        assertEquals("9780134685991", normalized)
    }

    @Test
    fun testDeepLinkExtraction() {
        val url = "https://bookora.app/books/clean_code_001"
        val parsed = BarcodeParser.parseScannedCode(url)
        assertEquals("clean_code_001", parsed.cleanIdentifier)
        assertEquals("QR Code (Deep Link)", parsed.formatLabel)
    }

    @Test
    fun testIsbn13Extraction() {
        val raw = "9780132350884"
        val parsed = BarcodeParser.parseScannedCode(raw)
        assertEquals("9780132350884", parsed.cleanIdentifier)
        assertTrue(parsed.isIsbn)
    }
}
