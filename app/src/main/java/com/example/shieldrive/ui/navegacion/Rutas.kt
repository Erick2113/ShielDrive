package com.example.shieldrive.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Ruta(val ruta: String) {
    object Login : Ruta("login")
    object Registro : Ruta("registro")
    object Principal : Ruta("principal") // Contenedor de las pestañas


    object DetalleVehiculo : Ruta("detalle_vehiculo/{vehiculoId}") {
        fun crearRuta(vehiculoId: String) = "detalle_vehiculo/$vehiculoId"
    }


    object ProcesoReserva : Ruta("proceso_reserva/{vehiculoId}") {
        fun crearRuta(vehiculoId: String) = "proceso_reserva/$vehiculoId"
    }
    object Notificaciones : Ruta("notificaciones")
}


sealed class RutaMenu(val ruta: String, val titulo: String, val icono: ImageVector) {
    object Inicio : RutaMenu("inicio", "Coches", Icons.Rounded.DirectionsCar)
    object Reservas : RutaMenu("reservas", "Reservas", Icons.Rounded.DateRange)
    object Favoritos : RutaMenu("favoritos", "Favoritos", Icons.Rounded.Favorite)
    object Perfil : RutaMenu("perfil", "Perfil", Icons.Rounded.Person)

}