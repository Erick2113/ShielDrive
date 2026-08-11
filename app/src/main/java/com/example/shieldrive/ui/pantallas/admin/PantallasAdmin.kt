package com.example.shieldrive.ui.pantallas.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History // <-- Importación para el historial
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.viewmodel.FlotaViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image


@Composable
fun PantallaAdminDashboard() {
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)).padding(16.dp)) {
        item {
            Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 4.dp, top = 8.dp))
            Text("Resumen de hoy", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                TarjetaEstadistica("Ingresos (Mes)", "$1,250", Icons.Filled.AttachMoney, Color(0xFF10B981), Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                TarjetaEstadistica("Rentados", "12", Icons.Filled.DirectionsCar, Color(0xFF2970FF), Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                TarjetaEstadistica("Disponibles", "8", Icons.Filled.Key, Color(0xFFF59E0B), Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                TarjetaEstadistica("Pendientes", "3", Icons.Filled.PendingActions, Color(0xFFEF4444), Modifier.weight(1f))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdminFlota(
    onAgregarVehiculo: () -> Unit,
    onAbrirEscaner: () -> Unit = {},
    onAbrirHistorialVehiculos: () -> Unit = {}, // <-- NUEVO PARÁMETRO PARA LA BITÁCORA
    flotaViewModel: FlotaViewModel = viewModel()
) {
    val listaVehiculos = flotaViewModel.listaVehiculos
    var vehiculoAEditar: Vehiculo? by remember { mutableStateOf(null) }
    val sheetStateEditar = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            // Columna para poner los tres botones flotantes uno encima del otro
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BOTÓN DE ESCÁNER
                FloatingActionButton(
                    onClick = onAbrirEscaner,
                    containerColor = Color(0xFF1E293B), // Oscuro premium
                    contentColor = Color.White
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear QR")
                }

                // BOTÓN DE HISTORIAL DE VEHÍCULOS (NUEVO)
                FloatingActionButton(
                    onClick = onAbrirHistorialVehiculos,
                    containerColor = Color(0xFF8B5CF6), // Morado para diferenciarlo
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.History, contentDescription = "Historial Vehículos")
                }

                // BOTÓN DE AGREGAR VEHÍCULO
                FloatingActionButton(
                    onClick = onAgregarVehiculo,
                    containerColor = Color(0xFF2970FF),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar Vehículo")
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text("Gestión de Flota", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 16.dp))

            if (listaVehiculos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay vehículos registrados.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(listaVehiculos) { vehiculo ->
                        ItemVehiculoGrid(vehiculo = vehiculo, onClick = { vehiculoAEditar = vehiculo })
                    }
                }
            }
        }
    }

    vehiculoAEditar?.let { vehiculo ->
        ModalBottomSheet(onDismissRequest = { vehiculoAEditar = null }, sheetState = sheetStateEditar, containerColor = Color.White) {
            PanelGestionVehiculo(vehiculo = vehiculo, flotaViewModel = flotaViewModel, onCerrar = { vehiculoAEditar = null })
        }
    }
}

@Composable
fun PanelGestionVehiculo(vehiculo: Vehiculo, flotaViewModel: FlotaViewModel, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var descripcion by remember { mutableStateOf(vehiculo.descripcion) }
    var estadoSeleccionado by remember { mutableStateOf(vehiculo.estado) }
    var guardando by remember { mutableStateOf(false) }
    val estadosPermitidos = listOf("Disponible", "En Mantenimiento", "Dado de baja")

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        Text("Gestionar ${vehiculo.marca} ${vehiculo.modelo}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 16.dp))
        Text("Estado Operativo:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            estadosPermitidos.forEach { estado ->
                Row(modifier = Modifier.fillMaxWidth().clickable { estadoSeleccionado = estado }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = (estado == estadoSeleccionado), onClick = { estadoSeleccionado = estado }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2970FF)))
                    Text(estado, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción o notas") }, modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 24.dp), maxLines = 4)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = Color.Gray) }
            Button(
                onClick = {
                    guardando = true
                    flotaViewModel.guardarVehiculoEnFirebase(context, vehiculo.copy(descripcion = descripcion, estado = estadoSeleccionado)) {
                        guardando = false
                        onCerrar()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                enabled = !guardando
            ) { Text(if (guardando) "Actualizando..." else "Guardar Cambios") }
        }
    }
}


@Composable
fun PantallaAdminConfiguracion() {
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Text("Configuración", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Crear Admin", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2970FF), modifier = Modifier.padding(bottom = 16.dp))
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = correo, onValueChange = { correo = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Registrar") }
                }
            }
        }
    }
}


@Composable
fun TarjetaEstadistica(titulo: String, valor: String, icono: ImageVector, colorIcono: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = titulo, color = Color.Gray, fontSize = 14.sp)
                Icon(imageVector = icono, contentDescription = null, tint = colorIcono)
            }
            Text(text = valor, color = Color(0xFF1E293B), fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ItemVehiculoGrid(vehiculo: Vehiculo, onClick: () -> Unit) {
    val colorEstado = when (vehiculo.estado) {
        "Disponible" -> Color(0xFF10B981)
        "En Mantenimiento" -> Color(0xFFF59E0B)
        "Dado de baja" -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    var decodedBitmap: android.graphics.Bitmap? = null
    if (vehiculo.urlImagen.isNotEmpty() && !vehiculo.urlImagen.startsWith("http")) {
        try {
            val imageBytes = Base64.decode(vehiculo.urlImagen, Base64.DEFAULT)
            decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) { e.printStackTrace() }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(230.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (vehiculo.urlImagen.startsWith("http")) {
                AsyncImage(model = vehiculo.urlImagen, contentDescription = null, modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Crop)
            } else if (decodedBitmap != null) {
                Image(bitmap = decodedBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = vehiculo.anio, color = Color.Gray, fontSize = 13.sp)
                Text(text = vehiculo.precio, color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(shape = RoundedCornerShape(8.dp), color = colorEstado.copy(alpha = 0.15f), contentColor = colorEstado, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = vehiculo.estado, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}