package com.example.shieldrive.model

data class Reserva(
    val id: String = "",
    val userId: String = "",
    val vehiculoId: String = "",
    val vehiculoInfo: String = "",
    val clienteNombre: String = "",
    val clienteTelefono: String = "",
    val documento: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val totalMonto: String = "",
    val estado: String = "Activa",
    val calificada: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)