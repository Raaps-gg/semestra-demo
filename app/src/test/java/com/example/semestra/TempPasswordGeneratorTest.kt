package com.example.semestra

import com.example.semestra.logic.TempPasswordGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class TempPasswordGeneratorTest {

    @Test
    fun generatedPassword_meetsRules() {
        repeat(20) {
            val p = TempPasswordGenerator.generate()
            assertTrue(p.length >= 8)
            assertTrue(p.any { it.isUpperCase() })
            assertTrue(p.any { it.isLowerCase() })
            assertTrue(p.any { it.isDigit() })
        }
    }
}
