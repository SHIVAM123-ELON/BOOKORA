package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.result.Resource
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.LibraryEntity
import com.example.data.repository.financial.EntitlementRepositoryImpl
import com.example.domain.financial.RazorpayBackendService
import com.example.domain.financial.RazorpayConfig
import com.example.domain.financial.RazorpaySignatureVerifier
import com.example.domain.model.financial.OrderStatus
import com.example.domain.model.financial.PaymentStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RazorpayIntegrationTest {

    private lateinit var database: BookoraDatabase
    private lateinit var entitlementRepository: EntitlementRepositoryImpl
    private lateinit var razorpayBackendService: RazorpayBackendService

    private val testKeyId = "rzp_test_TSGcMCkfMrL3Yg"
    private val testKeySecret = "xxaCXvxhFre61PeDpe82Dh1T"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BookoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        entitlementRepository = EntitlementRepositoryImpl(database)
        razorpayBackendService = RazorpayBackendService(
            database = database,
            entitlementRepository = entitlementRepository,
            keyId = testKeyId,
            keySecret = testKeySecret
        )

        // Seed a sample book
        val sampleBook = BookEntity(
            id = "book_atomic_habits_01",
            title = "Atomic Habits",
            subtitle = "Tiny Changes, Remarkable Results",
            authorId = "auth_james_clear",
            authorName = "James Clear",
            description = "An easy and proven way to build good habits and break bad ones.",
            coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c",
            fileUrl = "https://example.com/books/atomic-habits.epub",
            previewUrl = "https://example.com/previews/atomic-habits.pdf",
            categoryId = "cat_self_help",
            categoryName = "Self-Help",
            language = "English",
            price = 499.0, // ₹499.00 -> 49900 paise
            discountPrice = 299.0, // ₹299.00 -> 29900 paise
            rating = 4.9,
            reviewCount = 12400,
            pageCount = 320,
            publicationDate = "2018-10-16",
            isbn = "978-0735211292",
            tags = "habits,self-improvement,productivity",
            isFeatured = true,
            isTrending = true,
            isBestSeller = true,
            isNewRelease = false,
            status = "PUBLISHED"
        )
        runBlocking {
            database.bookDao().insertBook(sampleBook)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRazorpaySignatureVerifier_validAndInvalidSignatures() {
        val orderId = "order_rzp_9876543210"
        val paymentId = "pay_rzp_1234567890"

        // Calculate expected HMAC SHA-256 signature
        val validSignature = RazorpaySignatureVerifier.calculateHmacSha256(
            payload = "$orderId|$paymentId",
            secret = testKeySecret
        )

        assertNotNull(validSignature)
        assertTrue(validSignature.isNotBlank())

        // Verify valid signature passes
        val isValid = RazorpaySignatureVerifier.verifySignature(
            orderId = orderId,
            paymentId = paymentId,
            signature = validSignature,
            keySecret = testKeySecret
        )
        assertTrue("Authentic signature should verify successfully", isValid)

        // Verify tampered signature fails
        val isTamperedValid = RazorpaySignatureVerifier.verifySignature(
            orderId = orderId,
            paymentId = paymentId,
            signature = "tampered_fake_signature_hash_123456789",
            keySecret = testKeySecret
        )
        assertFalse("Tampered signature must be rejected", isTamperedValid)

        // Verify empty fields fail safely
        assertFalse(RazorpaySignatureVerifier.verifySignature("", paymentId, validSignature, testKeySecret))
        assertFalse(RazorpaySignatureVerifier.verifySignature(orderId, "", validSignature, testKeySecret))
        assertFalse(RazorpaySignatureVerifier.verifySignature(orderId, paymentId, "", testKeySecret))
    }

    @Test
    fun testCreateOrder_authoritativePriceAndDatabaseRecord() = runBlocking {
        val userId = "user_reader_101"
        val bookId = "book_atomic_habits_01"

        val createOrderResult = razorpayBackendService.createOrder(
            RazorpayBackendService.CreateOrderRequest(
                bookId = bookId,
                userId = userId
            )
        )

        assertTrue(createOrderResult is Resource.Success)
        val orderData = (createOrderResult as Resource.Success).data

        // Price must be authoritatively 29900 paise (from discountPrice ₹299.00)
        assertEquals(29900L, orderData.amountPaise)
        assertEquals(299.0, orderData.amountMajor, 0.01)
        assertEquals("INR", orderData.currency)
        assertEquals(testKeyId, orderData.razorpayKeyId)

        // Check that pending order exists in database
        val orderInDb = database.orderDao().getOrderByIdDirect(orderData.orderId)
        assertNotNull(orderInDb)
        assertEquals(OrderStatus.PAYMENT_PENDING.name, orderInDb!!.status)
        assertEquals(29900L, orderInDb.totalMinor)
    }

    @Test
    fun testVerifyPayment_validSignatureUnlocksBookInLibrary() = runBlocking {
        val userId = "user_reader_101"
        val bookId = "book_atomic_habits_01"

        // 1. Create order
        val createResult = razorpayBackendService.createOrder(
            RazorpayBackendService.CreateOrderRequest(bookId = bookId, userId = userId)
        )
        val orderData = (createResult as Resource.Success).data
        val paymentId = "pay_rzp_test_success_99"

        // 2. Generate valid HMAC-SHA256 signature
        val validSignature = RazorpaySignatureVerifier.calculateHmacSha256(
            payload = "${orderData.razorpayOrderId}|$paymentId",
            secret = testKeySecret
        )

        // 3. Verify payment via Backend Service
        val verifyResult = razorpayBackendService.verifyPayment(
            RazorpayBackendService.VerifyPaymentRequest(
                orderId = orderData.orderId,
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderData.razorpayOrderId,
                razorpaySignature = validSignature,
                userId = userId
            )
        )

        assertTrue("Payment verification should succeed", verifyResult is Resource.Success)
        val verifyData = (verifyResult as Resource.Success).data
        assertTrue(verifyData.success)
        assertEquals("PAID", verifyData.status)
        assertTrue(verifyData.unlockedBookIds.contains(bookId))

        // 4. Verify that the order in database is marked as PAID
        val updatedOrder = database.orderDao().getOrderByIdDirect(orderData.orderId)
        assertNotNull(updatedOrder)
        assertEquals(OrderStatus.PAID.name, updatedOrder!!.status)

        // 5. Verify that the payment record in database is marked as CAPTURED
        val paymentRecord = database.paymentDao().getPaymentByOrderIdDirect(orderData.orderId)
        assertNotNull(paymentRecord)
        assertEquals(PaymentStatus.CAPTURED.name, paymentRecord!!.status)
        assertEquals(paymentId, paymentRecord.providerPaymentId)

        // 6. Verify that the book is unlocked in User's Library
        val libraryItem = database.libraryDao().getLibraryItemDirect(userId, bookId)
        assertNotNull("Book must be present in user library", libraryItem)
        assertEquals(bookId, libraryItem!!.bookId)
        assertEquals(userId, libraryItem.userId)

        // 7. Verify active entitlement exists
        val entitlement = database.entitlementDao().getEntitlementDirect(userId, bookId)
        assertNotNull("Active entitlement must exist", entitlement)
    }

    @Test
    fun testVerifyPayment_invalidSignatureRejectsAndDoesNotUnlockBook() = runBlocking {
        val userId = "user_reader_102"
        val bookId = "book_atomic_habits_01"

        // 1. Create order
        val createResult = razorpayBackendService.createOrder(
            RazorpayBackendService.CreateOrderRequest(bookId = bookId, userId = userId)
        )
        val orderData = (createResult as Resource.Success).data
        val paymentId = "pay_rzp_fake_attempt_11"

        // 2. Submit fraudulent/tampered signature
        val fakeSignature = "invalid_signature_attempt"

        // 3. Verify payment
        val verifyResult = razorpayBackendService.verifyPayment(
            RazorpayBackendService.VerifyPaymentRequest(
                orderId = orderData.orderId,
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderData.razorpayOrderId,
                razorpaySignature = fakeSignature,
                userId = userId
            )
        )

        assertTrue("Payment verification must return error for invalid signature", verifyResult is Resource.Error)

        // 4. Verify order in DB is FAILED
        val orderInDb = database.orderDao().getOrderByIdDirect(orderData.orderId)
        assertEquals(OrderStatus.FAILED.name, orderInDb!!.status)

        // 5. Verify book is NOT added to library
        val libraryItem = database.libraryDao().getLibraryItemDirect(userId, bookId)
        assertNull("Book must NOT be in library after verification failure", libraryItem)
    }

    @Test
    fun testCreateOrder_failsWhenUserAlreadyOwnsBook() = runBlocking {
        val userId = "user_reader_103"
        val bookId = "book_atomic_habits_01"

        // Put book in library manually
        database.libraryDao().insertLibraryItem(
            LibraryEntity(
                id = "lib_existing_1",
                userId = userId,
                bookId = bookId,
                readingProgress = 0.5f,
                lastReadPage = 100,
                status = "READING",
                isDownloaded = false,
                purchasedAt = System.currentTimeMillis()
            )
        )

        // Attempting to buy again
        val createResult = razorpayBackendService.createOrder(
            RazorpayBackendService.CreateOrderRequest(bookId = bookId, userId = userId)
        )

        assertTrue("Should return error because user already owns the book", createResult is Resource.Error)
        assertTrue((createResult as Resource.Error).message.contains("already own", ignoreCase = true))
    }
}
