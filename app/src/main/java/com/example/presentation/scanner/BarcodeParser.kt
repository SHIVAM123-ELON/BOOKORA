package com.example.presentation.scanner

data class ParsedBarcode(
    val rawValue: String,
    val cleanIdentifier: String,
    val formatLabel: String,
    val isIsbn: Boolean = false
)

object BarcodeParser {
    fun normalizeIsbn(raw: String): String {
        return raw.replace("-", "").replace(" ", "").trim()
    }

    fun normalizeCode(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed.substringAfterLast("/").substringBefore("?").substringBefore("#")
        }
        return normalizeIsbn(trimmed)
    }

    fun getFormatLabel(format: Int): String {
        return when (format) {
            256 -> "QR Code"
            32 -> "EAN-13"
            64 -> "EAN-8"
            128 -> "ITF"
            1 -> "Code 128"
            2 -> "Code 39"
            else -> "Barcode"
        }
    }

    fun parseScannedCode(raw: String): ParsedBarcode {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val lastSegment = trimmed.substringAfterLast("/").substringBefore("?").substringBefore("#")
            return ParsedBarcode(
                rawValue = raw,
                cleanIdentifier = lastSegment,
                formatLabel = "QR Code (Deep Link)",
                isIsbn = false
            )
        }
        val clean = normalizeIsbn(trimmed)
        val isIsbn = (clean.length == 10 || clean.length == 13) && clean.all { it.isDigit() || it == 'X' || it == 'x' }
        return ParsedBarcode(
            rawValue = raw,
            cleanIdentifier = clean,
            formatLabel = if (isIsbn) "ISBN / EAN-13" else "Barcode",
            isIsbn = isIsbn
        )
    }
}
