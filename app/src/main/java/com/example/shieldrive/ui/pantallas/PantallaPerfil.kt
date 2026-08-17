package com.example.shieldrive.ui.pantallas

import android.Manifest
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.shieldrive.model.Resena
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import com.example.shieldrive.viewmodel.ReservasUsuarioViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

@Composable
fun PantallaPerfil(
    resenasViewModel: ResenasGlobalViewModel,
    reservasViewModel: ReservasUsuarioViewModel,
    onLogout: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }

    var vistaActual by remember { mutableStateOf("principal") }

    var nombreMostrar by remember { mutableStateOf("Cargando...") }
    var fotoBase64Mostrar by remember { mutableStateOf("") }
    val correoMostrar = user?.email ?: "cliente@shieldrive.com"

    LaunchedEffect(vistaActual, user) {
        user?.reload()?.addOnCompleteListener {
            user = auth.currentUser
            user?.uid?.let { uid ->
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            nombreMostrar = document.getString("nombre") ?: "Usuario ShielDrive"
                            fotoBase64Mostrar = document.getString("fotoBase64") ?: ""
                        } else {
                            nombreMostrar = user?.displayName.takeIf { !it.isNullOrBlank() } ?: "Usuario ShielDrive"
                        }
                    }
            }
        }
    }

    when (vistaActual) {
        "principal" -> {
            PantallaPerfilPrincipal(
                usuarioNombre = nombreMostrar, usuarioCorreo = correoMostrar, fotoBase64 = fotoBase64Mostrar,
                onNavigate = { nuevaVista -> vistaActual = nuevaVista },
                onCerrarSesion = { auth.signOut(); onLogout() }
            )
        }
        "editar" -> {
            PantallaEditarPerfil(
                usuarioActual = user, nombreActual = nombreMostrar, fotoActualBase64 = fotoBase64Mostrar,
                onVolver = { vistaActual = "principal" }
            )
        }
        "resenas" -> {
            PantallaMisResenas(
                misResenas = resenasViewModel.obtenerMisResenas(),
                onEliminarResena = { id -> resenasViewModel.eliminarResena(id) },
                onVolver = { vistaActual = "principal" }
            )
        }
        "historial" -> {
            PantallaHistorialReservas(reservasViewModel = reservasViewModel, onVolver = { vistaActual = "principal" })
        }
        "ajustes" -> {
            PantallaAjustesApp(onVolver = { vistaActual = "principal" })
        }
    }
}

@Composable
fun PantallaPerfilPrincipal(
    usuarioNombre: String, usuarioCorreo: String, fotoBase64: String, onNavigate: (String) -> Unit, onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Transparent).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(

            modifier = Modifier.size(110.dp).clip(CircleShape).background(Color(0xFFE2E8F0)).clickable { onNavigate("editar") },
            contentAlignment = Alignment.Center
        ) {
            val bitmap = decodificarBase64(fotoBase64)
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = usuarioNombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = usuarioCorreo, fontSize = 14.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(40.dp))


        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                ItemMenuPerfil(Icons.Default.Edit, "Editar Perfil") { onNavigate("editar") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.RateReview, "Mis Reseñas") { onNavigate("resenas") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.History, "Historial de Reservas") { onNavigate("historial") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.Settings, "Ajustes y Permisos") { onNavigate("ajustes") }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))


        OutlinedButton(
            onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor), border = androidx.compose.foundation.BorderStroke(1.dp, ErrorColor)
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustesApp(onVolver: () -> Unit) {
    val context = LocalContext.current

    var permisosCamara by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var permisosNotificaciones by remember {
        mutableStateOf(if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else true)
    }

    var mostrarDialogoRechazo by remember { mutableStateOf(false) }

    val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido -> permisosCamara = concedido }
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido -> permisosNotificaciones = concedido }

    if (mostrarDialogoRechazo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoRechazo = false },
            containerColor = Color.White, // FORZADO A BLANCO
            title = { Text("Seguridad de Android", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Android prohíbe desactivar permisos directamente desde la app. Debes hacerlo en los ajustes del sistema. ¿Deseas ir ahora?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoRechazo = false; abrirConfiguracionApp(context) }) { Text("Ir a Ajustes", color = Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoRechazo = false }) { Text("Cancelar", color = TextSecondary) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes de la App", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, "Volver", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {

            Text("Gestión de Permisos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Controla el acceso al hardware de tu dispositivo.", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp, top = 4.dp))


            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
                        onCheckedChange = { activar ->
                            if (activar) { pedirPermisoCamara.launch(Manifest.permission.CAMERA) }
                            else { mostrarDialogoRechazo = true }
                        },

                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1))
                    )
                }
            }


            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
                        onCheckedChange = { activar ->
                            if (activar && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            else { mostrarDialogoRechazo = true }
                        },

                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFCBD5E1))
                    )
                }
            }
        }
    }
}

