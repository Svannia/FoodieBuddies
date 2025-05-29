package com.example.foodiebuddy.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class ArcShape(private val arcHeight: Float = 100f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height - arcHeight)
            quadraticBezierTo(
                size.width / 2, size.height + arcHeight,
                size.width, size.height - arcHeight
            )
            lineTo(size.width, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}