package com.example.semestra.logic

import java.security.SecureRandom

object TempPasswordGenerator {
    private val random = SecureRandom()
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val ALL = UPPER + LOWER + DIGITS

    /** Meets Semestra rules: letters, at least one digit, at least one capital, length >= 8. */
    fun generate(length: Int = 12): String {
        require(length >= 8)
        val buf = CharArray(length)
        buf[0] = UPPER[random.nextInt(UPPER.length)]
        buf[1] = LOWER[random.nextInt(LOWER.length)]
        buf[2] = DIGITS[random.nextInt(DIGITS.length)]
        for (i in 3 until length) {
            buf[i] = ALL[random.nextInt(ALL.length)]
        }
        return buf.shuffleInPlace().concatToString()
    }

    private fun CharArray.shuffleInPlace(): CharArray {
        for (i in lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val t = this[i]
            this[i] = this[j]
            this[j] = t
        }
        return this
    }
}
