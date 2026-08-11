package com.example.appmunicipal.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Mantiene los controles fuera de la barra de estado y del recorte de cámara.
 * Además deja 8dp extra arriba para que acciones como Volver/Cancelar no queden
 * pegadas a la cámara en dispositivos con pantalla edge-to-edge (API 36).
 */
fun View.applySafeSystemInsets(extraTopDp: Int = 8) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    val extraTopPx = (extraTopDp * resources.displayMetrics.density).toInt()

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        view.setPadding(
            initialLeft + maxOf(bars.left, cutout.left),
            initialTop + maxOf(bars.top, cutout.top) + extraTopPx,
            initialRight + maxOf(bars.right, cutout.right),
            initialBottom + maxOf(bars.bottom, cutout.bottom)
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
