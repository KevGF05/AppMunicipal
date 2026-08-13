package com.example.appmunicipal.util

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

/**
 * Conecta la barra superior con el boton de retroceso del sistema.
 *
 * Antes cada pantalla tenia su propio MaterialButton "Volver" o "← Dashboard", que
 * ocupaba una fila entera y no coincidia con el gesto de retroceso del telefono.
 *
 * No se usa setSupportActionBar a proposito: la barra funciona como una vista normal,
 * asi el menu declarado con app:menu en el XML sigue siendo responsabilidad de la
 * propia barra y no hay que sincronizar nada con el ActionBar.
 */
fun AppCompatActivity.configurarToolbar(
    toolbar: MaterialToolbar,
    titulo: String? = null,
    subtitulo: String? = null
) {
    titulo?.let { toolbar.title = it }
    subtitulo?.let { toolbar.subtitle = it }
    toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
}
