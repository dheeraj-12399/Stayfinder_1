package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Property
import com.example.ui.components.PropertyCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateDarker
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

data class CityCoordinate(
    val name: String,
    val lat: Double,
    val lng: Double
)

val CityCoordinates = listOf(
    CityCoordinate("All Cities", 17.3850, 78.4867),
    CityCoordinate("Hyderabad", 17.3850, 78.4867),
    CityCoordinate("Bangalore", 12.9716, 77.5946),
    CityCoordinate("Pune", 18.5204, 73.8567),
    CityCoordinate("Delhi NCR", 28.6139, 77.2090),
    CityCoordinate("Chennai", 13.0827, 80.2707),
    CityCoordinate("Mumbai", 19.0760, 72.8777),
    CityCoordinate("Visakhapatnam", 17.6868, 83.2185)
)

@Composable
fun MapScreen(
    properties: List<Property>,
    favorites: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onPropertyClick: (Property) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCity by remember { mutableStateOf("All Cities") }
    var selectedProperty by remember { mutableStateOf<Property?>(null) }

    // Properties filtered by city
    val cityProperties = remember(properties, selectedCity) {
        if (selectedCity == "All Cities") properties else properties.filter {
            it.city.equals(selectedCity, ignoreCase = true)
        }
    }

    // Auto select first property if available
    remember(cityProperties) {
        if (selectedProperty == null && cityProperties.isNotEmpty()) {
            selectedProperty = cityProperties.firstOrNull()
        }
        null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
    ) {
        // RADAR / MAP VISUAL CANVAS
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Detect tap on map area to clear selection or cycle
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height * 0.42f

            // Map grid lines (Modern dark theme radar grid)
            val gridColor = Color(0xFF1E293B)
            val step = 60.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < height) {
                drawLine(gridColor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                y += step
            }

            // Radar circular rings
            drawCircle(
                color = EmeraldPrimary.copy(alpha = 0.08f),
                radius = width * 0.4f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = EmeraldPrimary.copy(alpha = 0.15f),
                radius = width * 0.28f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = EmeraldPrimary.copy(alpha = 0.25f),
                radius = width * 0.15f,
                center = Offset(centerX, centerY)
            )

            // Center location pulse
            drawCircle(
                color = EmeraldPrimary,
                radius = 8.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        // TOP CONTROLS (City selector & header)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(SlateDark, SlateDark.copy(alpha = 0.8f), Color.Transparent))
                )
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Map,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Map & Locality Radar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "${cityProperties.size} Stays",
                        color = EmeraldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // City chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(CityCoordinates) { city ->
                    val isSelected = city.name == selectedCity
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else SlateCard,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) EmeraldPrimary else SlateBorder
                            )
                        ),
                        modifier = Modifier.clickable {
                            selectedCity = city.name
                            selectedProperty = properties.firstOrNull {
                                city.name == "All Cities" || it.city.equals(city.name, ignoreCase = true)
                            }
                        }
                    ) {
                        Text(
                            text = city.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else SlateTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // PROPERTY PIN BADGES SCATTERED OVER MAP
        if (cityProperties.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                cityProperties.take(8).forEachIndexed { index, prop ->
                    // Position offsets deterministic based on index / hash
                    val offsetXRatio = when (index % 4) {
                        0 -> 0.22f
                        1 -> 0.72f
                        2 -> 0.38f
                        else -> 0.68f
                    }
                    val offsetYRatio = when (index % 4) {
                        0 -> 0.26f
                        1 -> 0.32f
                        2 -> 0.50f
                        else -> 0.44f
                    } + (index / 4) * 0.12f

                    val isSelected = selectedProperty?.id == prop.id

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 200.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) EmeraldPrimary else SlateCard,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) Color.White else SlateBorder
                                )
                            ),
                            shadowElevation = if (isSelected) 8.dp else 4.dp,
                            modifier = Modifier
                                .align(
                                    androidx.compose.ui.BiasAlignment(
                                        horizontalBias = (offsetXRatio * 2) - 1f,
                                        verticalBias = (offsetYRatio * 2) - 1f
                                    )
                                )
                                .clickable { selectedProperty = prop }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "₹${prop.monthlyRent}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else SlateTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty notification
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = SlateTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No properties in this location yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select another city or add properties in Firebase Console",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                }
            }
        }

        // BOTTOM PREVIEW CARD
        selectedProperty?.let { prop ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 85.dp)
            ) {
                PropertyCard(
                    property = prop,
                    isFavorite = favorites.contains(prop.id),
                    onFavoriteToggle = { onFavoriteToggle(prop.id) },
                    onClick = { onPropertyClick(prop) }
                )
            }
        }
    }
}
