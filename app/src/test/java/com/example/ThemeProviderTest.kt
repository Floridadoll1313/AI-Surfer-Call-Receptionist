package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.integrations.IntegrationPrefsStore
import com.example.ui.screens.OceanThemeSwitcherCard
import com.example.ui.theme.OceanPalette
import com.example.ui.theme.OceanTheme
import com.example.ui.theme.ThemeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeProviderTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun themeProvider_exposesDistinctOceanColorsAndMaterialColorScheme() {
    var deepSeaPrimary = 0UL
    var sunsetPrimary = 0UL
    var reefAbyss = 0UL
    var arcticAbyss = 0UL

    composeTestRule.setContent {
      var activePalette by remember { mutableStateOf(OceanPalette.DEEP_SEA) }
      ThemeProvider(
        palette = activePalette,
        onSelectPalette = { activePalette = it },
        animateTransitions = false
      ) {
        val oceanColors = OceanTheme.colors
        val materialPrimary = MaterialTheme.colorScheme.primary
        when (activePalette) {
          OceanPalette.DEEP_SEA -> deepSeaPrimary = materialPrimary.value
          OceanPalette.SUNSET_TIDE -> sunsetPrimary = materialPrimary.value
          OceanPalette.REEF_GLOW -> reefAbyss = oceanColors.abyss.value
          OceanPalette.ARCTIC_CREST -> arcticAbyss = oceanColors.abyss.value
        }
        val controller = OceanTheme.controller
        OceanThemeSwitcherCard(
          activePalette = controller.currentPalette,
          onSelectPalette = controller.selectPalette,
          onCyclePalette = controller.cyclePalette
        )
      }
    }

    composeTestRule.onNodeWithTag("theme_option_sunset_tide").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("theme_option_reef_glow").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("theme_option_arctic_crest").performClick()
    composeTestRule.waitForIdle()

    assertEquals(OceanPalette.DEEP_SEA.colors.turquoise.value, deepSeaPrimary)
    assertEquals(OceanPalette.SUNSET_TIDE.colors.turquoise.value, sunsetPrimary)
    assertEquals(OceanPalette.REEF_GLOW.colors.abyss.value, reefAbyss)
    assertEquals(OceanPalette.ARCTIC_CREST.colors.abyss.value, arcticAbyss)
    assertNotEquals(deepSeaPrimary, sunsetPrimary)
    assertNotEquals(reefAbyss, arcticAbyss)
  }

  @Test
  fun oceanThemeSwitcherCard_allowsSelectingAndCyclingPalettes() {
    composeTestRule.setContent {
      var activePalette by remember { mutableStateOf(OceanPalette.DEEP_SEA) }
      ThemeProvider(
        palette = activePalette,
        onSelectPalette = { activePalette = it },
        animateTransitions = false
      ) {
        val controller = OceanTheme.controller
        androidx.compose.foundation.layout.Column {
          OceanThemeSwitcherCard(
            activePalette = controller.currentPalette,
            onSelectPalette = controller.selectPalette,
            onCyclePalette = controller.cyclePalette
          )
          Text(
            text = OceanTheme.controller.currentPalette.displayName,
            modifier = Modifier.testTag("active_palette_probe")
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("theme_switcher_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_option_deep_sea").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_option_sunset_tide").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_option_reef_glow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("theme_option_arctic_crest").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sunset Tide").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reef Glow").assertIsDisplayed()
    composeTestRule.onNodeWithText("Arctic Crest").assertIsDisplayed()

    composeTestRule.onNodeWithTag("active_palette_probe").assertTextContains("Deep Sea")

    // Cycle to Sunset Tide
    composeTestRule.onNodeWithTag("cycle_theme_button").performClick()
    composeTestRule.onNodeWithTag("active_palette_probe").assertTextContains("Sunset Tide")

    // Cycle to Reef Glow
    composeTestRule.onNodeWithTag("cycle_theme_button").performClick()
    composeTestRule.onNodeWithTag("active_palette_probe").assertTextContains("Reef Glow")

    // Direct selection back to Arctic Crest then Deep Sea
    composeTestRule.onNodeWithTag("theme_option_arctic_crest").performClick()
    composeTestRule.onNodeWithTag("active_palette_probe").assertTextContains("Arctic Crest")

    composeTestRule.onNodeWithTag("theme_option_deep_sea").performClick()
    composeTestRule.onNodeWithTag("active_palette_probe").assertTextContains("Deep Sea")
  }

  @Test
  fun integrationPrefsStore_persistsSelectedOceanPalette() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals(OceanPalette.DEEP_SEA, IntegrationPrefsStore.loadOceanPalette(context))

    IntegrationPrefsStore.saveOceanPalette(context, OceanPalette.SUNSET_TIDE)
    assertEquals(OceanPalette.SUNSET_TIDE, IntegrationPrefsStore.loadOceanPalette(context))

    IntegrationPrefsStore.saveOceanPalette(context, OceanPalette.REEF_GLOW)
    assertEquals(OceanPalette.REEF_GLOW, IntegrationPrefsStore.loadOceanPalette(context))
  }
}
