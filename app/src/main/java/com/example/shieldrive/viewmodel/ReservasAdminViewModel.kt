package com.example.shieldrive.viewmodel

import androidx.lifecycle.ViewModel
import com.example.shieldrive.model.Reserva
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReservasAdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _listaReservas = MutableStateFlow<List<Reserva>>(emptyList())
    val listaReservas: StateFlow<List<Reserva>> = _listaReservas

    init {
        obtenerTodasLasReservas()
    }

    private fun obtenerTodasLasReservas() {
        db.collection("Reservas").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val reservas = snapshot.documents.mapNotNull { it.toObject(Reserva::class.java) }
                _listaReservas.value = reservas.sortedByDescending { it.timestamp }
            }
        }
    }

    fun procesarReserva(reservaId: String, nuevoEstadoReserva: String, vehiculoId: String, nuevoEstadoVehiculo: String) {

        db.collection("Reservas").document(reservaId).update("estado", nuevoEstadoReserva)


        db.collection("flota").document(vehiculoId).update("estado", nuevoEstadoVehiculo)
    }
}