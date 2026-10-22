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
    fun testNewPlayerScreenRendersAndAllowsInput() {
        composeTestRule.setContent {
            ConnectFourApp()
        }

        // verrify welcome text is shown
        composeTestRule.onNodeWithText("Welcome to Connect Four!").assertIsDisplayed()
        
        // Verify enter your name and create player button / label exists in the UI element.
        composeTestRule.onNodeWithText("Enter your name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Player").assertIsDisplayed()

        // Perform text input interaction
        composeTestRule.onNodeWithText("Enter your name").performTextInput("ProPlayer1")
    }

    @Test
    fun testGameScreenComponentsExist() {

        // test for app navigation correctly
        composeTestRule.setContent {
            ConnectFourApp()
        }
        
        composeTestRule.onNodeWithText("Welcome to Connect Four!").assertIsDisplayed()
    }
}
