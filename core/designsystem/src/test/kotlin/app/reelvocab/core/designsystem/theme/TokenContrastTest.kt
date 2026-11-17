package app.reelvocab.core.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class TokenContrastTest {

    @Test
    fun darkTextPairsMeetAa() {
        assertAa("dark primary", ReelArgb.DarkTextPrimary, ReelArgb.DarkBase, 7.0)
        assertAa("dark secondary", ReelArgb.DarkTextSecondary, ReelArgb.DarkBase, 4.5)
        assertAa("dark tertiary", ReelArgb.DarkTextTertiary, ReelArgb.DarkBase, 4.5)
        assertAa("dark gold on base", ReelArgb.DarkGold, ReelArgb.DarkBase, 4.5)
        assertAa("dark on-accent on gold", ReelArgb.DarkOnAccent, ReelArgb.DarkGold, 4.5)
        assertAa("dark gold on elevated", ReelArgb.DarkGold, ReelArgb.DarkElevated, 4.5)
    }

    @Test
    fun lightTextPairsMeetAa() {
        assertAa("light primary", ReelArgb.LightTextPrimary, ReelArgb.LightBase, 7.0)
        assertAa("light secondary", ReelArgb.LightTextSecondary, ReelArgb.LightBase, 4.5)
        assertAa("light tertiary", ReelArgb.LightTextTertiary, ReelArgb.LightBase, 4.5)
        assertAa("light gold on base", ReelArgb.LightGold, ReelArgb.LightBase, 4.5)
        assertAa("light gold on elevated", ReelArgb.LightGold, ReelArgb.LightElevated, 4.5)
        assertAa("light on-accent on gold", ReelArgb.LightOnAccent, ReelArgb.LightGold, 4.5)
        assertAa("light teal on base", ReelArgb.LightTeal, ReelArgb.LightBase, 4.5)
        assertAa("light rose on base", ReelArgb.LightRose, ReelArgb.LightBase, 4.5)
        assertAa("light green on base", ReelArgb.LightGreen, ReelArgb.LightBase, 4.5)
    }

    @Test
    fun gradeColorsStayDistinctFromEachOther() {
        val seen = setOf(
            ReelArgb.DarkRose,
            ReelArgb.DarkGoldDim,
            ReelArgb.DarkGold,
            ReelArgb.DarkGreen,
        )
        assertTrue("grade tones must be four distinct colors", seen.size == 4)
    }

    private fun assertAa(name: String, foreground: Long, background: Long, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$name contrast ${"%.2f".format(ratio)}:1 is below $minimum:1",
            ratio >= minimum,
        )
    }

    private fun contrastRatio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(argb: Long): Double {
        val r = channel(argb shr 16)
        val g = channel(argb shr 8)
        val b = channel(argb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(shifted: Long): Double {
        val raw = (shifted and 0xFF).toDouble() / 255.0
        return if (raw <= 0.03928) raw / 12.92 else ((raw + 0.055) / 1.055).pow(2.4)
    }
}
