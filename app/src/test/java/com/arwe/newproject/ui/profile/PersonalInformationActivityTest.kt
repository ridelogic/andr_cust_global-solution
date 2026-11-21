package com.arwe.newproject.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PersonalInformationActivity is read-only and has no ViewModel - the only extractable logic is
 * displayValueOrNotAvailable(), which decides what each SessionManager field renders as. Verifies
 * the four real fields (name/code/mobile/email) when present, and the null/blank fallback.
 */
class PersonalInformationActivityTest {

    @Test
    fun `a present full name is shown as-is`() {
        assertEquals("Vinoth", displayValueOrNotAvailable("Vinoth", "Not available"))
    }

    @Test
    fun `a present customer code is shown as-is`() {
        assertEquals("CUST-0007", displayValueOrNotAvailable("CUST-0007", "Not available"))
    }

    @Test
    fun `a present mobile number is shown as-is`() {
        assertEquals("9791222882", displayValueOrNotAvailable("9791222882", "Not available"))
    }

    @Test
    fun `a present email address is shown as-is`() {
        assertEquals("vinoth@example.com", displayValueOrNotAvailable("vinoth@example.com", "Not available"))
    }

    @Test
    fun `a null value falls back to Not available`() {
        assertEquals("Not available", displayValueOrNotAvailable(null, "Not available"))
    }

    @Test
    fun `a blank value falls back to Not available`() {
        assertEquals("Not available", displayValueOrNotAvailable("   ", "Not available"))
    }

    @Test
    fun `an empty string falls back to Not available`() {
        assertEquals("Not available", displayValueOrNotAvailable("", "Not available"))
    }
}
