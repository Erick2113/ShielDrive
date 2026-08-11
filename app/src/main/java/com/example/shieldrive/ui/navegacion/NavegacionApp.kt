package com.example.shieldrive.ui.navegacion

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.shieldrive.model.Reserva
import com.example.shieldrive.ui.PantallaLogin
import com.example.shieldrive.ui.PantallaRegistro
import com.example.shieldrive.ui.VideoSplashScreen
import com.example.shieldrive.ui.PantallaRecuperarContrasena
import com.example.shieldrive.ui.pantallas.PantallaFavoritos
import com.example.shieldrive.ui.pantallas.PantallaInicio
import com.example.shieldrive.ui.pantallas.PantallaPerfil
import com.example.shieldrive.ui.pantallas.PantallaReservas
import com.example.shieldrive.ui.pantallas.PantallaDetalleVehiculo
import com.example.shieldrive.ui.pantallas.PantallaProcesoReserva
import com.example.shieldrive.ui.pantallas.PantallaNotificaciones
import com.example.shieldrive.ui.pantallas.admin.PantallaAdminDashboard
import com.example.shieldrive.ui.pantallas.admin.PantallaAdminFlota
import com.example.shieldrive.ui.pantallas.admin.PantallaAdminReservas
import com.example.shieldrive.ui.pantallas.admin.PantallaAdminConfiguracion
import com.example.shieldrive.ui.pantallas.admin.PantallaAgregarVehiculo
import com.example.shieldrive.ui.pantallas.admin.PantallaEscanerQR
import com.example.shieldrive.ui.pantallas.admin.PantallaDetalleReservaQR // <-- NUEVO
import com.example.shieldrive.ui.pantallas.admin.PantallaSeleccionVehiculoBitacora // <-- NUEVO
import com.example.shieldrive.ui.pantallas.admin.PantallaBitacoraEspecifica // <-- NUEVO
import com.example.shieldrive.viewmodel.FlotaViewModel
import com.example.shieldrive.viewmodel.FavoritosViewModel
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import com.example.shieldrive.viewmodel.ReservasUsuarioViewModel

