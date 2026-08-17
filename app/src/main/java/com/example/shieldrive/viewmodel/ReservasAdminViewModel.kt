package com.example.shieldrive.viewmodel

import androidx.lifecycle.ViewModel
import com.example.shieldrive.model.Reserva
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

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


        db.collection("Reservas").document(reservaId).get().addOnSuccessListener { doc ->

            val usuarioId = doc.getString("userId") ?: doc.getString("usuarioId") ?: ""
            val vehiculoInfo = doc.getString("vehiculoInfo") ?: "el vehículo"

            if (usuarioId.isNotEmpty()) {
                val tituloNotif = if (nuevoEstadoReserva == "Confirmada") "¡Reserva Aprobada! " else "Reserva Rechazada "
                val mensajeNotif = if (nuevoEstadoReserva == "Confirmada") {
                    "Tu reserva para $vehiculoInfo ha sido confirmada. ¡Prepárate para tu viaje!"
                } else {
                    "Lo sentimos, tu reserva para $vehiculoInfo ha sido rechazada."
                }

                val notificacionMap = mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "usuarioId" to usuarioId,
                    "titulo" to tituloNotif,
                    "mensaje" to mensajeNotif,
                    "fecha" to System.currentTimeMillis(),
                    "leida" to false
                )


                db.collection("notificaciones").document(notificacionMap["id"] as String).set(notificacionMap)
            }
        }
    }
}