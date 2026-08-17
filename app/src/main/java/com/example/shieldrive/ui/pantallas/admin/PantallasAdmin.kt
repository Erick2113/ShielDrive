package com.example.shieldrive.ui.pantallas.admin

import android.Manifest
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.viewmodel.FlotaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@Composable
fun PantallaAdminDashboard(flotaViewModel: FlotaViewModel = viewModel()) {
    val db = FirebaseFirestore.getInstance()

    // Estados para guardar los números en tiempo real
    var ingresosTotales by remember { mutableDoubleStateOf(0.0) }
    var reservasPendientes by remember { mutableIntStateOf(0) }
    var autosRentados by remember { mutableIntStateOf(0) }


    val listaVehiculos = flotaViewModel.listaVehiculos
    val autosDisponibles = listaVehiculos.count { it.estado == "Disponible" }


    LaunchedEffect(Unit) {
        db.collection("Reservas").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                var ingresos = 0.0
                var pendientes = 0
                var rentados = 0

                for (doc in snapshot.documents) {
                    val estado = doc.getString("estado") ?: ""
                    val totalMontoStr = doc.getString("totalMonto") ?: "0"


                    if (estado == "En proceso de autorización") {
                        pendientes++
                    }


                    if (estado == "En uso") {
                        rentados++
                    }


                    if (estado == "Finalizada" || estado == "En uso") {
                        val montoNum = totalMontoStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                        ingresos += montoNum
                    }
                }


                ingresosTotales = ingresos
                reservasPendientes = pendientes
                autosRentados = rentados
            }
        }
    }


    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp)) {
        item {
            Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp))
            Text("Resumen en tiempo real", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 24.dp))
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {

                val ingresosFormateados = String.format(Locale.US, "$%,.2f", ingresosTotales)

                TarjetaEstadistica("Ingresos Totales", ingresosFormateados, Icons.Filled.AttachMoney, SuccessColor, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                TarjetaEstadistica("En Uso", autosRentados.toString(), Icons.Filled.DirectionsCar, Primary, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                TarjetaEstadistica("Disponibles", autosDisponibles.toString(), Icons.Filled.Key, WarningColor, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                TarjetaEstadistica("Pendientes", reservasPendientes.toString(), Icons.Filled.PendingActions, ErrorColor, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdminFlota(
    onAgregarVehiculo: () -> Unit,
    onAbrirEscaner: () -> Unit = {},
    onAbrirHistorialVehiculos: () -> Unit = {},
    flotaViewModel: FlotaViewModel = viewModel()
) {
    val listaVehiculos = flotaViewModel.listaVehiculos
    var vehiculoAEditar: Vehiculo? by remember { mutableStateOf(null) }
    val sheetStateEditar = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                FloatingActionButton(onClick = onAbrirEscaner, containerColor = Primary, contentColor = Color.White) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Escanear QR")
                }
                FloatingActionButton(onClick = onAbrirHistorialVehiculos, containerColor = TextPrimary, contentColor = Color.White) {
                    Icon(Icons.Filled.History, contentDescription = "Historial Vehículos")
                }
                FloatingActionButton(onClick = onAgregarVehiculo, containerColor = SuccessColor, contentColor = Color.White) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar Vehículo")
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Text("Gestión de Flota", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

            if (listaVehiculos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay vehículos registrados.", color = TextSecondary)
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

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Primary,
        unfocusedBorderColor = Color(0xFFE2E8F0),
        cursorColor = Primary,
        focusedLabelColor = Primary,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
        Text("Gestionar ${vehiculo.marca} ${vehiculo.modelo}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))
        Text("Estado Operativo:", fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            estadosPermitidos.forEach { estado ->
                Row(modifier = Modifier.fillMaxWidth().clickable { estadoSeleccionado = estado }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (estado == estadoSeleccionado),
                        onClick = { estadoSeleccionado = estado },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = TextSecondary)
                    )
                    Text(estado, color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción o notas") },
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 24.dp),
            maxLines = 4,
            colors = textFieldColors
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = TextSecondary) }
            Button(
                onClick = {
                    guardando = true
                    flotaViewModel.guardarVehiculoEnFirebase(context, vehiculo.copy(descripcion = descripcion, estado = estadoSeleccionado)) {
                        guardando = false
                        onCerrar()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !guardando
            ) { Text(if (guardando) "Actualizando..." else "Guardar Cambios", color = Color.White) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdminConfiguracion(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val usuarioActual = auth.currentUser

    // Estados
    var passNueva by remember { mutableStateOf("") }
    var passConfirmar by remember { mutableStateOf("") }
    var cargandoPass by remember { mutableStateOf(false) }

    var adminNombre by remember { mutableStateOf("") }
    var adminCorreo by remember { mutableStateOf("") }
    var adminPass by remember { mutableStateOf("") }
    var cargandoAdmin by remember { mutableStateOf(false) }

    var permisosCamara by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var permisosNotificaciones by remember { mutableStateOf(if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true) }

    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido -> permisosCamara = concedido }
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido -> permisosNotificaciones = concedido }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
        focusedBorderColor = Primary, unfocusedBorderColor = Color(0xFFE2E8F0),
        cursorColor = Primary, focusedLabelColor = Primary, unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedLeadingIconColor = Primary, unfocusedLeadingIconColor = TextSecondary
    )

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Configuración", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 4.dp, top = 8.dp))
        Text("Panel de seguridad y sistema", fontSize = 16.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 24.dp))


        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seguridad de Cuenta", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                OutlinedTextField(value = passNueva, onValueChange = { passNueva = it }, label = { Text("Nueva Contraseña") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, shape = RoundedCornerShape(12.dp), colors = textFieldColors)
                OutlinedTextField(value = passConfirmar, onValueChange = { passConfirmar = it }, label = { Text("Confirmar Contraseña") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, shape = RoundedCornerShape(12.dp), isError = passConfirmar.isNotEmpty() && passNueva != passConfirmar, colors = textFieldColors)

                if (passConfirmar.isNotEmpty() && passNueva != passConfirmar) {
                    Text("Las contraseñas no coinciden", color = ErrorColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                Button(
                    onClick = {
                        if (passNueva.isNotEmpty() && passNueva == passConfirmar) {
                            cargandoPass = true
                            usuarioActual?.updatePassword(passNueva)?.addOnCompleteListener { task ->
                                cargandoPass = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                                    passNueva = ""
                                    passConfirmar = ""
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}. Intenta reloguear.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Las contraseñas no coinciden o están vacías", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary), enabled = !cargandoPass
                ) { Text(if (cargandoPass) "Actualizando..." else "Actualizar Contraseña", color = Color.White) }
            }
        }

        // crear  NUEVO ADMINISTRADOR
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Icon(Icons.Default.AddModerator, contentDescription = null, tint = SuccessColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Nuevo Admin", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("Nota: Al crear un administrador, por seguridad tu sesión actual se cerrará.", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(value = adminNombre, onValueChange = { adminNombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = textFieldColors)
                OutlinedTextField(value = adminCorreo, onValueChange = { adminCorreo = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = textFieldColors)
                OutlinedTextField(value = adminPass, onValueChange = { adminPass = it }, label = { Text("Contraseña Provisional") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = textFieldColors)

                Button(
                    onClick = {
                        if (adminNombre.isNotEmpty() && adminCorreo.isNotEmpty() && adminPass.isNotEmpty()) {
                            cargandoAdmin = true
                            auth.createUserWithEmailAndPassword(adminCorreo, adminPass).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val nuevoUid = task.result?.user?.uid
                                    if (nuevoUid != null) {

                                        val adminData = mapOf(
                                            "nombre" to adminNombre,
                                            "correo" to adminCorreo,
                                            "esAdmin" to true
                                        )
                                        db.collection("usuarios").document(nuevoUid).set(adminData).addOnCompleteListener {
                                            cargandoAdmin = false
                                            Toast.makeText(context, "Administrador creado. Inicia sesión con la nueva cuenta.", Toast.LENGTH_LONG).show()
                                            onLogout() // Cierra la sesión y manda al login
                                        }
                                    }
                                } else {
                                    cargandoAdmin = false
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SuccessColor), enabled = !cargandoAdmin
                ) { Text(if (cargandoAdmin) "Creando..." else "Registrar Administrador", color = Color.White) }
            }
        }


        Text("Permisos del Dispositivo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Cámara", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (permisosCamara) "Activada" else "Desactivada", color = if (permisosCamara) SuccessColor else TextSecondary, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = permisosCamara,
                    onCheckedChange = { if (it) pedirPermisoCamara.launch(Manifest.permission.CAMERA) else abrirConfiguracionApp(context) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1))
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = WarningColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Notificaciones Push", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (permisosNotificaciones) "Activadas" else "Desactivadas", color = if (permisosNotificaciones) SuccessColor else TextSecondary, fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = permisosNotificaciones,
                    onCheckedChange = { if (it && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS) else abrirConfiguracionApp(context) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1))
                )
            }
        }


        OutlinedButton(
            onClick = { auth.signOut(); onLogout() },
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor)
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun abrirConfiguracionApp(context: Context) {
    Toast.makeText(context, "Modifica los permisos desde el sistema", Toast.LENGTH_LONG).show()
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
    context.startActivity(intent)
}

@Composable
fun TarjetaEstadistica(titulo: String, valor: String, icono: ImageVector, colorIcono: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(120.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = titulo, color = TextSecondary, fontSize = 14.sp)
                Icon(imageVector = icono, contentDescription = null, tint = colorIcono)
            }
            Text(text = valor, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ItemVehiculoGrid(vehiculo: Vehiculo, onClick: () -> Unit) {
    val colorEstado = when (vehiculo.estado) {
        "Disponible" -> SuccessColor
        "En Mantenimiento" -> WarningColor
        "Dado de baja" -> ErrorColor
        else -> TextSecondary
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
                    Icon(imageVector = Icons.Filled.DirectionsCar, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextPrimary)
                Text(text = vehiculo.anio, color = TextSecondary, fontSize = 13.sp)
                Text(text = vehiculo.precio, color = SuccessColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(shape = RoundedCornerShape(8.dp), color = colorEstado.copy(alpha = 0.15f), contentColor = colorEstado, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = vehiculo.estado, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}