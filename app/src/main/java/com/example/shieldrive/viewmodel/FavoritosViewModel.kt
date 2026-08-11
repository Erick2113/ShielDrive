package com.example.shieldrive.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.example.shieldrive.model.Vehiculo
import com.google.firebase.auth.FirebaseAuth


class FavoritosViewModel(application: Application) : AndroidViewModel(application) {


    private val prefs = application.getSharedPreferences("shieldrive_favoritos", Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()

    // Lista reactiva para la UI que guarda los IDs
    private val _favoritosIds = mutableStateListOf<String>()

    init {

        auth.addAuthStateListener {
            cargarFavoritosDelUsuario()
        }
    }

    private fun cargarFavoritosDelUsuario() {
        _favoritosIds.clear()

        val userId = auth.currentUser?.uid ?: "invitado"


        val guardados = prefs.getStringSet("favoritos_$userId", emptySet()) ?: emptySet()
        _favoritosIds.addAll(guardados)
    }

    fun toggleFavorito(vehiculo: Vehiculo) {
        val userId = auth.currentUser?.uid ?: "invitado"

        if (_favoritosIds.contains(vehiculo.id)) {
            _favoritosIds.remove(vehiculo.id)
        } else {
            _favoritosIds.add(vehiculo.id)
        }


        prefs.edit().putStringSet("favoritos_$userId", _favoritosIds.toSet()).apply()
    }

    fun esFavorito(vehiculoId: String): Boolean {
        return _favoritosIds.contains(vehiculoId)
    }


    fun getVehiculosFavoritos(flotaCompleta: List<Vehiculo>): List<Vehiculo> {
        return flotaCompleta.filter { _favoritosIds.contains(it.id) }
    }
}