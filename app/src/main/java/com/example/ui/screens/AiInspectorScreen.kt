package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiService
import com.example.ai.LeaseAnalysisReport
import com.example.ai.RoomInspectionReport
import com.example.data.model.Property
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentSky
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateDarker
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import kotlinx.coroutines.launch

data class ChatItem(
    val sender: String, // "user" or "ai"
    val text: String
)

@Composable
fun AiInspectorScreen(
    initialTab: Int = 0,
    realFirestoreProperties: List<Property>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiService = remember { AiService.getInstance() }

    var currentTab by remember { mutableIntStateOf(initialTab) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("ai_screen_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SlateTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "StayFinder AI Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldContainer
                    ) {
                        Text(
                            text = "Gemini",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Automated room inspections, lease analysis & concierge",
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }
        }

        // TABS
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = SlateCard,
            contentColor = EmeraldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                    color = EmeraldPrimary
                )
            }
        ) {
            Tab(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                text = { Text("Room Inspector", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                text = { Text("Lease Analyzer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                text = { Text("AI Concierge", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
        }

        when (currentTab) {
            0 -> RoomInspectorTab(aiService = aiService)
            1 -> LeaseAnalyzerTab(aiService = aiService)
            2 -> AiConciergeTab(aiService = aiService, properties = realFirestoreProperties)
        }
    }
}

// -------------------------------------------------------------
// TAB 1: AI ROOM INSPECTOR
// -------------------------------------------------------------
@Composable
private fun RoomInspectorTab(aiService: AiService) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var inspectionReport by remember { mutableStateOf<RoomInspectionReport?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Google Play Policy compliant photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bitmap
                inspectionReport = null
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load selected image"
            }
        }
    }

    fun runInspection() {
        val bmp = selectedBitmap ?: return
        isAnalyzing = true
        errorMessage = null

        scope.launch {
            val result = aiService.inspectRoomImage(bmp)
            isAnalyzing = false
            if (result.isSuccess) {
                inspectionReport = result.getOrNull()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "AI inspection failed."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "AI Room & Property Inspector",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
        Text(
            text = "Upload a room photo to evaluate ventilation, natural lighting, cleanliness and visible conditions before booking.",
            style = MaterialTheme.typography.bodySmall,
            color = SlateTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Photo Upload Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(SlateCard)
                .border(
                    width = 1.dp,
                    color = if (selectedBitmap != null) EmeraldPrimary else SlateBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (selectedBitmap != null) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected Room",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Upload photo",
                        tint = EmeraldLight,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to choose a room photo",
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "JPEG, PNG supported (Google Photo Picker)",
                        fontSize = 11.sp,
                        color = SlateTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { runInspection() },
            enabled = selectedBitmap != null && !isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Image Pixels...", color = Color.White)
            } else {
                Text("Inspect Room Conditions", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage!!, color = AccentRose, fontSize = 12.sp)
        }

        // INSPECTION REPORT DISPLAY
        inspectionReport?.let { report ->
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Visual Condition Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "Score: ${report.overallScore}/10",
                                color = EmeraldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    InspectionMetricRow("Cleanliness", report.cleanliness)
                    InspectionMetricRow("Lighting", report.lighting)
                    InspectionMetricRow("Ventilation", report.ventilation)
                    InspectionMetricRow("Furniture", report.furnitureCondition)
                    InspectionMetricRow("Visible Damage", report.visibleDamage)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Observations", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateTextPrimary)
                    Text(report.observations, fontSize = 12.sp, color = SlateTextSecondary, lineHeight = 16.sp)

                    if (report.concerns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Points to verify in person:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentAmber)
                        report.concerns.forEach { c ->
                            Text("• $c", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MANDATORY DISCLAIMER
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = SlateTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = report.disclaimer,
                                fontSize = 10.sp,
                                color = SlateTextMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectionMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = SlateTextMuted)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateTextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

// -------------------------------------------------------------
// TAB 2: AI LEASE ANALYZER
// -------------------------------------------------------------
@Composable
private fun LeaseAnalyzerTab(aiService: AiService) {
    val scope = rememberCoroutineScope()
    var leaseText by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<LeaseAnalysisReport?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sampleLease = """
        Rental Tenancy Agreement:
        1. Monthly rent shall be ₹9,500 payable by the 5th of each month.
        2. A refundable security deposit of ₹25,000 is deposited. Deductions will apply for wall repainting.
        3. Lock-in period is 6 months. Minimum 30 days written notice required prior to vacating.
        4. Gate closes strictly at 10:00 PM. No outside guests allowed post 8:00 PM.
        5. Electricity and maintenance charged extra at ₹10 per unit.
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "AI Lease Agreement Analyzer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
        Text(
            text = "Paste your rental agreement clauses below to detect hidden deductions, curfew rules, deposit refund conditions, and unusual lock-in terms.",
            style = MaterialTheme.typography.bodySmall,
            color = SlateTextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Insert Sample Snippet",
                color = EmeraldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { leaseText = sampleLease }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = leaseText,
            onValueChange = { leaseText = it },
            label = { Text("Paste agreement text / clauses...") },
            minLines = 6,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = SlateTextPrimary,
                unfocusedTextColor = SlateTextPrimary,
                focusedContainerColor = SlateDark,
                unfocusedContainerColor = SlateDark
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (leaseText.isNotBlank()) {
                    isAnalyzing = true
                    errorMessage = null
                    scope.launch {
                        val res = aiService.analyzeLeaseText(leaseText)
                        isAnalyzing = false
                        if (res.isSuccess) {
                            report = res.getOrNull()
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Analysis failed."
                        }
                    }
                }
            },
            enabled = leaseText.isNotBlank() && !isAnalyzing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Clauses...", color = Color.White)
            } else {
                Text("Analyze Tenancy Clauses", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage!!, color = AccentRose, fontSize = 12.sp)
        }

        report?.let { rep ->
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Executive Tenancy Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rep.summary, fontSize = 12.sp, color = SlateTextSecondary, lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    InspectionMetricRow("Rent & Due Date", rep.rent)
                    InspectionMetricRow("Security Deposit", rep.securityDeposit)
                    InspectionMetricRow("Notice Period", rep.noticePeriod)
                    InspectionMetricRow("Lock-In Period", rep.lockInPeriod)
                    InspectionMetricRow("Utilities & Maint.", rep.maintenanceAndHiddenCharges)
                    InspectionMetricRow("Curfew & Restrictions", rep.restrictionsAndCurfew)
                    InspectionMetricRow("Refund Conditions", rep.refundConditions)

                    if (rep.cautionaryTerms.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cautionary / Unusual Clauses:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentRose)
                        rep.cautionaryTerms.forEach { item ->
                            Text("⚠️ $item", fontSize = 11.sp, color = SlateTextPrimary, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // MANDATORY LEGAL DISCLAIMER
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = rep.disclaimer, fontSize = 10.sp, color = SlateTextMuted, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: AI ACCOMMODATION CONCIERGE (Strictly Real Properties)
// -------------------------------------------------------------
@Composable
private fun AiConciergeTab(
    aiService: AiService,
    properties: List<Property>
) {
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatItem(
                    sender = "ai",
                    text = "Hello! I am your StayFinder AI Concierge. Ask me about available verified PGs, budget accommodation, or specific amenities. I only answer using real properties from your database."
                )
            )
        )
    }
    var isThinking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Chat messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isUser) 14.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) EmeraldPrimary else SlateCard
                        ),
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isUser) Color.White else SlateTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = EmeraldLight, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Searching StayFinder database...", fontSize = 11.sp, color = SlateTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Bottom input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask e.g. Boys PG under ₹8,000 in Hyderabad", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_concierge_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = SlateTextPrimary,
                    unfocusedTextColor = SlateTextPrimary,
                    focusedContainerColor = SlateDark,
                    unfocusedContainerColor = SlateDark
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val query = inputQuery.trim()
                    if (query.isNotBlank() && !isThinking) {
                        inputQuery = ""
                        chatMessages = chatMessages + ChatItem("user", query)
                        isThinking = true

                        scope.launch {
                            val answer = aiService.askAccommodationAssistant(query, properties)
                            isThinking = false
                            chatMessages = chatMessages + ChatItem("ai", answer)
                        }
                    }
                },
                enabled = inputQuery.isNotBlank() && !isThinking,
                modifier = Modifier
                    .background(EmeraldPrimary, CircleShape)
                    .size(42.dp)
                    .testTag("btn_send_ai_query")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
