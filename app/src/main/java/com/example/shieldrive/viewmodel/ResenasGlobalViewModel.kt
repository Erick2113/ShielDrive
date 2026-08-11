package com.example.shieldrive.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.shieldrive.model.Resena
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class ResenasGlobalViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _todasLasResenas = mutableStateListOf<Resena>()
    val todasLasResenas: List<Resena> get() = _todasLasResenas

    init {
        db.collection("resenas").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null) {
                _todasLasResenas.clear()
                for (document in snapshot.documents) {
                    val resena = document.toObject(Resena::class.java)
                    if (resena != null) {
                        _todasLasResenas.add(resena)
                    }
                }
            }
        }
    }

    fun agregarResena(vehiculoId: String, vehiculoNombre: String, estrellas: Int, comentario: String) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: return


        db.collection("usuarios").document(uid).get().addOnSuccessListener { document ->


            val nombreAutor = if (document != null && document.exists()) {
                document.getString("nombre") ?: currentUser.displayName.takeIf { !it.isNullOrBlank() } ?: "Usuario ShielDrive"
            } else {
                currentUser.displayName.takeIf { !it.isNullOrBlank() } ?: "Usuario ShielDrive"
            }

            val nuevaResena = Resena(
                id = UUID.randomUUID().toString(),
                vehiculoId = vehiculoId,
                vehiculoNombre = vehiculoNombre,
                usuarioId = uid,
                autor = nombreAutor,
                comentario = comentario,
                estrellas = estrellas
            )


            _todasLasResenas.add(nuevaResena)

            db.collection("resenas").document(nuevaResena.id).set(nuevaResena)

        }.addOnFailureListener {

            val nombreRespaldo = currentUser.displayName.takeIf { !it.isNullOrBlank() } ?: "Usuario ShielDrive"
            val nuevaResena = Resena(
                id = UUID.randomUUID().toString(),
                vehiculoId = vehiculoId,
                vehiculoNombre = vehiculoNombre,
                usuarioId = uid,
                autor = nombreRespaldo,
                comentario = comentario,
                estrellas = estrellas
            )
            _todasLasResenas.add(nuevaResena)
            db.collection("resenas").document(nuevaResena.id).set(nuevaResena)
        }
    }

    fun eliminarResena(resenaId: String) {
        _todasLasResenas.removeAll { it.id == resenaId }
        db.collection("resenas").document(resenaId).delete()
    }

    fun obtenerResenasPorVehiculo(vehiculoId: String): List<Resena> {
        return _todasLasResenas.filter { it.vehiculoId == vehiculoId }
    }

    fun obtenerMisResenas(): List<Resena> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        return _todasLasResenas.filter { it.usuarioId == uid }
    }

    fun yaCalificoVehiculo(vehiculoId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return _todasLasResenas.any { it.vehiculoId == vehiculoId && it.usuarioId == uid }
    }
}