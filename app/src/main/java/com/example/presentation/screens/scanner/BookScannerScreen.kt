package com.example.presentation.screens.scanner

import android.Manifest
import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.Book
import com.example.presentation.scanner.BarcodeParser
import com.example.presentation.scanner.QrBarcodeAnalyzer
import com.example.presentation.viewmodel.scanner.BookScannerViewModel
import com.example.presentation.viewmodel.scanner.ScannerUiState
import com.example.ui.theme.PolishAccentOrange
import com.example.ui.theme.PolishBackground
import com.example.ui.theme.PolishPrimaryDark
import com.example.ui.theme.PolishPrimaryIndigo
import com.example.ui.theme.PolishSlate100
import com.example.ui.theme.PolishSlate400
import com.example.ui.theme.PolishSlate700
import com.example.ui.theme.PolishSlate900
import com.example.ui.theme.PolishSuccess
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookScannerScreen(
    viewModel: BookScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBookDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("book_scanner_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreviewContent(
                    uiState = uiState,
                    onBarcodeDetected = { raw, format ->
                        viewModel.onBarcodeDetected(raw, format)
                    }
                )
            } else {
                CameraPermissionFallback(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onManualInput = { viewModel.showManualInputDialog(true) }
                )
            }

            // Top Overlay Action Bar
            ScannerTopBar(
                isTorchOn = uiState.isTorchOn,
                autoAddToWishlist = uiState.autoAddToWishlist,
                sessionScansCount = uiState.sessionScannedBooks.size,
                onNavigateBack = onNavigateBack,
                onToggleTorch = { viewModel.toggleTorch() },
                onToggleAutoAdd = { viewModel.toggleAutoAdd(!uiState.autoAddToWishlist) },
                onOpenManualInput = { viewModel.showManualInputDialog(true) },
                onOpenHistory = { viewModel.showHistorySheet(true) }
            )

            // Simulation / Quick Barcode Chips (Great for testing in emulator / preview)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .fillMaxWidth()
            ) {
                SimulationBarcodeChips(
                    onSimulateScan = { sampleCode ->
                        viewModel.simulateSampleScan(sampleCode)
                    }
                )
            }

            // Bottom Scanned Result Card
            AnimatedVisibility(
                visible = uiState.currentScannedBook != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                uiState.currentScannedBook?.let { book ->
                    ScannedBookResultCard(
                        book = book,
                        barcodeFormat = uiState.currentBarcodeFormat ?: "EAN-13 (ISBN)",
                        isInWishlist = uiState.isBookInWishlist,
                        isProcessing = uiState.isProcessing,
                        onAddToWishlist = { viewModel.addCurrentBookToWishlist() },
                        onViewDetails = { onNavigateToBookDetails(book.id) },
                        onDismiss = { viewModel.dismissCurrentScan() }
                    )
                }
            }
        }
    }

    // Manual ISBN Input Dialog
    if (uiState.showManualInputDialog) {
        ManualIsbnInputDialog(
            onDismiss = { viewModel.showManualInputDialog(false) },
            onSubmit = { isbn -> viewModel.searchOrAddManualIsbn(isbn) }
        )
    }

    // Scan History Bottom Sheet
    if (uiState.showHistorySheet) {
        ScanHistoryBottomSheet(
            scannedBooks = uiState.sessionScannedBooks,
            onDismiss = { viewModel.showHistorySheet(false) },
            onAddAllToWishlist = { viewModel.addAllSessionBooksToWishlist() },
            onSelectBook = { bookId ->
                viewModel.showHistorySheet(false)
                onNavigateToBookDetails(bookId)
            },
            onClearHistory = { viewModel.clearSessionHistory() }
        )
    }
}

