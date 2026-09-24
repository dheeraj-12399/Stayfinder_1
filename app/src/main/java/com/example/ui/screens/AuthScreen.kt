package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.firebase.PhoneAuthState
import com.example.data.security.TurnstileValidationResult
import com.example.data.security.TurnstileVerificationService
import com.example.ui.components.FirebaseConfigSheet
import com.example.ui.components.TurnstileVerificationStatus
import com.example.ui.components.TurnstileWidget
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
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
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val firebaseManager = remember { FirebaseManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Steps: 0 = Phone number entry, 1 = 6-digit OTP entry
    var step by remember { mutableIntStateOf(0) }
    var rawPhone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdownSeconds by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }
    var showConfigSheet by remember { mutableStateOf(false) }

    // Cloudflare Turnstile Security Verification State
    val turnstileService = remember { TurnstileVerificationService.getInstance(context) }
    var turnstileStatus by remember { mutableStateOf(TurnstileVerificationStatus.IDLE) }
    var turnstileToken by remember { mutableStateOf<String?>(null) }
    var isTurnstileValidatedByBackend by remember { mutableStateOf(false) }

    // Countdown timer for Resend OTP
    LaunchedEffect(step, countdownSeconds, canResend) {
        if (step == 1 && countdownSeconds > 0 && !canResend) {
            delay(1000L)
            countdownSeconds -= 1
            if (countdownSeconds <= 0) {
                canResend = true
            }
        }
    }

    val formattedPhoneNumber = remember(rawPhone) {
        val clean = rawPhone.trim().replace(" ", "").replace("-", "")
        if (clean.startsWith("+")) clean else "+91$clean"
    }

    fun startVerification(isResend: Boolean = false) {
        if (activity == null) {
            errorMessage = "Activity not available for phone verification"
            return
        }

        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length < 10) {
            errorMessage = "Please enter a valid 10-digit mobile number."
            return
        }

        errorMessage = null
        isLoading = true
        keyboardController?.hide()

        firebaseManager.startPhoneVerification(
            phoneNumber = formattedPhoneNumber,
            activity = activity,
            resendToken = if (isResend) resendToken else null
        ) { state ->
            when (state) {
                is PhoneAuthState.CodeSending -> {
                    isLoading = true
                }
                is PhoneAuthState.CodeSent -> {
                    isLoading = false
                    verificationId = state.verificationId
                    resendToken = state.token
                    step = 1
                    countdownSeconds = 30
                    canResend = false
                }
                is PhoneAuthState.AutoVerified, is PhoneAuthState.Success -> {
                    isLoading = false
                    onAuthSuccess()
                }
                is PhoneAuthState.Error -> {
                    isLoading = false
                    errorMessage = state.message
                }
                else -> Unit
            }
        }
    }

    /**
     * Intercepts OTP sending with Cloudflare Turnstile security validation.
     * Sequence:
     * 1. Validate phone number.
     * 2. Ensure Turnstile token has been solved by client widget.
     * 3. Submit Turnstile token to StayFinder backend /api/verify-turnstile.
     * 4. If backend validates token successfully with Cloudflare Siteverify:
     *    Call existing startVerification() -> Firebase sends real SMS OTP.
     * 5. If Turnstile verification fails:
     *    STOP the flow. Do NOT send OTP. Reset Turnstile state and allow retry.
     */
    fun requestOtpWithTurnstileCheck(isResend: Boolean = false) {
        if (activity == null) {
            errorMessage = "Activity not available for phone verification"
            return
        }

        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length < 10) {
            errorMessage = "Please enter a valid 10-digit mobile number."
            return
        }

        if (isTurnstileValidatedByBackend) {
            // Already validated with backend; proceed directly to existing OTP flow
            startVerification(isResend)
            return
        }

        val token = turnstileToken
        if (token.isNullOrBlank()) {
            errorMessage = "Please complete the Cloudflare security verification."
            return
        }

        errorMessage = null
        isLoading = true
        keyboardController?.hide()
        turnstileStatus = TurnstileVerificationStatus.VALIDATING_BACKEND

        scope.launch {
            when (val validation = turnstileService.verifyTokenWithBackend(token)) {
                is TurnstileValidationResult.Success -> {
                    turnstileStatus = TurnstileVerificationStatus.VERIFIED
                    isTurnstileValidatedByBackend = true
                    // Proceed immediately to call existing Firebase Phone Auth OTP sending
                    startVerification(isResend)
                }
                is TurnstileValidationResult.Failure -> {
                    isLoading = false
                    turnstileStatus = TurnstileVerificationStatus.FAILED
                    turnstileToken = null
                    isTurnstileValidatedByBackend = false
                    errorMessage = validation.message
                }
            }
        }
    }

    fun verifyEnteredOtp() {
        if (otpCode.length < 6) {
            errorMessage = "Please enter the complete 6-digit OTP code."
            return
        }

        errorMessage = null
        isLoading = true
        keyboardController?.hide()

        scope.launch {
            val result = firebaseManager.verifyOtpCode(
                verificationId = verificationId,
                otpCode = otpCode,
                phoneNumber = formattedPhoneNumber
            )

            isLoading = false
            if (result.isSuccess) {
                onAuthSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Invalid OTP code. Please try again."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDarker)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Bar with setup shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step == 1) {
                    IconButton(
                        onClick = {
                            step = 0
                            errorMessage = null
                            otpCode = ""
                            turnstileToken = null
                            isTurnstileValidatedByBackend = false
                            turnstileStatus = TurnstileVerificationStatus.IDLE
                        },
                        modifier = Modifier.testTag("auth_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SlateTextPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Firebase Certificate & Configuration Sheet
                IconButton(
                    onClick = { showConfigSheet = true },
                    modifier = Modifier.testTag("btn_firebase_config")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Firebase Setup & SHA-256",
                        tint = EmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Emblem
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SlateCard,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary.copy(alpha = 0.4f))
                ),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Apartment,
                        contentDescription = "StayFinder Logo",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "StayFinder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )

            Text(
                text = if (step == 0) "Find verified student PGs, rooms & flats" else "Native In-App Phone Verification",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SlateBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (step == 0) {
                        // STEP 0: PHONE NUMBER
                        Text(
                            text = "Login or Sign Up",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter your mobile number to receive a secure SMS verification code",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Phone TextField
                        OutlinedTextField(
                            value = rawPhone,
                            onValueChange = { input ->
                                if (input.length <= 15) {
                                    rawPhone = input
                                    errorMessage = null
                                }
                            },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("98765 43210") },
                            leadingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+91",
                                        fontWeight = FontWeight.Bold,
                                        color = SlateTextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(16.dp)
                                            .background(SlateBorder)
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { requestOtpWithTurnstileCheck() }),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_input"),
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

                        // Cloudflare Turnstile Anti-Bot Security Verification Layer
                        TurnstileWidget(
                            status = turnstileStatus,
                            onTokenReceived = { token ->
                                turnstileToken = token
                                turnstileStatus = TurnstileVerificationStatus.TOKEN_RECEIVED
                                errorMessage = null
                            },
                            onError = { err ->
                                turnstileStatus = TurnstileVerificationStatus.FAILED
                                turnstileToken = null
                                isTurnstileValidatedByBackend = false
                                errorMessage = "Security verification error: $err"
                            },
                            onExpired = {
                                turnstileStatus = TurnstileVerificationStatus.EXPIRED
                                turnstileToken = null
                                isTurnstileValidatedByBackend = false
                            },
                            onResetRequested = {
                                turnstileStatus = TurnstileVerificationStatus.IDLE
                                turnstileToken = null
                                isTurnstileValidatedByBackend = false
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { requestOtpWithTurnstileCheck() },
                            enabled = !isLoading && rawPhone.filter { it.isDigit() }.length >= 10 && !turnstileToken.isNullOrBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                disabledContainerColor = SlateBorder
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Send Verification Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // STEP 1: 6-DIGIT OTP VERIFICATION
                        Text(
                            text = "Enter 6-Digit OTP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Enter the 6-digit OTP sent to\n$formattedPhoneNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 6 Digit OTP Input Boxes
                        OtpInputField(
                            otp = otpCode,
                            onOtpChange = {
                                if (it.length <= 6) {
                                    otpCode = it
                                    errorMessage = null
                                    if (it.length == 6) {
                                        verifyEnteredOtp()
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { verifyEnteredOtp() },
                            enabled = !isLoading && otpCode.length == 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                disabledContainerColor = SlateBorder
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Verify OTP & Continue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend Countdown
                        if (!canResend) {
                            Text(
                                text = "Resend OTP in $countdownSeconds seconds",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextMuted
                            )
                        } else {
                            TextButton(
                                onClick = { startVerification(isResend = true) },
                                enabled = !isLoading,
                                modifier = Modifier.testTag("resend_otp_button")
                            ) {
                                Text(
                                    text = "Resend OTP",
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = AccentRose.copy(alpha = 0.15f)),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(AccentRose.copy(alpha = 0.4f))
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = AccentRose,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateTextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trust & Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showConfigSheet = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = SlateTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured with Cloudflare Turnstile & Firebase Phone Auth",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Firebase Configuration Bottom Sheet
        if (showConfigSheet) {
            ModalBottomSheet(
                onDismissRequest = { showConfigSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SlateCard
            ) {
                FirebaseConfigSheet(onDismiss = { showConfigSheet = false })
            }
        }
    }
}

/**
 * 6-Digit Native OTP Input Component with 6 distinct boxes.
 */
@Composable
private fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit
) {
    BasicTextField(
        value = TextFieldValue(otp, selection = TextRange(otp.length)),
        onValueChange = { onOtpChange(it.text.filter { char -> char.isDigit() }) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val char = otp.getOrNull(i)?.toString() ?: ""
                    val isFocused = i == otp.length
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(SlateDark, RoundedCornerShape(10.dp))
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) EmeraldPrimary else SlateBorder,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (char.isNotEmpty()) EmeraldLight else SlateTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        modifier = Modifier.testTag("otp_input_field")
    )
}