@Composable
fun ShielDriveApp() {
    val navController = rememberNavController()
    val flotaViewModelGlobal: FlotaViewModel = viewModel()
    val favoritosViewModelGlobal: FavoritosViewModel = viewModel()
    val resenasViewModelGlobal: ResenasGlobalViewModel = viewModel()
    val reservasViewModelGlobal: ReservasUsuarioViewModel = viewModel()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            VideoSplashScreen(onVideoFinished = { navController.navigate(Ruta.Login.ruta) { popUpTo("splash") { inclusive = true } } })
        }
        composable(Ruta.Login.ruta) {
            PantallaLogin(
                onLoginExitoso = { esAdmin ->
                    if (esAdmin) navController.navigate("admin_principal") { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                    else navController.navigate(Ruta.Principal.ruta) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                },
                onIrARegistro = { navController.navigate(Ruta.Registro.ruta) },
                onIrARecuperar = { navController.navigate("recuperar") }
            )
        }
        composable(Ruta.Registro.ruta) {
            PantallaRegistro(
                onRegistroExitoso = { esAdmin ->
                    if (esAdmin) navController.navigate("admin_principal") { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                    else navController.navigate(Ruta.Principal.ruta) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                },
                onIrALogin = { navController.popBackStack() }
            )
        }
        composable("recuperar") {
            PantallaRecuperarContrasena(
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Ruta.Principal.ruta) {
            PantallaPrincipalContenedor(
                rootNavController = navController,
                flotaViewModel = flotaViewModelGlobal,
                favoritosViewModel = favoritosViewModelGlobal,
                resenasViewModel = resenasViewModelGlobal,
                reservasViewModel = reservasViewModelGlobal
            )
        }
        composable("detalle_vehiculo/{vehiculoId}") { backStackEntry ->
            val vehiculoId = backStackEntry.arguments?.getString("vehiculoId")
            val vehiculoSeleccionado = flotaViewModelGlobal.listaVehiculos.find { it.id == vehiculoId }

            if (vehiculoSeleccionado != null) {
                PantallaDetalleVehiculo(
                    vehiculo = vehiculoSeleccionado,
                    resenasViewModel = resenasViewModelGlobal,
                    onVolver = { navController.popBackStack() },
                    onReservarClick = { vehiculoClick -> navController.navigate(Ruta.ProcesoReserva.crearRuta(vehiculoClick.id)) }
                )
            }
        }
        composable("proceso_reserva/{vehiculoId}") { backStackEntry ->
            val vehiculoId = backStackEntry.arguments?.getString("vehiculoId")
            val vehiculoSeleccionado = flotaViewModelGlobal.listaVehiculos.find { it.id == vehiculoId }
            val contexto = LocalContext.current

            if (vehiculoSeleccionado != null) {
                PantallaProcesoReserva(
                    vehiculo = vehiculoSeleccionado,
                    onVolver = { navController.popBackStack() },
                    onConfirmar = { nombre, telefono, documento, fechaInicio, fechaFin, idReserva, total ->
                        val nuevaReserva = Reserva(
                            id = idReserva,
                            vehiculoId = vehiculoSeleccionado.id,
                            vehiculoInfo = "${vehiculoSeleccionado.marca} ${vehiculoSeleccionado.modelo}",
                            clienteNombre = nombre,
                            clienteTelefono = telefono,
                            documento = documento,
                            fechaInicio = fechaInicio,
                            fechaFin = fechaFin,
                            totalMonto = total,
                            estado = "En proceso de autorización",
                            calificada = false
                        )

                        reservasViewModelGlobal.guardarReserva(nuevaReserva) {
                            Toast.makeText(contexto, "¡Reserva guardada con éxito!", Toast.LENGTH_LONG).show()
                            navController.navigate(Ruta.Principal.ruta) { popUpTo(Ruta.Principal.ruta) { inclusive = true } }
                        }
                    },
                    onCambiarEstadoVehiculo = { nuevoEstado ->
                        flotaViewModelGlobal.actualizarEstadoVehiculo(vehiculoSeleccionado.id, nuevoEstado)
                    }
                )
            }
        }
        composable(Ruta.Notificaciones.ruta) { PantallaNotificaciones(onVolver = { navController.popBackStack() }) }

        composable("admin_principal") {
            PantallaAdminContenedor(
                onAgregarClick = { navController.navigate(RutaAdminMenu.AgregarVehiculo.ruta) },
                flotaViewModel = flotaViewModelGlobal // <-- SE LO PASAMOS AL ADMIN AHORA
            )
        }
        composable(RutaAdminMenu.AgregarVehiculo.ruta) { PantallaAgregarVehiculo(onVolver = { navController.popBackStack() }) }
    }
}

@Composable
fun PantallaPrincipalContenedor(
    rootNavController: NavHostController,
    flotaViewModel: FlotaViewModel,
    favoritosViewModel: FavoritosViewModel,
    resenasViewModel: ResenasGlobalViewModel,
    reservasViewModel: ReservasUsuarioViewModel
) {
    val tabNavController = rememberNavController()
    val itemsMenu = listOf(RutaMenu.Inicio, RutaMenu.Reservas, RutaMenu.Favoritos, RutaMenu.Perfil)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, contentColor = Color(0xFF2970FF)) {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val rutaActual = navBackStackEntry?.destination?.route

                itemsMenu.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icono, contentDescription = item.titulo) },
                        label = { Text(item.titulo) },
                        selected = rutaActual == item.ruta,
                        onClick = { tabNavController.navigate(item.ruta) { popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2970FF), selectedTextColor = Color(0xFF2970FF), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFFE0E7FF))
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(navController = tabNavController, startDestination = RutaMenu.Inicio.ruta, modifier = Modifier.padding(paddingValues)) {
            composable(RutaMenu.Inicio.ruta) {
                PantallaInicio(
                    rootNavController,
                    flotaViewModel,
                    reservasViewModel,
                    favoritosViewModel,
                    resenasViewModel
                )
            }
            composable(RutaMenu.Reservas.ruta) {
                PantallaReservas(reservasViewModel = reservasViewModel)
            }
            composable(RutaMenu.Favoritos.ruta) {
                PantallaFavoritos(
                    navController = rootNavController,
                    favoritosViewModel = favoritosViewModel,
                    flotaViewModel = flotaViewModel
                )
            }
            composable(RutaMenu.Perfil.ruta) {
                PantallaPerfil(
                    resenasViewModel = resenasViewModel,
                    onLogout = { rootNavController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                )
            }
        }
    }
}

