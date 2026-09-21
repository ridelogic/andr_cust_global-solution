package com.arwe.newproject.ui.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductCatalogTest {

    @Test
    fun `known battery codes map to the exact four supported products`() {
        assertEquals("car_battery", ProductCatalog.batteryProductForCode("CAR_BATTERY")?.id)
        assertEquals("bike_battery", ProductCatalog.batteryProductForCode("BIKE_BATTERY")?.id)
        assertEquals("ups_battery", ProductCatalog.batteryProductForCode("UPS_BATTERY")?.id)
        assertEquals("other_battery", ProductCatalog.batteryProductForCode("OTHER_BATTERY")?.id)
    }

    @Test
    fun `code matching is case-insensitive`() {
        assertEquals("car_battery", ProductCatalog.batteryProductForCode("car_battery")?.id)
    }

    @Test
    fun `an unrecognized code such as a future Mobile Phone Battery returns null`() {
        assertNull(ProductCatalog.batteryProductForCode("MOBILE_PHONE_BATTERY"))
        assertNull(ProductCatalog.batteryProductForCode(""))
        assertNull(ProductCatalog.batteryProductForCode("SOME_UNRELATED_CODE"))
    }

    @Test
    fun `productsFor BATTERY still returns exactly the four supported products unchanged`() {
        val ids = ProductCatalog.productsFor(ServiceCategory.BATTERY).map { it.id }

        assertEquals(listOf("car_battery", "bike_battery", "ups_battery", "other_battery"), ids)
    }
}
