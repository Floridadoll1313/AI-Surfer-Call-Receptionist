package com.example.ui.input

import android.view.KeyEvent

/**
 * Filters hardware/emulator keyboard bounce where a single physical or WebRTC-bridged key press
 * dispatches two identical ACTION_DOWN events within a tiny bounce window (~0-40ms).
 *
 * Intentionally ignores:
 * - Key auto-repeats (repeatCount > 0) when a key is held down
 * - Non-printable / navigation / deletion keys (Back, Delete, Enter, D-Pad, modifiers)
 * - Normal human double-taps (> 40ms apart)
 */
class DuplicateKeyEventFilter(
  private val bounceWindowMs: Long = 40L
) {
  private var lastKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN
  private var lastUnicodeChar: Int = 0
  private var lastEventTimeMs: Long = -1L

  fun shouldSuppressDuplicateKeyDown(
    action: Int,
    keyCode: Int,
    unicodeChar: Int,
    repeatCount: Int,
    isSystem: Boolean,
    eventTimeMs: Long
  ): Boolean {
    if (action != KeyEvent.ACTION_DOWN) return false
    if (repeatCount > 0 || isSystem || unicodeChar == 0) return false
    if (isExcludedControlKey(keyCode)) return false

    val delta = if (lastEventTimeMs >= 0L) eventTimeMs - lastEventTimeMs else Long.MAX_VALUE
    val isDuplicateBounce =
      keyCode == lastKeyCode &&
        unicodeChar == lastUnicodeChar &&
        delta in 0L..bounceWindowMs

    if (!isDuplicateBounce) {
      lastKeyCode = keyCode
      lastUnicodeChar = unicodeChar
      lastEventTimeMs = eventTimeMs
    }
    return isDuplicateBounce
  }

  fun shouldSuppressDuplicateKeyEvent(event: KeyEvent, fallbackTimeMs: Long): Boolean {
    val eventTime = if (event.eventTime > 0L) event.eventTime else fallbackTimeMs
    return shouldSuppressDuplicateKeyDown(
      action = event.action,
      keyCode = event.keyCode,
      unicodeChar = event.unicodeChar,
      repeatCount = event.repeatCount,
      isSystem = event.isSystem,
      eventTimeMs = eventTime
    )
  }

  private fun isExcludedControlKey(keyCode: Int): Boolean = when (keyCode) {
    KeyEvent.KEYCODE_DEL,
    KeyEvent.KEYCODE_FORWARD_DEL,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_TAB,
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_BACK -> true
    else -> false
  }
}
