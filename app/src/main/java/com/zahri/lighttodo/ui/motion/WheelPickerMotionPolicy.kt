package com.zahri.lighttodo.ui.motion

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Immutable
data class WheelPickerTextMotion(
    val selectedFontSize: TextUnit,
    val unselectedFontSize: TextUnit
)

object WheelPickerMotionPolicy {
    const val CenterScaleBoost = 0.28f
    const val FarItemAlpha = 0.42f
    const val CenterItemAlpha = 1f

    val DateColumnTextMotion = WheelPickerTextMotion(
        selectedFontSize = 18.sp,
        unselectedFontSize = 14.sp
    )
    val TimeColumnTextMotion = WheelPickerTextMotion(
        selectedFontSize = 24.sp,
        unselectedFontSize = 16.sp
    )
    val DayColumnTextMotion = WheelPickerTextMotion(
        selectedFontSize = 22.sp,
        unselectedFontSize = 15.sp
    )

    fun proximityRadiusPx(itemHeightPx: Float): Float =
        itemHeightPx * AppMotion.PickerItemProximityRadiusItems

    fun proximityForDistance(distancePx: Float, radiusPx: Float): Float {
        if (radiusPx <= 0f) return 0f
        return (1f - kotlin.math.abs(distancePx) / radiusPx).coerceIn(0f, 1f)
    }

    fun scaleForProximity(proximity: Float): Float =
        1f + normalizedProximity(proximity) * CenterScaleBoost

    fun alphaForProximity(proximity: Float): Float =
        FarItemAlpha + normalizedProximity(proximity) * (CenterItemAlpha - FarItemAlpha)

    fun fontSizeForProximity(textMotion: WheelPickerTextMotion, proximity: Float): TextUnit {
        val p = normalizedProximity(proximity)
        val size = textMotion.unselectedFontSize.value +
            (textMotion.selectedFontSize.value - textMotion.unselectedFontSize.value) * p
        return size.sp
    }

    fun colorForProximity(
        selectedColor: Color,
        unselectedColor: Color,
        proximity: Float
    ): Color =
        lerp(unselectedColor, selectedColor, normalizedProximity(proximity))

    fun fontWeightForProximity(proximity: Float): FontWeight {
        val p = normalizedProximity(proximity)
        val weight = FontWeight.Normal.weight +
            ((FontWeight.SemiBold.weight - FontWeight.Normal.weight) * p).roundToInt()
        return FontWeight(weight)
    }

    private fun normalizedProximity(proximity: Float): Float =
        proximity.coerceIn(0f, 1f)
}
