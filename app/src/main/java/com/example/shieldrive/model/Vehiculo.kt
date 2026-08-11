package com.example.shieldrive.model

data class Vehiculo(
    val id: String = "",
    val vin: String = "",
    val marca: String = "",
    val modelo: String = "",
    val anio: String = "",
    val precio: String = "",
    val descripcion: String = "",
    val urlImagen: String = "",
    val estado: String = "Disponible",


    val asientos: String = "",
    val transmision: String = "",
    val motor: String = "",

    val rating: Double = 0.0,
    val numResenas: Int = 0,


    val listaResenas: List<Resena> = emptyList()
)