package com.oscar.connectfour

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectFourUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNewPlayerScreenRenders() {
        composeTestRule.setContent {
            ConnectFourApp()
        }

        // Verify welcome text is displayed
        composeTestRule.onNodeWithText("Welcome to Connect Four!").assertIsDisplayed()
        // Verify text field label
        composeTestRule.onNodeWithText("Enter your name").assertIsDisplayed()
        // Verify create player button
        composeTestRule.onNodeWithText("Create Player").assertIsDisplayed()
    }

    @Test
    fun testPlayerNameInputAndInteraction() {
        composeTestRule.setContent {
            ConnectFourApp()
        }

        // Type player name
        composeTestRule.onNodeWithText("Enter your name").performTextInput("TestPlayer")
        composeTestRule.onNodeWithText("Create Player").assertIsDisplayed()
    }
}
