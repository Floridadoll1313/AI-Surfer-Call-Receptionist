package com.example

import android.view.KeyEvent
import com.example.ui.input.DuplicateKeyEventFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateKeyEventFilterTest {

  @Test
  fun duplicateKeyDownWithinBounceWindowIsSuppressedWhileRealDoubleTapAndBackspacePass() {
    val filter = DuplicateKeyEventFilter(bounceWindowMs = 40L)

    // First 'D' key press passes
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_D,
        unicodeChar = 'D'.code,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1000L
      )
    )

    // Duplicate 'D' key press 12ms later is suppressed (prevents "DD")
    assertTrue(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_D,
        unicodeChar = 'D'.code,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1012L
      )
    )

    // Real second 'D' press 110ms later passes (normal human typing)
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_D,
        unicodeChar = 'D'.code,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1110L
      )
    )

    // Different key right away (e.g. 'l' 15ms later) passes immediately
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_L,
        unicodeChar = 'l'.code,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1125L
      )
    )

    // Key hold repeat (repeatCount > 0) passes
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_L,
        unicodeChar = 'l'.code,
        repeatCount = 1,
        isSystem = false,
        eventTimeMs = 1130L
      )
    )

    // Rapid Backspace taps within 40ms are never suppressed
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_DEL,
        unicodeChar = 0,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1200L
      )
    )
    assertFalse(
      filter.shouldSuppressDuplicateKeyDown(
        action = KeyEvent.ACTION_DOWN,
        keyCode = KeyEvent.KEYCODE_DEL,
        unicodeChar = 0,
        repeatCount = 0,
        isSystem = false,
        eventTimeMs = 1210L
      )
    )
  }
}
