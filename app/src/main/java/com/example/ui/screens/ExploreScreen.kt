package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Property
import com.example.ui.components.AllSupportedAmenities
import com.example.ui.components.PropertyCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateDarker
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

val FilterSubtypes = listOf(
    "Boys PG",
    "Girls PG",
    "Co-Living PG",
    "Single Room",
    "1BHK",
    "2BHK",
    "3BHK"
)

val SharingOptionsList = listOf(
    "singleSharing" to "Single Sharing",
    "doubleSharing" to "Double Sharing",
    "tripleSharing" to "Triple Sharing"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    properties: List<Property>,
    favorites: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubtypes by remember { mutableStateOf(setOf<String>()) }
    var maxRent by remember { mutableFloatStateOf(50000f) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var selectedSharing by remember { mutableStateOf(setOf<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredProperties = remember(
        properties,
        searchQuery,
        selectedSubtypes,
        maxRent,
        selectedAmenities,
        selectedSharing
    ) {
        properties.filter { p ->
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.city.lowercase().contains(q) ||
                p.area.lowercase().contains(q) ||
                p.locality.lowercase().contains(q) ||
                p.landmark.lowercase().contains(q) ||
                p.address.lowercase().contains(q) ||
                p.subType.lowercase().contains(q)

            val matchesSubtype = selectedSubtypes.isEmpty() || selectedSubtypes.contains(p.subType)
            val matchesRent = p.monthlyRent <= maxRent.toLong()
            val matchesAmenities = selectedAmenities.all { p.amenities[it] == true }
            val matchesSharing = selectedSharing.isEmpty() || selectedSharing.any { p.sharingOptions[it] == true }

            matchesSearch && matchesSubtype && matchesRent && matchesAmenities && matchesSharing
        }
    }

    val activeFilterCount = (if (selectedSubtypes.isNotEmpty()) 1 else 0) +
        (if (maxRent < 50000f) 1 else 0) +
        selectedAmenities.size +
        selectedSharing.size

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
    ) {
        // TOP SEARCH & FILTER BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city, area, college, locality…", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = SlateTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary,
                        focusedContainerColor = SlateDark,
                        unfocusedContainerColor = SlateDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explore_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Filter Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (activeFilterCount > 0) EmeraldPrimary else SlateDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (activeFilterCount > 0) EmeraldPrimary else SlateBorder
                        )
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { showFilterSheet = true }
                        .testTag("btn_filter_sheet")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                tint = if (activeFilterCount > 0) Color.White else SlateTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Active filters pills row
            if (activeFilterCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeFilterCount filter${if (activeFilterCount > 1) "s" else ""} applied",
                        fontSize = 11.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Clear All",
                        fontSize = 11.sp,
                        color = SlateTextMuted,
                        modifier = Modifier.clickable {
                            selectedSubtypes = emptySet()
                            maxRent = 50000f
                            selectedAmenities = emptySet()
                            selectedSharing = emptySet()
                        }
                    )
                }
            }
        }

        // RESULTS LIST
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "${filteredProperties.size} found",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            if (filteredProperties.isEmpty()) {
                item {
                    // STRICT REQUIREMENT #14:
                    // "If nothing matches: No matching StayFinder properties were found."
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
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = SlateTextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching StayFinder properties were found.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try adjusting your search keyword, city, or filter parameters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
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

    // FILTER BOTTOM SHEET
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SlateCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Properties",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Reset",
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            selectedSubtypes = emptySet()
                            maxRent = 50000f
                            selectedAmenities = emptySet()
                            selectedSharing = emptySet()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Max Rent Slider
                Text(
                    text = "Max Monthly Rent: ₹${maxRent.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Slider(
                    value = maxRent,
                    onValueChange = { maxRent = it },
                    valueRange = 3000f..50000f,
                    steps = 47,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SlateBorder
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹3,000", fontSize = 11.sp, color = SlateTextMuted)
                    Text("₹20,000", fontSize = 11.sp, color = SlateTextMuted)
                    Text("₹50,000+", fontSize = 11.sp, color = SlateTextMuted)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Property SubTypes
                Text(
                    text = "Property Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterSubtypes.forEach { subtype ->
                        val isSelected = selectedSubtypes.contains(subtype)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSubtypes = if (isSelected) selectedSubtypes - subtype else selectedSubtypes + subtype
                            },
                            label = { Text(subtype, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateDark,
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

                Spacer(modifier = Modifier.height(18.dp))

                // Sharing Options
                Text(
                    text = "Occupancy / Sharing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                SharingOptionsList.forEach { (key, title) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSharing = if (selectedSharing.contains(key)) selectedSharing - key else selectedSharing + key
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSharing.contains(key),
                            onCheckedChange = { checked ->
                                selectedSharing = if (checked) selectedSharing + key else selectedSharing - key
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldPrimary,
                                uncheckedColor = SlateBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = title, color = SlateTextPrimary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amenities
                Text(
                    text = "Must-Have Amenities",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                AllSupportedAmenities.forEach { amenity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAmenities = if (selectedAmenities.contains(amenity.key)) {
                                    selectedAmenities - amenity.key
                                } else {
                                    selectedAmenities + amenity.key
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedAmenities.contains(amenity.key),
                            onCheckedChange = { checked ->
                                selectedAmenities = if (checked) {
                                    selectedAmenities + amenity.key
                                } else {
                                    selectedAmenities - amenity.key
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldPrimary,
                                uncheckedColor = SlateBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = amenity.icon,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = amenity.title, color = SlateTextPrimary, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(
                        text = "Apply Filters (${filteredProperties.size} Results)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
