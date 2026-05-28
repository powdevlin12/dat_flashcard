package com.dttrn.datfs.feature.study

import org.junit.Assert.*
import org.junit.Test

class DictationTest {

    @Test
    fun `isDictationMatch - exact match`() {
        assertTrue(isDictationMatch("hello", "hello"))
    }

    @Test
    fun `isDictationMatch - different case`() {
        assertTrue(isDictationMatch("Hello", "hello"))
    }

    @Test
    fun `isDictationMatch - extra whitespace`() {
        assertTrue(isDictationMatch("  hello world  ", "hello world"))
    }

    @Test
    fun `isDictationMatch - punctuation difference`() {
        assertTrue(isDictationMatch("hello, world!", "hello world"))
    }

    @Test
    fun `isDictationMatch - both punctuation and case difference`() {
        assertTrue(isDictationMatch("Hello, World!", "hello world"))
    }

    @Test
    fun `isDictationMatch - wrong answer`() {
        assertFalse(isDictationMatch("goodbye", "hello"))
    }

    @Test
    fun `isDictationMatch - partially correct`() {
        assertFalse(isDictationMatch("hello", "hello world"))
    }

    @Test
    fun `isDictationMatch - empty answer`() {
        assertFalse(isDictationMatch("", "hello"))
    }

    @Test
    fun `isDictationMatch - both empty`() {
        assertTrue(isDictationMatch("", ""))
    }

    @Test
    fun `isDictationMatch - numbers and symbols stripped`() {
        assertTrue(isDictationMatch("it's a test.", "its a test"))
    }
}

private fun isDictationMatch(userAnswer: String, correctAnswer: String): Boolean {
    val normalizedUser = userAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    val normalizedCorrect = correctAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    return normalizedUser == normalizedCorrect
}