@Composable
private fun CameraPreviewContent(
    uiState: ScannerUiState,
    onBarcodeDetected: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }

    val analyzer = remember {
        QrBarcodeAnalyzer { rawValue, format, _ ->
            onBarcodeDetected(rawValue, format)
        }
    }

    LaunchedEffect(uiState.isTorchOn) {
        cameraControl?.cameraControl?.enableTorch(uiState.isTorchOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, analyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera
                        camera.cameraControl.enableTorch(uiState.isTorchOn)
                    } catch (exc: Exception) {
                        // Camera binding fallback
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Viewfinder reticle overlay
        ScanningReticleOverlay()
    }
}

@Composable
private fun ScanningReticleOverlay(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "laser_transition")
    val animatedProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Frame dimensions: standard book barcode rectangle in center
        val frameWidth = (canvasWidth * 0.82f).coerceAtMost(340.dp.toPx())
        val frameHeight = 220.dp.toPx()
        val left = (canvasWidth - frameWidth) / 2f
        val top = (canvasHeight - frameHeight) / 2f - 40.dp.toPx()

        // 1. Semi-transparent backdrop outside scanning window
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            size = size
        )

        // 2. Clear out the viewfinder window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 3. Viewfinder border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // 4. Accent Corner Brackets (Stylized aiming brackets)
        val bracketLength = 28.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val cornerColor = PolishAccentOrange

        // Top-Left
        drawLine(cornerColor, Offset(left, top), Offset(left + bracketLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + bracketLength), strokeWidth)

        // Top-Right
        drawLine(cornerColor, Offset(left + frameWidth, top), Offset(left + frameWidth - bracketLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left + frameWidth, top), Offset(left + frameWidth, top + bracketLength), strokeWidth)

        // Bottom-Left
        drawLine(cornerColor, Offset(left, top + frameHeight), Offset(left + bracketLength, top + frameHeight), strokeWidth)
        drawLine(cornerColor, Offset(left, top + frameHeight), Offset(left, top + frameHeight - bracketLength), strokeWidth)

        // Bottom-Right
        drawLine(cornerColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth - bracketLength, top + frameHeight), strokeWidth)
        drawLine(cornerColor, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth, top + frameHeight - bracketLength), strokeWidth)

        // 5. Animated laser scanning beam
        val laserY = top + (frameHeight * animatedProgress)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    PolishPrimaryIndigo.copy(alpha = 0.05f),
                    PolishAccentOrange.copy(alpha = 0.85f),
                    PolishPrimaryIndigo.copy(alpha = 0.05f)
                ),
                startY = laserY - 12.dp.toPx(),
                endY = laserY + 12.dp.toPx()
            ),
            topLeft = Offset(left + 8.dp.toPx(), laserY - 6.dp.toPx()),
            size = Size(frameWidth - 16.dp.toPx(), 12.dp.toPx())
        )
    }

    // Instructional label under frame
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 180.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = PolishAccentOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Align book ISBN barcode or QR code inside box",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ScannerTopBar(
    isTorchOn: Boolean,
    autoAddToWishlist: Boolean,
    sessionScansCount: Int,
    onNavigateBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleAutoAdd: () -> Unit,
    onOpenManualInput: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Scan Physical Book",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "ISBN & QR Wishlist Scanner",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Torch toggle
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .background(
                            if (isTorchOn) PolishAccentOrange.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .testTag("scanner_torch_toggle")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) PolishAccentOrange else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Auto-Add toggle
                IconButton(
                    onClick = onToggleAutoAdd,
                    modifier = Modifier
                        .background(
                            if (autoAddToWishlist) PolishSuccess.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .testTag("scanner_auto_add_toggle")
                ) {
                    Icon(
                        imageVector = if (autoAddToWishlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Auto Add",
                        tint = if (autoAddToWishlist) PolishSuccess else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Manual input button
                IconButton(
                    onClick = onOpenManualInput,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .testTag("scanner_manual_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Manual ISBN",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // History button with count badge
                Box {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("scanner_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Scan History",
                            tint = Color.White
                        )
                    }
                    if (sessionScansCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = PolishAccentOrange,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = sessionScansCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationBarcodeChips(
    onSimulateScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val samples = listOf(
        "Clean Architecture" to "978-0134494166",
        "Atomic Habits" to "978-0735211292",
        "Frontier AI" to "978-1954123456",
        "Clean Code" to "978-0132350884",
        "Data-Intensive Apps" to "978-1449373320",
        "Pragmatic Programmer" to "978-0135957059",
        "Deep Work" to "978-1455586691",
        "Sapiens" to "978-0062316097"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PolishAccentOrange,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Quick Demo Barcodes (Tap to Simulate Physical Scan)",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            samples.forEach { (name, isbn) ->
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PolishPrimaryIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onSimulateScan(isbn) }
                        .testTag("simulate_chip_$isbn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = PolishAccentOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedBookResultCard(
    book: Book,
    barcodeFormat: String,
    isInWishlist: Boolean,
    isProcessing: Boolean,
    onAddToWishlist: () -> Unit,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("scanned_book_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Bar in Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PolishSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ Book Identified",
                            color = PolishSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = barcodeFormat,
                        color = PolishSlate400,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = PolishSlate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Book Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 68.dp, height = 96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PolishSlate100)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PolishSlate900
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${book.authorName}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PolishSlate700,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PolishSlate100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ISBN: ${book.isbn.ifBlank { "N/A" }}",
                                color = PolishSlate700,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹${book.discountPrice?.toInt() ?: book.price.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = PolishPrimaryIndigo,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isInWishlist) {
                    Button(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PolishSuccess
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("in_wishlist_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In Wishlist (View Book)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Button(
                        onClick = onAddToWishlist,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PolishPrimaryIndigo
                        ),
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_scanned_to_wishlist_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add to Wishlist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PolishSlate400),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "Details",
                        color = PolishSlate900,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onManualInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PolishBackground)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = PolishPrimaryIndigo.copy(alpha = 0.1f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = PolishPrimaryIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PolishSlate900,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bookora needs camera access to scan ISBN barcodes and QR codes from physical books to instantly add them to your digital wishlist.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PolishSlate700,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_camera_permission_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Camera Access",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onManualInput,
                    modifier = Modifier.testTag("manual_isbn_fallback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = PolishPrimaryIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Or Enter ISBN Manually",
                        color = PolishPrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualIsbnInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var isbnText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter ISBN / Barcode Number",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = PolishSlate900
            )
        },
        text = {
            Column {
                Text(
                    text = "Type the 10 or 13-digit ISBN printed on the back cover of the physical book (e.g. 978-0134494166).",
                    fontSize = 12.sp,
                    color = PolishSlate700
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = isbnText,
                    onValueChange = { isbnText = it },
                    placeholder = { Text("e.g. 9780134494166 or 978-0735211292") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_isbn_text_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isbnText.isNotBlank()) {
                        onSubmit(isbnText)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                modifier = Modifier.testTag("submit_manual_isbn_button")
            ) {
                Text("Search & Add", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PolishSlate700)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanHistoryBottomSheet(
    scannedBooks: List<Book>,
    onDismiss: () -> Unit,
    onAddAllToWishlist: () -> Unit,
    onSelectBook: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Session Scan History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PolishSlate900
                    )
                    Text(
                        text = "${scannedBooks.size} physical books scanned this session",
                        fontSize = 12.sp,
                        color = PolishSlate400
                    )
                }

                if (scannedBooks.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_scan_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear History",
                            tint = PolishSlate400
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (scannedBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No books scanned yet in this session.\nPoint your camera at a barcode to get started.",
                        color = PolishSlate400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(scannedBooks) { book ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PolishBackground,
                            border = BorderStroke(1.dp, PolishSlate100),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBook(book.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = book.coverUrl,
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp, 60.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        color = PolishSlate900
                                    )
                                    Text(
                                        text = book.authorName,
                                        fontSize = 12.sp,
                                        color = PolishSlate700
                                    )
                                    Text(
                                        text = "ISBN: ${book.isbn}",
                                        fontSize = 10.sp,
                                        color = PolishSlate400
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddAllToWishlist,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PolishPrimaryIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_all_scans_to_wishlist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add All ${scannedBooks.size} Books to Wishlist",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
