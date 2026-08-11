package com.example.shieldrive.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.shieldrive.model.Reserva

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.shieldrive.NotificacionHelper

class ReservasUsuarioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null


    private val estadosPrevios = mutableMapOf<String, String>()

    var listaMisReservas by mutableStateOf<List<Reserva>>(emptyList())
        private set

    init {
        auth.addAuthStateListener {
            obtenerMisReservas()
        }
    }

    fun guardarReserva(reserva: Reserva, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        val reservaConUser = reserva.copy(userId = userId)

        db.collection("Reservas").document(reserva.id)
            .set(reservaConUser)
            .addOnSuccessListener {
                onSuccess()
            }
    }

    fun obtenerMisReservas() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            listaMisReservas = emptyList()
            estadosPrevios.clear()
            listener?.remove()
            return
        }

        listener?.remove()

        listener = db.collection("Reservas")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {


                    for (dc in snapshot.documentChanges) {
                        val reserva = dc.document.toObject(Reserva::class.java)
                        val estadoActual = reserva.estado

                        if (dc.type == DocumentChange.Type.MODIFIED) {
                            val estadoViejo = estadosPrevios[reserva.id]

                            // Si antes no estaba "Confirmada" y ahora sí, ¡DISPARAMOS NOTIFICACIÓN!
                            // Ojo: Asegúrate de que el texto "Confirmada" sea exactamente el que usas cuando el Admin acepta.
                            if (estadoViejo != "Confirmada" && estadoActual == "Confirmada") {
                                NotificacionHelper.mostrarNotificacion(
                                    context = getApplication<Application>().applicationContext,
                                    titulo = "¡Reserva Aprobada! 🚗💨",
                                    mensaje = "Tu ${reserva.vehiculoInfo} está listo. Revisa tu ticket en la app."
                                )
                            }
                        }

                        estadosPrevios[reserva.id] = estadoActual
                    }

                    // Actualizamos la UI normal
                    val reservas = snapshot.documents.mapNotNull { it.toObject(Reserva::class.java) }
                    listaMisReservas = reservas.sortedByDescending { it.timestamp }
                }
            }
    }

    fun calificarReserva(reserva: Reserva, rating: Float, comentario: String) {
        db.collection("Reservas").document(reserva.id)
            .update("calificada", true)
            .addOnSuccessListener { obtenerMisReservas() }
    }

    fun calificarReserva(reserva: Reserva, rating: Int, comentario: String) {
        db.collection("Reservas").document(reserva.id)
            .update("calificada", true)
            .addOnSuccessListener { obtenerMisReservas() }
    }
}