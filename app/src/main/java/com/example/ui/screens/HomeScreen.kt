package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Property
import com.example.ui.components.PropertyCard
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

val SupportedCities = listOf(
    "All Cities",
    "Hyderabad",
    "Bangalore",
    "Pune",
    "Delhi NCR",
    "Chennai",
    "Mumbai",
    "Visakhapatnam"
)

val SupportedTypes = listOf(
    "All",
    "Boys PG",
    "Girls PG",
    "Co-Living PG",
    "Single Room",
    "1BHK",
    "2BHK"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    properties: List<Property>,
    favorites: Set<String>,
    isLoading: Boolean,
    errorMessage: String?,
    onFavoriteToggle: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    onSearchClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onAiInspectorClick: () -> Unit,
    onAiLeaseClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCity by remember { mutableStateOf("All Cities") }
    var selectedType by remember { mutableStateOf("All") }

    val filteredProperties = remember(properties, selectedCity, selectedType) {
        properties.filter { p ->
            val cityMatch = selectedCity == "All Cities" || p.city.equals(selectedCity, ignoreCase = true)
            val typeMatch = selectedType == "All" || p.subType.equals(selectedType, ignoreCase = true)
            cityMatch && typeMatch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // TOP HEADER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SlateCard, SlateDarker))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StayFinder",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlateTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "VERIFIED",
                                    color = EmeraldLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Verified student housing & PGs",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }

                    // AI Assistant Quick Access Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = EmeraldPrimary,
                        modifier = Modifier
                            .clickable(onClick = onAiAssistantClick)
                            .testTag("btn_header_ai")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Search",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar Bar Tap
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSearchClick)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search city, area, college, locality…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateTextMuted
                        )
                    }
                }
            }
        }

        // CITIES ROW
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Popular Locations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SupportedCities) { city ->
                        val isSelected = city == selectedCity
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else SlateCard,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) EmeraldPrimary else SlateBorder
                                )
                            ),
                            modifier = Modifier.clickable { selectedCity = city }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (city != "All Cities") {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else SlateTextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = city,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SlateTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // STUDENT HUB CARDS (AI Room Inspector & Lease Analyzer)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Student Hub & AI Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: AI Room Inspector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAiInspectorClick)
                            .testTag("card_ai_inspector"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoCamera,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Room Inspector",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Scan room photo for ventilation & condition",
                                fontSize = 10.sp,
                                color = SlateTextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Card 2: AI Lease Analyzer
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAiLeaseClick)
                            .testTag("card_ai_lease"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentSky.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = AccentSky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lease Analyzer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = "Check curfew, hidden deposit deductions",
                                fontSize = 10.sp,
                                color = SlateTextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // QUICK FILTER CHIPS
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(SupportedTypes) { type ->
                    val isSelected = type == selectedType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateCard,
                            selectedContainerColor = EmeraldPrimary,
                            labelColor = SlateTextSecondary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = SlateBorder,
                            selectedBorderColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // PROPERTIES LIST HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCity == "All Cities") "Available Properties" else "Properties in $selectedCity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )

                if (properties.isNotEmpty()) {
                    Text(
                        text = "${filteredProperties.size} listed",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight
                    )
                }
            }
        }

        // ERROR STATE
        if (errorMessage != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to connect.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please check your internet connection and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        } else if (isLoading && properties.isEmpty()) {
            // LOADING STATE
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            }
        } else if (properties.isEmpty()) {
            // STRICT REQUIREMENT #5 & #44:
            // "If Firestore contains zero properties, display: No properties available yet. Do not invent properties."
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HomeWork,
                            contentDescription = null,
                            tint = SlateTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No properties available yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When listings are added in your Firebase Console 'properties' collection, they will appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (filteredProperties.isEmpty()) {
            // Filter produced 0 results
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching StayFinder properties were found for this filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // REAL PROPERTIES LIST
            items(filteredProperties, key = { it.id }) { property ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    PropertyCard(
                        property = property,
                        isFavorite = favorites.contains(property.id),
                        onFavoriteToggle = { onFavoriteToggle(property.id) },
                        onClick = { onPropertyClick(property) }
                    )
                }
            }
        }
    }
}
