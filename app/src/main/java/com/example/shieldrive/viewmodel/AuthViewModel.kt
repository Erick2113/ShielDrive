package com.example.shieldrive.viewmodel

import androidx.lifecycle.ViewModel
import com.example.shieldrive.model.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun registrarUsuario(
        correo: String,
        contrasena: String,
        nombre: String,
        telefono: String,
        onExito: (esAdmin: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        _cargando.value = true

        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Sincronizamos el nombre en Firebase Auth inmediatamente
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre.trim())
                            .build()

                        firebaseUser.updateProfile(profileUpdates)

                        val nuevoUsuario = Usuario(
                            uid = firebaseUser.uid,
                            correo = correo,
                            nombre = nombre,
                            telefono = telefono,
                            rol = "cliente"
                        )

                        db.collection("usuarios").document(firebaseUser.uid)
                            .set(nuevoUsuario)
                            .addOnSuccessListener {
                                _cargando.value = false
                                onExito(false)
                            }
                            .addOnFailureListener { error ->
                                _cargando.value = false
                                onError(error.message ?: "Error al guardar el perfil en Firestore")
                            }
                    }
                } else {
                    _cargando.value = false

                    val excepcion = task.exception
                    val mensajeAmigable = when (excepcion) {
                        is FirebaseAuthUserCollisionException -> "Este correo ya está registrado. Por favor, usa otro o inicia sesión."
                        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil. Usa al menos 6 caracteres."
                        is FirebaseAuthInvalidCredentialsException -> "El formato del correo no es válido."
                        else -> excepcion?.message ?: "Error al registrar en Firebase Auth"
                    }

                    onError(mensajeAmigable)
                }
            }
    }

    fun iniciarSesion(
        correo: String,
        contrasena: String,
        onExito: (esAdmin: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        _cargando.value = true

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        db.collection("usuarios").document(firebaseUser.uid).get()
                            .addOnSuccessListener { document ->
                                _cargando.value = false
                                if (document != null && document.exists()) {
                                    val rol = document.getString("rol") ?: "cliente"
                                    onExito(rol == "admin")
                                } else {
                                    onExito(false)
                                }
                            }
                            .addOnFailureListener { error ->
                                _cargando.value = false
                                onError(error.message ?: "Error al leer el perfil")
                            }
                    }
                } else {
                    _cargando.value = false
                    onError("Correo o contraseña incorrectos")
                }
            }
    }

    fun restablecerContrasena(
        correo: String,
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (correo.isBlank()) {
            onError("Ingresa tu correo electrónico arriba para enviarte el enlace.")
            return
        }

        _cargando.value = true

        auth.sendPasswordResetEmail(correo.trim())
            .addOnCompleteListener { task ->
                _cargando.value = false
                if (task.isSuccessful) {
                    onExito()
                } else {
                    val excepcion = task.exception
                    val mensajeAmigable = when (excepcion) {
                        is FirebaseAuthInvalidUserException -> "No existe ninguna cuenta registrada con este correo."
                        is FirebaseAuthInvalidCredentialsException -> "El formato del correo no es válido."
                        else -> excepcion?.message ?: "Error al enviar el correo de recuperación."
                    }
                    onError(mensajeAmigable)
                }
            }
    }
}