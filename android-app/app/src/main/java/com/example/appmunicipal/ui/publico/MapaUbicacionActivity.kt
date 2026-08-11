package com.example.appmunicipal.ui.publico

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.appmunicipal.R
import com.example.appmunicipal.databinding.ActivityMapaUbicacionBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale
import com.example.appmunicipal.util.applySafeSystemInsets

class MapaUbicacionActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerDragListener {

    private lateinit var binding: ActivityMapaUbicacionBinding
    private lateinit var mapa: GoogleMap
    private var marcador: Marker? = null
    private var ubicacionSeleccionada: LatLng? = null
    private var direccionSeleccionada: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaUbicacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySafeSystemInsets()

        binding.buttonCancelarMapa.setOnClickListener { finish() }
        binding.buttonConfirmarUbicacion.setOnClickListener { confirmarUbicacion() }

        val fragment = supportFragmentManager.findFragmentById(R.id.mapContainer)
            as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().replace(R.id.mapContainer, it).commit()
            }
        fragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        mapa.uiSettings.isZoomControlsEnabled = true
        mapa.uiSettings.isMapToolbarEnabled = false
        mapa.setOnMapClickListener { seleccionarPunto(it, false) }
        mapa.setOnMarkerDragListener(this)

        val lat = intent.getDoubleExtra(EXTRA_LATITUD, Double.NaN)
        val lon = intent.getDoubleExtra(EXTRA_LONGITUD, Double.NaN)
        val direccion = intent.getStringExtra(EXTRA_DIRECCION).orEmpty()
        if (lat.isFinite() && lon.isFinite()) {
            direccionSeleccionada = direccion
            seleccionarPunto(LatLng(lat, lon), true, direccion.isBlank())
        } else {
            mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(CENTRO_EL_PROGRESO, 13f))
        }
    }

    private fun seleccionarPunto(punto: LatLng, moverCamara: Boolean, buscarDireccion: Boolean = true) {
        ubicacionSeleccionada = punto
        if (marcador == null) {
            marcador = mapa.addMarker(
                MarkerOptions().position(punto).title(getString(R.string.ubicacion_reporte)).draggable(true)
            )
        } else marcador?.position = punto

        if (moverCamara) mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(punto, 17f))
        binding.buttonConfirmarUbicacion.isEnabled = true
        actualizarCoordenadas(punto)
        if (buscarDireccion) {
            direccionSeleccionada = ""
            obtenerDireccion(punto)
        } else mostrarDireccion(direccionSeleccionada.ifBlank { direccionPorCoordenadas(punto) })
    }

    private fun actualizarCoordenadas(punto: LatLng) {
        binding.textCoordenadasMapa.text = getString(
            R.string.formato_coordenadas, punto.latitude, punto.longitude
        )
    }

    private fun obtenerDireccion(punto: LatLng) {
        mostrarDireccion(getString(R.string.buscando_direccion))
        val geocoder = Geocoder(this, Locale("es", "HN"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(punto.latitude, punto.longitude, 1) { addresses ->
                runOnUiThread { aplicarDireccion(punto, addresses.firstOrNull()?.getAddressLine(0)) }
            }
        } else {
            Thread {
                @Suppress("DEPRECATION")
                val direccion = runCatching {
                    geocoder.getFromLocation(punto.latitude, punto.longitude, 1)
                        ?.firstOrNull()?.getAddressLine(0)
                }.getOrNull()
                runOnUiThread { aplicarDireccion(punto, direccion) }
            }.start()
        }
    }

    private fun aplicarDireccion(punto: LatLng, direccion: String?) {
        if (isFinishing || isDestroyed || ubicacionSeleccionada != punto) return
        direccionSeleccionada = direccion?.trim().orEmpty().ifBlank { direccionPorCoordenadas(punto) }
        mostrarDireccion(direccionSeleccionada)
    }

    private fun mostrarDireccion(value: String) { binding.textDireccionMapa.text = value }

    private fun direccionPorCoordenadas(punto: LatLng) = getString(
        R.string.direccion_coordenadas, punto.latitude, punto.longitude
    )

    private fun confirmarUbicacion() {
        val punto = ubicacionSeleccionada ?: return
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_DIRECCION, direccionSeleccionada.ifBlank { direccionPorCoordenadas(punto) })
            putExtra(EXTRA_LATITUD, punto.latitude)
            putExtra(EXTRA_LONGITUD, punto.longitude)
        })
        finish()
    }

    override fun onMarkerDragStart(marker: Marker) = Unit
    override fun onMarkerDrag(marker: Marker) {
        ubicacionSeleccionada = marker.position
        actualizarCoordenadas(marker.position)
    }
    override fun onMarkerDragEnd(marker: Marker) { seleccionarPunto(marker.position, false) }

    companion object {
        const val EXTRA_DIRECCION = "extra_direccion"
        const val EXTRA_LATITUD = "extra_latitud"
        const val EXTRA_LONGITUD = "extra_longitud"
        private val CENTRO_EL_PROGRESO = LatLng(15.4009, -87.8000)
    }
}