fun abrirConfiguracionApp(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarPerfil(usuarioActual: com.google.firebase.auth.FirebaseUser?, nombreActual: String, fotoActualBase64: String, onVolver: () -> Unit) {
    var nombre by remember { mutableStateOf(if (nombreActual == "Usuario ShielDrive" || nombreActual == "Cargando...") "" else nombreActual) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fotoBase64 by remember { mutableStateOf(fotoActualBase64) }
    var isLoading by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val tienePermisoCamara = ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contexto.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                fotoBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) { Toast.makeText(contexto, "Error al procesar la imagen", Toast.LENGTH_LONG).show() }
        }
    }


    val textColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Primary,
        cursorColor = Primary,
        focusedLabelColor = Primary,
        focusedLeadingIconColor = Primary,
        unfocusedLeadingIconColor = TextSecondary
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Editar Perfil", fontWeight = FontWeight.Bold, color = TextPrimary) }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
                    .clickable {
                        if (tienePermisoCamara) {
                            galleryLauncher.launch("image/*")
                        } else {
                            Toast.makeText(contexto, "Permiso denegado. Actívalo en Ajustes de la App.", Toast.LENGTH_LONG).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val bitmap = decodificarBase64(fotoBase64)
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White) }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp)); Text("Subir foto", color = TextSecondary, fontSize = 12.sp) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                Text("Nickname de Usuario", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(12.dp), colors = textColors)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Nueva Contraseña (Opcional)", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Déjalo vacío para no cambiarla") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = textColors)

                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Confirmar Nueva Contraseña", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Repite la contraseña") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), isError = confirmPassword.isNotEmpty() && password != confirmPassword, colors = textColors)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    if (usuarioActual == null) return@Button
                    if (password.isNotEmpty() && password != confirmPassword) { Toast.makeText(contexto, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show(); return@Button }

                    isLoading = true
                    val nuevoNombre = nombre.trim()
                    val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(nuevoNombre).build()

                    usuarioActual.updateProfile(profileUpdates).addOnCompleteListener { taskName ->
                        if (taskName.isSuccessful) {
                            val updatesFirestore = mapOf("nombre" to nuevoNombre, "fotoBase64" to fotoBase64)


                            db.collection("usuarios").document(usuarioActual.uid)
                                .set(updatesFirestore, com.google.firebase.firestore.SetOptions.merge())
                                .addOnCompleteListener {
                                    if (password.isNotBlank()) {
                                        usuarioActual.updatePassword(password).addOnCompleteListener { taskPw ->
                                            isLoading = false
                                            if (taskPw.isSuccessful) { Toast.makeText(contexto, "Perfil actualizado completo", Toast.LENGTH_LONG).show(); onVolver() }
                                            else { Toast.makeText(contexto, "Error de seguridad. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show() }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(contexto, "Perfil actualizado", Toast.LENGTH_LONG).show(); onVolver()
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Color(0xFFE2E8F0)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun decodificarBase64(base64Str: String): Bitmap? {
    if (base64Str.isEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorialReservas(reservasViewModel: ReservasUsuarioViewModel, onVolver: () -> Unit) {
    val historial = reservasViewModel.listaMisReservas.filter { it.archivada }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de Reservas", fontWeight = FontWeight.Bold, color = TextPrimary) }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (historial.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tienes reservas en tu historial.", color = TextSecondary) } }
            else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    items(historial) { reserva ->
                        // TARJETA BLANCA
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(50.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.DirectionsCar, null, tint = TextSecondary) }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(reserva.vehiculoInfo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Text("${reserva.fechaInicio} al ${reserva.fechaFin}", fontSize = 12.sp, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(reserva.estado, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (reserva.estado == "Rechazada") ErrorColor else TextSecondary)
                                    Text("$${reserva.totalMonto}", fontWeight = FontWeight.Black, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisResenas(misResenas: List<Resena>, onEliminarResena: (String) -> Unit, onVolver: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Reseñas", fontWeight = FontWeight.Bold, color = TextPrimary) }, navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
            if (misResenas.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No has calificado ningún vehículo aún.", color = TextSecondary) } }
            else {
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(misResenas) { resena ->

                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(resena.vehiculoNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) { (1..5).forEach { i -> Icon(Icons.Rounded.Star, null, tint = if (i <= resena.estrellas) WarningColor else TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp)) } }
                                    Text(resena.comentario, fontSize = 14.sp, color = TextSecondary)
                                }
                                IconButton(onClick = { onEliminarResena(resena.id) }) { Icon(Icons.Filled.Delete, "Eliminar", tint = ErrorColor) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemMenuPerfil(icono: ImageVector, titulo: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 18.dp, horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icono, null, tint = Primary, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = titulo, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}