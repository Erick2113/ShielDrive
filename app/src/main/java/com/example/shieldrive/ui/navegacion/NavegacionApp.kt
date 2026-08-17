package com.example.shieldrive.ui.navegacion

import android.widget.Toast
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.example.shieldrive.ui.pantallas.admin.PantallaDetalleReservaQR
import com.example.shieldrive.ui.pantallas.admin.PantallaSeleccionVehiculoBitacora
import com.example.shieldrive.ui.pantallas.admin.PantallaBitacoraEspecifica
import com.example.shieldrive.viewmodel.FlotaViewModel
import com.example.shieldrive.viewmodel.FavoritosViewModel
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import com.example.shieldrive.viewmodel.ReservasUsuarioViewModel
import com.google.firebase.firestore.FirebaseFirestore

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
                rootNavController = navController,
                onAgregarClick = { navController.navigate(RutaAdminMenu.AgregarVehiculo.ruta) },
                flotaViewModel = flotaViewModelGlobal
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


    val fondoDegradado = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F2FE),
            Color(0xFFFFFFFF)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().background(fondoDegradado),
        containerColor = Color.Transparent, // Hacemos el Scaffold transparente
        bottomBar = {
            NavigationBar(containerColor = DarkSurface, contentColor = Color.White) {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val rutaActual = navBackStackEntry?.destination?.route

                itemsMenu.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icono, contentDescription = item.titulo) },
                        label = { Text(item.titulo) },
                        selected = rutaActual == item.ruta,
                        onClick = { tabNavController.navigate(item.ruta) { popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = PrimaryDark
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(navController = tabNavController, startDestination = RutaMenu.Inicio.ruta, modifier = Modifier.padding(paddingValues)) {
            composable(RutaMenu.Inicio.ruta) {

                PantallaInicio(
                    navController = rootNavController,
                    flotaViewModel = flotaViewModel,
                    reservasViewModel = reservasViewModel,
                    favoritosViewModel = favoritosViewModel,
                    resenasViewModel = resenasViewModel
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
                    reservasViewModel = reservasViewModel,
                    onLogout = { rootNavController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdminContenedor(
    rootNavController: NavHostController,
    onAgregarClick: () -> Unit,
    flotaViewModel: FlotaViewModel
) {
    val tabNavController = rememberNavController()
    val itemsMenuAdmin = listOf(RutaAdminMenu.Dashboard, RutaAdminMenu.Flota, RutaAdminMenu.Reservas, RutaAdminMenu.Configuracion)

    val db = FirebaseFirestore.getInstance()
    var reservasPendientes by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.collection("Reservas")
            .whereEqualTo("estado", "En proceso de autorización")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    reservasPendientes = snapshot.documents.count { doc ->
                        doc.getBoolean("ocultaAdmin") != true
                    }
                }
            }
    }


    val fondoDegradado = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F2FE),
            Color(0xFFFFFFFF)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().background(fondoDegradado),
        containerColor = Color.Transparent, // Hacemos el Scaffold transparente
        bottomBar = {
            NavigationBar(containerColor = DarkSurface, contentColor = Color.White) {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val rutaActual = navBackStackEntry?.destination?.route

                itemsMenuAdmin.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            if (item.ruta == RutaAdminMenu.Reservas.ruta && reservasPendientes > 0) {
                                BadgedBox(

                                    badge = { Badge(containerColor = ErrorColor, contentColor = Color.White) { Text(reservasPendientes.toString()) } }
                                ) {
                                    Icon(item.icono, contentDescription = item.titulo)
                                }
                            } else {
                                Icon(item.icono, contentDescription = item.titulo)
                            }
                        },
                        label = { Text(item.titulo) },
                        selected = rutaActual == item.ruta,
                        onClick = { tabNavController.navigate(item.ruta) { popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = PrimaryDark
                        )
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
                    onAbrirHistorialVehiculos = { tabNavController.navigate("seleccion_historial") },
                    flotaViewModel = flotaViewModel
                )
            }

            composable(RutaAdminMenu.Reservas.ruta) { PantallaAdminReservas() }

            composable(RutaAdminMenu.Configuracion.ruta) {
                PantallaAdminConfiguracion(
                    onLogout = {
                        rootNavController.navigate(Ruta.Login.ruta) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("escaner_qr") {
                PantallaEscanerQR(
                    onVolver = { tabNavController.popBackStack() },
                    onQrEscaneado = { idReserva ->
                        tabNavController.popBackStack()
                        tabNavController.navigate("detalle_qr/$idReserva")
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