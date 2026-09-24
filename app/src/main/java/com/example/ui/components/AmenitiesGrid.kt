package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

data class AmenityInfo(
    val key: String,
    val title: String,
    val icon: ImageVector
)

val AllSupportedAmenities = listOf(
    AmenityInfo("wifi", "High-Speed Wi-Fi", Icons.Outlined.Wifi),
    AmenityInfo("ac", "Air Conditioning", Icons.Outlined.AcUnit),
    AmenityInfo("food", "3 Meals / Food", Icons.Outlined.Restaurant),
    AmenityInfo("parking", "Vehicle Parking", Icons.Outlined.LocalParking),
    AmenityInfo("cctv", "24x7 CCTV Security", Icons.Outlined.Videocam),
    AmenityInfo("laundry", "Laundry Service", Icons.Outlined.LocalLaundryService),
    AmenityInfo("attachedBathroom", "Attached Washroom", Icons.Outlined.Bathtub),
    AmenityInfo("powerBackup", "Power Backup", Icons.Outlined.ElectricBolt),
    AmenityInfo("waterSupply", "24/7 Water Supply", Icons.Outlined.WaterDrop),
    AmenityInfo("washingMachine", "Washing Machine", Icons.Outlined.LocalLaundryService)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmenitiesGrid(
    amenities: Map<String, Boolean>,
    modifier: Modifier = Modifier
) {
    val activeAmenities = AllSupportedAmenities.filter { amenities[it.key] == true }

    if (activeAmenities.isEmpty()) {
        Text(
            text = "Basic living essentials included",
            style = MaterialTheme.typography.bodyMedium,
            color = SlateTextSecondary,
            modifier = modifier
        )
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activeAmenities.forEach { amenity ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateBorder))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = amenity.icon,
                        contentDescription = amenity.title,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amenity.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextPrimary
                    )
                }
            }
        }
    }
}
