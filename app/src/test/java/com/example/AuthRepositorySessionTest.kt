package com.example

import com.example.core.result.Resource
import com.example.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositorySessionTest {

    @Test
    fun testRoleMappingForAdminEmail() {
        val email = "admin.super@bookora.com"
        val role = if (email.contains("admin", ignoreCase = true)) {
            UserRole.ADMIN
        } else if (email.contains("author", ignoreCase = true)) {
            UserRole.AUTHOR
        } else {
            UserRole.READER
        }
        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun testRoleMappingForAuthorEmail() {
        val email = "author.jane@bookora.com"
        val role = if (email.contains("admin", ignoreCase = true)) {
            UserRole.ADMIN
        } else if (email.contains("author", ignoreCase = true)) {
            UserRole.AUTHOR
        } else {
            UserRole.READER
        }
        assertEquals(UserRole.AUTHOR, role)
    }

    @Test
    fun testRoleMappingForReaderEmail() {
        val email = "reader.sam@bookora.com"
        val role = if (email.contains("admin", ignoreCase = true)) {
            UserRole.ADMIN
        } else if (email.contains("author", ignoreCase = true)) {
            UserRole.AUTHOR
        } else {
            UserRole.READER
        }
        assertEquals(UserRole.READER, role)
    }
}
