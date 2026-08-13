package com.example.appmunicipal.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Mantiene el contenido fuera de la barra de estado, la barra de navegacion y el
 * recorte de camara.
 *
 * Con [topAnchor] el inset superior se aplica a la barra de la pantalla en lugar de
 * a la raiz, para que el color verde de la barra llegue hasta el borde de la pantalla
 * en vez de dejar una franja del color de fondo.
 */
fun View.applySafeSystemInsets(topAnchor: View? = null, extraTopDp: Int = 0) {
    val raizIzquierda = paddingLeft
    val raizArriba = paddingTop
    val raizDerecha = paddingRight
    val raizAbajo = paddingBottom
    val anclaArriba = topAnchor?.paddingTop ?: 0
    val extraArribaPx = (extraTopDp * resources.displayMetrics.density).toInt()

    ViewCompat.setOnApplyWindowInsetsListener(this) { vista, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val recorte = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val arriba = maxOf(barras.top, recorte.top)
        val izquierda = maxOf(barras.left, recorte.left)
        val derecha = maxOf(barras.right, recorte.right)
        val abajo = maxOf(barras.bottom, recorte.bottom)

        if (topAnchor == null) {
            vista.setPadding(
                raizIzquierda + izquierda,
                raizArriba + arriba + extraArribaPx,
                raizDerecha + derecha,
                raizAbajo + abajo
            )
        } else {
            vista.setPadding(
                raizIzquierda + izquierda,
                raizArriba,
                raizDerecha + derecha,
                raizAbajo + abajo
            )
            topAnchor.setPadding(
                topAnchor.paddingLeft,
                anclaArriba + arriba + extraArribaPx,
                topAnchor.paddingRight,
                topAnchor.paddingBottom
            )
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
