package com.example.shieldrive.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector


sealed class RutaAdminMenu(val ruta: String, val titulo: String, val icono: ImageVector) {
    object Dashboard : RutaAdminMenu("admin_dashboard", "Dashboard", Icons.Filled.Dashboard)
    object Flota : RutaAdminMenu("admin_flota", "Flota", Icons.Filled.DirectionsCar)
    object Reservas : RutaAdminMenu("admin_reservas", "Reservas", Icons.Filled.DateRange)
    object Configuracion : RutaAdminMenu("admin_configuracion", "Ajustes", Icons.Filled.Settings)
    

    object AgregarVehiculo : RutaAdminMenu("admin_agregar_vehiculo", "Nuevo Vehículo", Icons.Filled.DirectionsCar)
}