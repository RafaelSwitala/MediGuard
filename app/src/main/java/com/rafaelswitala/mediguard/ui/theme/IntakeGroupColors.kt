package com.rafaelswitala.mediguard.ui.theme

import androidx.compose.ui.graphics.Color

object IntakeGroupColors {
    val palette = listOf(
        Color(0xFFB3E5FC), // light blue
        Color(0xFFC8E6C9), // light green
        Color(0xFFFFF9C4), // light yellow
        Color(0xFFE1BEE7), // light purple
        Color(0xFFF8BBD0), // pink
        Color(0xFFFFE0B2)  // light orange
    )

    fun colorForGroupId(groupId: Int?): Color? {
        if (groupId == null || groupId <= 0) return null
        return palette[(groupId - 1) % palette.size]
    }
}
