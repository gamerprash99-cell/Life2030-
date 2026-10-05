package com.lifeos.app.domain.intelligence

import com.lifeos.app.domain.model.CaptureConfidence
import com.lifeos.app.domain.model.ExpenseCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategorizerTest {

    @Test
    fun `an exact brand match is trusted`() {
        assertEquals("Food", ExpenseCategorizer.categorize("Swiggy")?.category)
        assertEquals(CaptureConfidence.HIGH, ExpenseCategorizer.categorize("Starbucks")?.confidence)
    }

    @Test
    fun `a brand inside a longer name is matched with less trust`() {
        val suggestion = ExpenseCategorizer.categorize("SWIGGY EATS BANGALORE")

        assertEquals("Food", suggestion?.category)
        assertEquals(CaptureConfidence.MEDIUM, suggestion?.confidence)
    }

    @Test
    fun `punctuation and domains do not stop a match`() {
        assertEquals("Food", ExpenseCategorizer.categorize("Swiggy.in")?.category)
        assertEquals("Food", ExpenseCategorizer.categorize("Domino's")?.category)
        assertEquals("Travel", ExpenseCategorizer.categorize("Ola Cabs")?.category)
    }

    @Test
    fun `casing does not matter`() {
        assertEquals("Travel", ExpenseCategorizer.categorize("UBER")?.category)
        assertEquals("Subscriptions", ExpenseCategorizer.categorize("netflix")?.category)
    }

    @Test
    fun `subscriptions win over shopping for an ambiguous name`() {
        assertEquals("Subscriptions", ExpenseCategorizer.categorize("Amazon Prime Video")?.category)
    }

    @Test
    fun `a short token does not match inside an unrelated word`() {
        // "ola" sits inside "cholamandalam"; substring matching would file an
        // electricity bill as transport.
        assertNull(ExpenseCategorizer.categorize("Cholamandalam MS"))
        assertNull(ExpenseCategorizer.categorize("Saffron Card Services"))
    }

    @Test
    fun `an unknown merchant has no suggestion`() {
        assertNull(ExpenseCategorizer.categorize("Kettle and Kettle Enterprises"))
        assertNull(ExpenseCategorizer.categorize(""))
        assertNull(ExpenseCategorizer.categorize("   "))
        assertNull(ExpenseCategorizer.categorize(null))
    }

    @Test
    fun `unknown merchants fall back to Other`() {
        assertEquals(ExpenseCategories.DEFAULT, ExpenseCategorizer.categoryOrDefault("Kettle and Kettle"))
        assertEquals(ExpenseCategories.DEFAULT, ExpenseCategorizer.categoryOrDefault(null))
        assertEquals(ExpenseCategories.DEFAULT, ExpenseCategorizer.categoryOrDefault(""))
    }

    @Test
    fun `every suggestion is a category the app already offers`() {
        val merchants = listOf(
            "Uber", "Swiggy", "Netflix", "Amazon", "Airtel", "Apollo", "Starbucks",
            "Zomato", "IRCTC", "Coursera", "BookMyShow", "DMart", "1mg", "Ola Cabs"
        )
        for (merchant in merchants) {
            val category = ExpenseCategorizer.categoryOrDefault(merchant)
            assertTrue("$merchant produced $category", ExpenseCategories.isKnown(category))
        }
    }

    @Test
    fun `the default category is itself a real category`() {
        assertTrue(ExpenseCategories.isKnown(ExpenseCategories.DEFAULT))
        assertFalse(ExpenseCategories.ALL.none { it.name == ExpenseCategories.DEFAULT })
    }
}