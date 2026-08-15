package com.energy.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom icons (kept out of material-icons-extended, which would balloon
 * the debug dex). Paths are standard Material icon outlines.
 */
object EnergyIcons {
    val TrendingUp: ImageVector by lazy {
        materialIcon(name = "Energy.TrendingUp") {
            materialPath {
                moveTo(16.0f, 6.0f)
                lineToRelative(2.29f, 2.29f)
                lineToRelative(-4.88f, 4.88f)
                lineToRelative(-4.0f, -4.0f)
                lineTo(2.0f, 16.59f)
                lineTo(3.41f, 18.0f)
                lineToRelative(6.0f, -6.0f)
                lineToRelative(4.0f, 4.0f)
                lineToRelative(6.3f, -6.29f)
                lineTo(22.0f, 12.0f)
                lineTo(22.0f, 6.0f)
                close()
            }
        }
    }
}
