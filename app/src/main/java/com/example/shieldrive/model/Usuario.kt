package com.example.shieldrive.model

data class Usuario(
    val uid: String = "",
    val correo: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val rol: String = "cliente"
)