@Composable
fun PantallaAdminContenedor(onAgregarClick: () -> Unit, flotaViewModel: FlotaViewModel) {
    val tabNavController = rememberNavController()
    val itemsMenuAdmin = listOf(RutaAdminMenu.Dashboard, RutaAdminMenu.Flota, RutaAdminMenu.Reservas, RutaAdminMenu.Configuracion)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E293B), contentColor = Color.White) {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val rutaActual = navBackStackEntry?.destination?.route

                itemsMenuAdmin.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icono, contentDescription = item.titulo) },
                        label = { Text(item.titulo) },
                        selected = rutaActual == item.ruta,
                        onClick = { tabNavController.navigate(item.ruta) { popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = Color.White, unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF334155))
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(navController = tabNavController, startDestination = RutaAdminMenu.Dashboard.ruta, modifier = Modifier.padding(paddingValues)) {
            composable(RutaAdminMenu.Dashboard.ruta) { PantallaAdminDashboard() }


            composable(RutaAdminMenu.Flota.ruta) {
                PantallaAdminFlota(
                    onAgregarVehiculo = onAgregarClick,
                    onAbrirEscaner = { tabNavController.navigate("escaner_qr") },
                    onAbrirHistorialVehiculos = { tabNavController.navigate("seleccion_historial") } // <-- NUEVO
                )
            }

            composable(RutaAdminMenu.Reservas.ruta) { PantallaAdminReservas() }
            composable(RutaAdminMenu.Configuracion.ruta) { PantallaAdminConfiguracion() }


            composable("escaner_qr") {
                PantallaEscanerQR(
                    onVolver = { tabNavController.popBackStack() },
                    onQrEscaneado = { idReserva ->
                        tabNavController.popBackStack() // Regresa a Flota al leerlo
                        tabNavController.navigate("detalle_qr/$idReserva") // <-- SALTA A LOS DETALLES
                    }
                )
            }


            composable("detalle_qr/{idReserva}") { backStackEntry ->
                val idReserva = backStackEntry.arguments?.getString("idReserva") ?: ""
                PantallaDetalleReservaQR(
                    idReserva = idReserva,
                    onVolver = { tabNavController.popBackStack() },
                    onFinalizado = { tabNavController.popBackStack() }
                )
            }


            composable("seleccion_historial") {
                PantallaSeleccionVehiculoBitacora(
                    listaVehiculos = flotaViewModel.listaVehiculos,
                    onVolver = { tabNavController.popBackStack() },
                    onVehiculoSeleccionado = { vehiculo ->
                        tabNavController.navigate("expediente_vehiculo/${vehiculo.id}/${vehiculo.marca} ${vehiculo.modelo}")
                    }
                )
            }

            composable("expediente_vehiculo/{vehiculoId}/{nombreVehiculo}") { backStackEntry ->
                val vehiculoId = backStackEntry.arguments?.getString("vehiculoId") ?: ""
                val nombreVehiculo = backStackEntry.arguments?.getString("nombreVehiculo") ?: "Expediente"

                PantallaBitacoraEspecifica(
                    vehiculoId = vehiculoId,
                    nombreVehiculo = nombreVehiculo,
                    onVolver = { tabNavController.popBackStack() }
                )
            }
        }
    }
}