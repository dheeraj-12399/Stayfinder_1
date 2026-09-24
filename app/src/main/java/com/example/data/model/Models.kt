package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val gender: String = "",
    val collegeName: String = "",
    val role: String = "tenant",
    val isVerifiedOwner: Boolean = false,
    val isApproved: Boolean = true,
    val isBlocked: Boolean = false,
    val profileImage: String = "",
    val aadhaar: String = ""
)

@JsonClass(generateAdapter = true)
data class Property(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "PG", // PG, Room, Flat
    val subType: String = "Boys PG", // Boys PG, Girls PG, Co-Living PG, Single Room, 1BHK, 2BHK, 3BHK
    val address: String = "",
    val area: String = "",
    val locality: String = "",
    val city: String = "",
    val landmark: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val monthlyRent: Long = 0L,
    val annualFee: Long = 0L,
    val securityDeposit: Long = 0L,
    val vacancies: Int = 0,
    val availableBeds: Int = 0,
    val totalBeds: Int = 0,
    val furnishedStatus: String = "Furnished", // Furnished, Semi-Furnished, Unfurnished
    val rating: Double = 0.0,
    val availableFrom: String = "",
    val rules: List<String> = emptyList(),
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerWhatsapp: String = "",
    val ownerEmail: String = "",
    val landlordIsVerified: Boolean = false,
    val images: List<String> = emptyList(),
    val propertyImages: Map<String, String> = emptyMap(),
    val amenities: Map<String, Boolean> = emptyMap(),
    val sharingOptions: Map<String, Boolean> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class Booking(
    val id: String = "",
    val propertyId: String = "",
    val propertyName: String = "",
    val propertyAddress: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val ownerId: String = "",
    val roomType: String = "",
    val sharingType: String = "",
    val checkInDate: String = "",
    val durationMonths: Int = 1,
    val monthlyRent: Long = 0L,
    val depositAmount: Long = 0L,
    val totalAmount: Long = 0L,
    val paymentStatus: String = "pending", // pending, paid, pay_on_arrival
    val status: String = "pending", // pending, confirmed, cancelled, completed
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Review(
    val id: String = "",
    val propertyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "info",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class FilterCriteria(
    val selectedCity: String = "",
    val searchQuery: String = "",
    val selectedSubtypes: Set<String> = emptySet(),
    val maxRent: Long = 50000L,
    val selectedAmenities: Set<String> = emptySet(),
    val selectedSharing: Set<String> = emptySet()
)
