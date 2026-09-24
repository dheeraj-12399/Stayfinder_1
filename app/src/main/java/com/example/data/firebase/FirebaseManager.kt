package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed class PhoneAuthState {
    object Idle : PhoneAuthState()
    object CodeSending : PhoneAuthState()
    data class CodeSent(val verificationId: String, val token: PhoneAuthProvider.ForceResendingToken?) : PhoneAuthState()
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneAuthState()
    data class Success(val user: FirebaseUser) : PhoneAuthState()
    data class Error(val message: String, val isRateLimited: Boolean = false, val isVerificationFailed: Boolean = false) : PhoneAuthState()
}

class FirebaseManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "StayFinderFirebase"
        private const val PREFS_NAME = "stayfinder_firebase_prefs"
        private const val KEY_CUSTOM_PROJECT_ID = "custom_project_id"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_APP_ID = "custom_app_id"
        private const val KEY_CUSTOM_STORAGE_BUCKET = "custom_storage_bucket"

        @Volatile
        private var instance: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val isFirebaseInitialized: Boolean
        get() {
            return try {
                FirebaseApp.getApps(context).isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

    fun getAuth(): FirebaseAuth? {
        return try {
            if (isFirebaseInitialized) FirebaseAuth.getInstance() else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseAuth", e)
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return try {
            if (isFirebaseInitialized) FirebaseFirestore.getInstance() else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseFirestore", e)
            null
        }
    }

    fun getStorage(): FirebaseStorage? {
        return try {
            if (isFirebaseInitialized) FirebaseStorage.getInstance() else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FirebaseStorage", e)
            null
        }
    }

    fun getProjectId(): String {
        return try {
            if (isFirebaseInitialized) {
                FirebaseApp.getInstance().options.projectId ?: "stayfinder-firebase"
            } else {
                "Not initialized (Awaiting google-services.json)"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getApplicationId(): String {
        return context.packageName
    }

    /**
     * Extracts SHA-1 or SHA-256 certificate fingerprint for the current app signing keystore.
     * This helps the user copy the exact fingerprints to paste into Firebase Console.
     */
    fun getCertificateFingerprint(algorithm: String): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "Not found"
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(cert)
            digest.joinToString(":") { String.format(Locale.US, "%02X", it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating $algorithm fingerprint", e)
            "Error retrieving $algorithm"
        }
    }

    /**
     * Start Firebase Phone Authentication completely native inside the app.
     * Does NOT open browser, webview, or external intent.
     */
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        onStateChanged: (PhoneAuthState) -> Unit
    ) {
        val auth = getAuth()
        if (auth == null) {
            onStateChanged(
                PhoneAuthState.Error(
                    "Firebase is not initialized. Please ensure your Firebase configuration (google-services.json) is connected."
                )
            )
            return
        }

        onStateChanged(PhoneAuthState.CodeSending)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: Instant / auto verification completed")
                // Instant verification happened (e.g. Play Integrity or instant SMS verification)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        result.user?.let { user ->
                            ensureUserInFirestore(user, phoneNumber)
                            onStateChanged(PhoneAuthState.Success(user))
                        } ?: run {
                            onStateChanged(PhoneAuthState.AutoVerified(credential))
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "signInWithCredential failed in onVerificationCompleted", e)
                        onStateChanged(PhoneAuthState.Error(parseFirebaseError(e)))
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        "Invalid phone number. Please ensure the country code (e.g., +91) and digits are correct."
                    }
                    is FirebaseTooManyRequestsException -> {
                        "Too many requests. Firebase has temporarily rate-limited SMS requests for this phone number. Please wait a few minutes."
                    }
                    else -> {
                        parseFirebaseError(e)
                    }
                }
                onStateChanged(PhoneAuthState.Error(errorMessage, isRateLimited = e is FirebaseTooManyRequestsException, isVerificationFailed = true))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: verificationId=$verificationId")
                onStateChanged(PhoneAuthState.CodeSent(verificationId, token))
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Log.d(TAG, "onCodeAutoRetrievalTimeOut: verificationId=$verificationId")
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        try {
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "verifyPhoneNumber call failed", e)
            onStateChanged(PhoneAuthState.Error("Verification request failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    /**
     * Verifies the 6-digit OTP code entered by the user in the native StayFinder screen.
     */
    suspend fun verifyOtpCode(
        verificationId: String,
        otpCode: String,
        phoneNumber: String
    ): Result<FirebaseUser> {
        val auth = getAuth() ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("User is null after verification")
            ensureUserInFirestore(user, phoneNumber)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "verifyOtpCode failed", e)
            Result.failure(Exception(parseFirebaseError(e)))
        }
    }

    /**
     * Ensures `users/{firebaseUid}` exists or is updated with phone number and profile info.
     */
    fun ensureUserInFirestore(user: FirebaseUser, phone: String) {
        val firestore = getFirestore() ?: return
        val userRef = firestore.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newUser = hashMapOf(
                    "id" to user.uid,
                    "name" to (user.displayName ?: "StayFinder Guest"),
                    "email" to (user.email ?: ""),
                    "phone" to (user.phoneNumber ?: phone),
                    "whatsapp" to (user.phoneNumber ?: phone),
                    "gender" to "",
                    "collegeName" to "",
                    "role" to "tenant",
                    "isVerifiedOwner" to false,
                    "isApproved" to true,
                    "isBlocked" to false,
                    "profileImage" to (user.photoUrl?.toString() ?: ""),
                    "aadhaar" to "",
                    "createdAt" to System.currentTimeMillis()
                )
                userRef.set(newUser).addOnSuccessListener {
                    Log.d(TAG, "User document created for UID: ${user.uid}")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create user document", e)
                }
            } else {
                // Update phone if missing
                val currentPhone = snapshot.getString("phone")
                if (currentPhone.isNullOrBlank() && phone.isNotBlank()) {
                    userRef.update("phone", phone, "whatsapp", phone)
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking user document", e)
        }
    }

    private fun parseFirebaseError(e: Throwable): String {
        val msg = e.message ?: "Authentication failed."
        return when {
            msg.contains("The verification code from SMS is invalid", ignoreCase = true) ||
            msg.contains("invalid-verification-code", ignoreCase = true) ->
                "Invalid 6-digit OTP. Please check the code and try again."

            msg.contains("session-expired", ignoreCase = true) ||
            msg.contains("sms-code-has-expired", ignoreCase = true) ->
                "This verification code has expired. Please tap 'Resend Code'."

            msg.contains("quota-exceeded", ignoreCase = true) ->
                "SMS quota exceeded for today. Please try again later or contact support."

            msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Unable to connect. Please check your internet connection and try again."

            msg.contains("app-not-authorized", ignoreCase = true) ||
            msg.contains("SafetyNet", ignoreCase = true) ||
            msg.contains("Play Integrity", ignoreCase = true) ->
                "App verification error. Ensure your SHA-256 fingerprint is added to Firebase Console under Android App settings."

            else -> msg
        }
    }
}
