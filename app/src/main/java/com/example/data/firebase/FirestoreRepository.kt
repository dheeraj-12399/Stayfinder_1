package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.AppNotification
import com.example.data.model.Booking
import com.example.data.model.Message
import com.example.data.model.Property
import com.example.data.model.Review
import com.example.data.model.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val context: Context) {

    private val firebaseManager = FirebaseManager.getInstance(context)
    private val firestore: FirebaseFirestore?
        get() = firebaseManager.getFirestore()

    companion object {
        private const val TAG = "StayFinderFirestore"
        const val COLLECTION_PROPERTIES = "properties"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_REVIEWS = "reviews"
        const val COLLECTION_MESSAGES = "messages"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val COLLECTION_FAVORITES = "favorites"
    }

    /**
     * Real-time listener for all properties in Firebase Firestore.
     * Changes made in Firebase Console (add, edit, delete, change rent/vacancy)
     * are received immediately without rebuilding APK.
     */
    fun getPropertiesFlow(): Flow<Result<List<Property>>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(Result.failure(Exception("Firebase Firestore is not initialized.")))
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_PROPERTIES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Properties snapshot listener error", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val properties = snapshot.documents.mapNotNull { doc ->
                        mapDocToProperty(doc)
                    }
                    Log.d(TAG, "Real-time properties updated from Firestore: ${properties.size} items")
                    trySend(Result.success(properties))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time listener for current user's profile.
     */
    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val db = firestore
        if (db == null || userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "User profile listener error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = User(
                        id = snapshot.getString("id") ?: userId,
                        name = snapshot.getString("name") ?: "",
                        email = snapshot.getString("email") ?: "",
                        phone = snapshot.getString("phone") ?: "",
                        whatsapp = snapshot.getString("whatsapp") ?: "",
                        gender = snapshot.getString("gender") ?: "",
                        collegeName = snapshot.getString("collegeName") ?: "",
                        role = snapshot.getString("role") ?: "tenant",
                        isVerifiedOwner = snapshot.getBoolean("isVerifiedOwner") ?: false,
                        isApproved = snapshot.getBoolean("isApproved") ?: true,
                        isBlocked = snapshot.getBoolean("isBlocked") ?: false,
                        profileImage = snapshot.getString("profileImage") ?: "",
                        aadhaar = snapshot.getString("aadhaar") ?: ""
                    )
                    trySend(user)
                } else {
                    trySend(null)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val updates = mapOf(
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "whatsapp" to user.whatsapp,
                "gender" to user.gender,
                "collegeName" to user.collegeName,
                "profileImage" to user.profileImage
            )
            db.collection(COLLECTION_USERS).document(user.id).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for tenant bookings.
     */
    fun getBookingsFlow(customerId: String): Flow<List<Booking>> = callbackFlow {
        val db = firestore
        if (db == null || customerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Bookings listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    mapDocToBooking(doc)
                } ?: emptyList()
                trySend(bookings)
            }

        awaitClose { registration.remove() }
    }

    suspend fun createBooking(booking: Booking): Result<String> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection(COLLECTION_BOOKINGS).document()
            val bookingWithId = booking.copy(id = docRef.id)
            docRef.set(bookingWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create booking", e)
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection(COLLECTION_BOOKINGS).document(bookingId)
                .update("status", "cancelled")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for property reviews.
     */
    fun getReviewsFlow(propertyId: String): Flow<List<Review>> = callbackFlow {
        val db = firestore
        if (db == null || propertyId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_REVIEWS)
            .whereEqualTo("propertyId", propertyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Reviews listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    Review(
                        id = doc.id,
                        propertyId = doc.getString("propertyId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "Anonymous",
                        rating = doc.getDouble("rating") ?: 5.0,
                        comment = doc.getString("comment") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(reviews)
            }

        awaitClose { registration.remove() }
    }

    suspend fun addReview(review: Review): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection(COLLECTION_REVIEWS).document()
            docRef.set(review.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time favorites set for the user.
     */
    fun getFavoritesFlow(userId: String): Flow<Set<String>> = callbackFlow {
        val db = firestore
        if (db == null || userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_FAVORITES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }

        awaitClose { registration.remove() }
    }

    suspend fun toggleFavorite(userId: String, propertyId: String, isFavorite: Boolean): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val favRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(propertyId)

            if (isFavorite) {
                favRef.delete().await()
            } else {
                favRef.set(mapOf("propertyId" to propertyId, "savedAt" to System.currentTimeMillis())).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time chat messages listener.
     */
    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val db = firestore
        if (db == null || chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_MESSAGES)
            .whereEqualTo("chatId", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    Message(
                        id = doc.id,
                        chatId = doc.getString("chatId") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                }?.sortedBy { it.timestamp } ?: emptyList()
                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(message: Message): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val docRef = db.collection(COLLECTION_MESSAGES).document()
            docRef.set(message.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time notifications for the user.
     */
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>> = callbackFlow {
        val db = firestore
        if (db == null || userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifs = snapshot?.documents?.mapNotNull { doc ->
                    AppNotification(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        type = doc.getString("type") ?: "info",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        isRead = doc.getBoolean("isRead") ?: false
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(notifs)
            }

        awaitClose { registration.remove() }
    }

    private fun mapDocToProperty(doc: DocumentSnapshot): Property? {
        return try {
            val imagesList = when (val rawImages = doc.get("images")) {
                is List<*> -> rawImages.filterIsInstance<String>()
                is String -> listOf(rawImages)
                else -> emptyList()
            }

            @Suppress("UNCHECKED_CAST")
            val propImagesMap = doc.get("propertyImages") as? Map<String, String> ?: emptyMap()

            val rulesList = when (val rawRules = doc.get("rules")) {
                is List<*> -> rawRules.filterIsInstance<String>()
                is String -> rawRules.split(",").map { it.trim() }
                else -> emptyList()
            }

            @Suppress("UNCHECKED_CAST")
            val amenitiesMap = doc.get("amenities") as? Map<String, Boolean> ?: run {
                // If amenities are stored as a List of strings
                val list = doc.get("amenities") as? List<*>
                list?.filterIsInstance<String>()?.associateWith { true } ?: emptyMap()
            }

            @Suppress("UNCHECKED_CAST")
            val sharingMap = doc.get("sharingOptions") as? Map<String, Boolean> ?: emptyMap()

            Property(
                id = doc.id,
                name = doc.getString("name") ?: "StayFinder Property",
                description = doc.getString("description") ?: "",
                type = doc.getString("type") ?: "PG",
                subType = doc.getString("subType") ?: "Boys PG",
                address = doc.getString("address") ?: "",
                area = doc.getString("area") ?: "",
                locality = doc.getString("locality") ?: "",
                city = doc.getString("city") ?: "",
                landmark = doc.getString("landmark") ?: "",
                latitude = doc.getDouble("latitude") ?: 0.0,
                longitude = doc.getDouble("longitude") ?: 0.0,
                monthlyRent = doc.getLong("monthlyRent") ?: 0L,
                annualFee = doc.getLong("annualFee") ?: 0L,
                securityDeposit = doc.getLong("securityDeposit") ?: 0L,
                vacancies = doc.getLong("vacancies")?.toInt() ?: 0,
                availableBeds = doc.getLong("availableBeds")?.toInt() ?: 0,
                totalBeds = doc.getLong("totalBeds")?.toInt() ?: 0,
                furnishedStatus = doc.getString("furnishedStatus") ?: "Furnished",
                rating = doc.getDouble("rating") ?: 0.0,
                availableFrom = doc.getString("availableFrom") ?: "Immediate",
                rules = rulesList,
                ownerId = doc.getString("ownerId") ?: "",
                ownerName = doc.getString("ownerName") ?: "Property Manager",
                ownerPhone = doc.getString("ownerPhone") ?: "",
                ownerWhatsapp = doc.getString("ownerWhatsapp") ?: "",
                ownerEmail = doc.getString("ownerEmail") ?: "",
                landlordIsVerified = doc.getBoolean("landlordIsVerified") ?: false,
                images = imagesList,
                propertyImages = propImagesMap,
                amenities = amenitiesMap,
                sharingOptions = sharingMap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to Property", e)
            null
        }
    }

    private fun mapDocToBooking(doc: DocumentSnapshot): Booking? {
        return try {
            Booking(
                id = doc.id,
                propertyId = doc.getString("propertyId") ?: "",
                propertyName = doc.getString("propertyName") ?: "",
                propertyAddress = doc.getString("propertyAddress") ?: "",
                customerId = doc.getString("customerId") ?: "",
                customerName = doc.getString("customerName") ?: "",
                customerEmail = doc.getString("customerEmail") ?: "",
                customerPhone = doc.getString("customerPhone") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                roomType = doc.getString("roomType") ?: "",
                sharingType = doc.getString("sharingType") ?: "",
                checkInDate = doc.getString("checkInDate") ?: "",
                durationMonths = doc.getLong("durationMonths")?.toInt() ?: 1,
                monthlyRent = doc.getLong("monthlyRent") ?: 0L,
                depositAmount = doc.getLong("depositAmount") ?: 0L,
                totalAmount = doc.getLong("totalAmount") ?: 0L,
                paymentStatus = doc.getString("paymentStatus") ?: "pending",
                status = doc.getString("status") ?: "pending",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapping doc ${doc.id} to Booking", e)
            null
        }
    }
}
