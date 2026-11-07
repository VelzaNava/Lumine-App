package com.thesis.lumine.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChangePasswordDialogTest {

    @Test
    fun aValidChangeHasNoProblem() {
        assertNull(passwordChangeProblem("old-password", "new-password", "new-password"))
    }

    @Test
    fun theNewPasswordNeedsEightCharacters() {
        assertEquals(
            "Password must be at least 8 characters.",
            passwordChangeProblem("old-password", "short", "short")
        )
    }

    @Test
    fun theConfirmationMustMatch() {
        assertEquals(
            "Passwords don't match.",
            passwordChangeProblem("old-password", "new-password", "new-passw0rd")
        )
    }

    @Test
    fun theNewPasswordMustDiffer() {
        assertEquals(
            "Please choose a different password.",
            passwordChangeProblem("same-password", "same-password", "same-password")
        )
    }
}
