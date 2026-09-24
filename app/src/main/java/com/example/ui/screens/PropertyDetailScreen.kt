package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Booking
import com.example.data.model.Property
import com.example.data.model.Review
import com.example.ui.components.AmenitiesGrid
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    property: Property,
    reviews: List<Review>,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onBackClick: () -> Unit,
    onBookProperty: (Booking) -> Unit,
    onAddReview: (Review) -> Unit,
    onOpenChat: (ownerId: String, ownerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBookingSheet by remember { mutableStateOf(false) }
    var showReviewSheet by remember { mutableStateOf(false) }

    val images = remember(property) {
        val list = mutableListOf<String>()
        list.addAll(property.images)
        list.addAll(property.propertyImages.values)
        list.filter { it.isNotBlank() }.distinct()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
        ) {
            // IMAGE GALLERY / HERO
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 11f)
                        .background(SlateDark)
                ) {
                    if (images.isNotEmpty()) {
                        AsyncImage(
                            model = images.first(),
                            contentDescription = property.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Apartment,
                                contentDescription = null,
                                tint = SlateTextMuted,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    // Top Action Bar Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(SlateDark.copy(alpha = 0.8f), CircleShape)
                                .testTag("btn_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .background(SlateDark.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) AccentRose else Color.White
                            )
                        }
                    }
                }
            }

            // MAIN INFO
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = property.subType,
                                color = EmeraldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        if (property.landlordIsVerified) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified Landlord",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verified StayFinder",
                                    color = EmeraldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = property.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Address
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = listOfNotNull(property.address.ifBlank { null }, property.area.ifBlank { null }, property.city.ifBlank { null })
                                .joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rent and Deposit Highlight Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Monthly Rent", fontSize = 11.sp, color = SlateTextMuted)
                                Text(
                                    text = "₹${property.monthlyRent}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text("per bed / month", fontSize = 10.sp, color = SlateTextSecondary)
                            }
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(SlateBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Security Deposit", fontSize = 11.sp, color = SlateTextMuted)
                                Text(
                                    text = "₹${property.securityDeposit}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text("100% refundable", fontSize = 10.sp, color = SlateTextSecondary)
                            }
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(SlateBorder))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Vacancies", fontSize = 11.sp, color = SlateTextMuted)
                                Text(
                                    text = "${property.vacancies} Beds",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (property.vacancies > 0) EmeraldLight else AccentAmber
                                )
                                Text("Available now", fontSize = 10.sp, color = SlateTextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description
                    if (property.description.isNotBlank()) {
                        Text(
                            text = "About this Stay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = property.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextSecondary,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // AMENITIES
                    Text(
                        text = "Amenities & Facilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AmenitiesGrid(amenities = property.amenities)

                    Spacer(modifier = Modifier.height(20.dp))

                    // RULES & RESTRICTIONS
                    if (property.rules.isNotEmpty()) {
                        Text(
                            text = "House Rules & Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        property.rules.forEach { rule ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = rule,
                                    fontSize = 13.sp,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // LANDLORD CONTACT CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = EmeraldLight
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = property.ownerName.ifBlank { "Verified Host" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SlateTextPrimary
                                        )
                                        Text(
                                            text = "Property Landlord",
                                            fontSize = 11.sp,
                                            color = SlateTextMuted
                                        )
                                    }
                                }

                                // In-app Chat Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldPrimary,
                                    modifier = Modifier.clickable {
                                        onOpenChat(property.ownerId, property.ownerName)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Chat",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Direct WhatsApp or Phone if available
                            val contactNum = property.ownerWhatsapp.ifBlank { property.ownerPhone }
                            if (contactNum.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        val cleanNum = contactNum.replace(" ", "").replace("+", "")
                                        val url = "https://wa.me/$cleanNum?text=Hi, I am inquiring about ${Uri.encode(property.name)} on StayFinder."
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
                                ) {
                                    Text("WhatsApp Landlord Directly", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // REVIEWS SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tenant Reviews (${reviews.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "+ Write Review",
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { showReviewSheet = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (reviews.isEmpty()) {
                        Text(
                            text = "No reviews yet for this property. Be the first tenant to leave a review!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted
                        )
                    } else {
                        reviews.forEach { review ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = review.userName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = SlateTextPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = AccentAmber,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${review.rating}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = review.comment,
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM STICKY BOOKING ACTION BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = SlateCard,
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Monthly Rent",
                        fontSize = 11.sp,
                        color = SlateTextMuted
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${property.monthlyRent}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "/mo",
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = { showBookingSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("btn_book_now")
                ) {
                    Text(
                        text = "Book Bed Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // BOOKING BOTTOM SHEET
        if (showBookingSheet) {
            var selectedSharing by remember {
                mutableStateOf(property.sharingOptions.keys.firstOrNull() ?: "singleSharing")
            }
            var checkInDate by remember { mutableStateOf("Immediate") }
            var durationMonths by remember { mutableIntStateOf(3) }

            ModalBottomSheet(
                onDismissRequest = { showBookingSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SlateCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Confirm Booking Reservation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Sharing Type", fontSize = 12.sp, color = SlateTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sharingKeys = if (property.sharingOptions.isNotEmpty()) property.sharingOptions.keys.toList() else listOf("singleSharing", "doubleSharing")
                        sharingKeys.forEach { share ->
                            val label = when (share) {
                                "singleSharing" -> "Single Sharing"
                                "doubleSharing" -> "Double Sharing"
                                "tripleSharing" -> "3 Sharing"
                                else -> share
                            }
                            FilterChip(
                                selected = selectedSharing == share,
                                onClick = { selectedSharing = share },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    containerColor = SlateDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price Breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monthly Rent:", fontSize = 12.sp, color = SlateTextSecondary)
                                Text("₹${property.monthlyRent}", fontSize = 12.sp, color = SlateTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Security Deposit:", fontSize = 12.sp, color = SlateTextSecondary)
                                Text("₹${property.securityDeposit}", fontSize = 12.sp, color = SlateTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Initial Due:", fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                Text(
                                    "₹${property.monthlyRent + property.securityDeposit}",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val newBooking = Booking(
                                propertyId = property.id,
                                propertyName = property.name,
                                propertyAddress = property.address,
                                ownerId = property.ownerId,
                                roomType = property.subType,
                                sharingType = selectedSharing,
                                checkInDate = checkInDate,
                                durationMonths = durationMonths,
                                monthlyRent = property.monthlyRent,
                                depositAmount = property.securityDeposit,
                                totalAmount = property.monthlyRent + property.securityDeposit,
                                paymentStatus = "pay_on_arrival",
                                status = "confirmed"
                            )
                            onBookProperty(newBooking)
                            showBookingSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Confirm & Reserve Bed", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // ADD REVIEW BOTTOM SHEET
        if (showReviewSheet) {
            var rating by remember { mutableDoubleStateOf(5.0) }
            var comment by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { showReviewSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SlateCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Write a Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { rating = i.toDouble() }) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "$i Stars",
                                    tint = if (i <= rating) AccentAmber else SlateBorder,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your Experience (Cleanliness, Food, Wi-Fi...)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (comment.isNotBlank()) {
                                val rev = Review(
                                    propertyId = property.id,
                                    rating = rating,
                                    comment = comment
                                )
                                onAddReview(rev)
                                showReviewSheet = false
                            }
                        },
                        enabled = comment.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Submit Review", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
