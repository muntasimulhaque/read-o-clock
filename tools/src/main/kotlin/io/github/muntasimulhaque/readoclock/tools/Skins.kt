package io.github.muntasimulhaque.readoclock.tools

import java.awt.Color

/**
 * One design take: the palette of a dial. Everything else (geometry, hands,
 * numerals) is shared, so the owner judges material and color only.
 */
data class Skin(
    val id: String,
    val name: String,
    val field: Color,
    val dial: Color,
    val dialEdge: Color,
    val numerals: Color,
    val ticks: Color,
    val hands: Color,
    val secondHand: Color,
    val caseFill: Color,
    val caseEdge: Color,
)

object Skins {
    val all: List<Skin> = listOf(
        Skin(
            id = "a-schoolhouse",
            name = "Schoolhouse enamel",
            field = Color(0xFFE0DBD0.toInt()),
            dial = Color(0xFFF7F2E6.toInt()),
            dialEdge = Color(0xFFE6DECB.toInt()),
            numerals = Color(0xFF26282C.toInt()),
            ticks = Color(0xFF2E3136.toInt()),
            hands = Color(0xFF202329.toInt()),
            secondHand = Color(0xFFC6402F.toInt()),
            caseFill = Color(0xFF322D27.toInt()),
            caseEdge = Color(0xFF171410.toInt()),
        ),
        Skin(
            id = "b-porcelain",
            name = "Porcelain kitchen",
            field = Color(0xFFE6E9EC.toInt()),
            dial = Color(0xFFFFFFFF.toInt()),
            dialEdge = Color(0xFFE8ECEF.toInt()),
            numerals = Color(0xFF15181B.toInt()),
            ticks = Color(0xFF1D2124.toInt()),
            hands = Color(0xFF101316.toInt()),
            secondHand = Color(0xFFD8453C.toInt()),
            caseFill = Color(0xFFC6CBD0.toInt()),
            caseEdge = Color(0xFF82898F.toInt()),
        ),
        Skin(
            id = "c-sunrise",
            name = "Sunrise",
            field = Color(0xFFF7E7CE.toInt()),
            dial = Color(0xFFFFF9EC.toInt()),
            dialEdge = Color(0xFFF1E0C2.toInt()),
            numerals = Color(0xFF2E4A6B.toInt()),
            ticks = Color(0xFF47678A.toInt()),
            hands = Color(0xFF2E4A6B.toInt()),
            secondHand = Color(0xFFFF6F55.toInt()),
            caseFill = Color(0xFFC9905F.toInt()),
            caseEdge = Color(0xFF9C6836.toInt()),
        ),
    )
}
