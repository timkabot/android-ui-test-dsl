package example.uitestdsl.testing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import example.uitestdsl.Tags

class PromoScreen(private val rule: ComposeTestRule) {
    fun submit(code: String) {
        rule.onNodeWithTag(Tags.CODE).performTextInput(code)
        rule.onNodeWithTag(Tags.SUBMIT).performClick()
    }

    fun assertResult(expected: String) = assertText(Tags.RESULT, expected)
    fun loadProducts() { rule.onNodeWithTag(Tags.LOAD_PRODUCTS).performClick() }
    fun assertProducts(expected: String) = assertText(Tags.PRODUCTS, expected)

    fun assertPromoDisabled() {
        rule.onNodeWithTag(Tags.CODE).assertDoesNotExist()
        rule.onNodeWithTag(Tags.SUBMIT).assertDoesNotExist()
        rule.onNodeWithText("Промокоды недоступны").assertIsDisplayed()
    }

    private fun assertText(tag: String, expected: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasTestTag(tag) and hasText(expected)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(tag).assertIsDisplayed().assertTextEquals(expected)
    }
}
