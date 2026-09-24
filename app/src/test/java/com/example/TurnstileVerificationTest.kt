package com.example

import com.example.data.security.TurnstileValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnstileVerificationTest {

    @Test
    fun testTurnstileValidationResult_success() {
        val result: TurnstileValidationResult = TurnstileValidationResult.Success
        assertTrue(result is TurnstileValidationResult.Success)
    }

    @Test
    fun testTurnstileValidationResult_failure() {
        val errorMsg = "Security verification failed"
        val result: TurnstileValidationResult = TurnstileValidationResult.Failure(errorMsg)
        assertTrue(result is TurnstileValidationResult.Failure)
        assertEquals(errorMsg, (result as TurnstileValidationResult.Failure).message)
    }
}
