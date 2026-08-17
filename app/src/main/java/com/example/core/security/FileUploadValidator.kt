package com.example.core.security

/**
 * File Upload Security & Magic Byte Inspection Validator.
 * Prevents disguised executable uploads (e.g. polyglot files, malicious scripts masquerading as PDF/EPUB/images).
 */
object FileUploadValidator {

    private const val MAX_EBOOK_SIZE_BYTES = 50 * 1024 * 1024L // 50MB max for PDF/EPUB
    private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L   // 5MB max for cover art
    private const val MAX_AUDIO_SIZE_BYTES = 100 * 1024 * 1024L // 100MB max for audiobooks

    // Magic Byte Signatures
    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK.. (EPUB is a ZIP package)
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // .PNG
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) // JPEG SOI

    enum class AllowedFileType(
        val mimeType: String,
        val extensions: List<String>,
        val maxSizeBytes: Long
    ) {
        PDF("application/pdf", listOf("pdf"), MAX_EBOOK_SIZE_BYTES),
        EPUB("application/epub+zip", listOf("epub"), MAX_EBOOK_SIZE_BYTES),
        IMAGE_PNG("image/png", listOf("png"), MAX_IMAGE_SIZE_BYTES),
        IMAGE_JPEG("image/jpeg", listOf("jpg", "jpeg"), MAX_IMAGE_SIZE_BYTES)
    }

    data class ValidationResult(
        val isAllowed: Boolean,
        val detectedType: AllowedFileType? = null,
        val errorMessage: String? = null
    )

    /**
     * Inspects file name, declared MIME type, size, and real byte header signatures.
     */
    fun validateUpload(
        fileName: String,
        fileSizeBytes: Long,
        fileHeaderBytes: ByteArray,
        expectedCategory: String // "ebook" or "cover"
    ): ValidationResult {
        // 1. Check file size
        val maxLimit = if (expectedCategory == "cover") MAX_IMAGE_SIZE_BYTES else MAX_EBOOK_SIZE_BYTES
        if (fileSizeBytes > maxLimit) {
            return ValidationResult(
                isAllowed = false,
                errorMessage = "File size (${fileSizeBytes / (1024 * 1024)}MB) exceeds allowable limit (${maxLimit / (1024 * 1024)}MB)"
            )
        }
        if (fileSizeBytes <= 0) {
            return ValidationResult(isAllowed = false, errorMessage = "Empty file submitted")
        }

        // 2. Validate Extension
        val extension = fileName.substringAfterLast(".", "").lowercase()
        if (extension.isEmpty()) {
            return ValidationResult(isAllowed = false, errorMessage = "File must have an explicit extension")
        }

        // 3. Inspect Header Magic Bytes
        if (fileHeaderBytes.size < 4) {
            return ValidationResult(isAllowed = false, errorMessage = "Incomplete file header bytes")
        }

        val matchesPdf = startsWith(fileHeaderBytes, PDF_MAGIC)
        val matchesZipEpub = startsWith(fileHeaderBytes, ZIP_MAGIC)
        val matchesPng = startsWith(fileHeaderBytes, PNG_MAGIC)
        val matchesJpeg = startsWith(fileHeaderBytes, JPEG_MAGIC)

        if (expectedCategory == "ebook") {
            if (extension == "pdf" && matchesPdf) {
                return ValidationResult(isAllowed = true, detectedType = AllowedFileType.PDF)
            } else if (extension == "epub" && matchesZipEpub) {
                return ValidationResult(isAllowed = true, detectedType = AllowedFileType.EPUB)
            } else {
                return ValidationResult(
                    isAllowed = false,
                    errorMessage = "Disguised or corrupted ebook file. Magic byte signature does not match '.$extension' format."
                )
            }
        } else if (expectedCategory == "cover") {
            if ((extension == "jpg" || extension == "jpeg") && matchesJpeg) {
                return ValidationResult(isAllowed = true, detectedType = AllowedFileType.IMAGE_JPEG)
            } else if (extension == "png" && matchesPng) {
                return ValidationResult(isAllowed = true, detectedType = AllowedFileType.IMAGE_PNG)
            } else {
                return ValidationResult(
                    isAllowed = false,
                    errorMessage = "Disguised or corrupted image file. Header does not match valid PNG/JPEG signatures."
                )
            }
        }

        return ValidationResult(isAllowed = false, errorMessage = "Unsupported file format or category")
    }

    private fun startsWith(source: ByteArray, prefix: ByteArray): Boolean {
        if (source.size < prefix.size) return false
        for (i in prefix.indices) {
            if (source[i] != prefix[i]) return false
        }
        return true
    }
}
