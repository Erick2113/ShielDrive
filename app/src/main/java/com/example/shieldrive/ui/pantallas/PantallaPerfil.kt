package com.example.shieldrive.ui.pantallas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shieldrive.model.Resena
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun PantallaPerfil(
    resenasViewModel: ResenasGlobalViewModel,
    onLogout: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }

    var vistaActual by remember { mutableStateOf("principal") }


    var nombreMostrar by remember { mutableStateOf("Cargando...") }
    val correoMostrar = user?.email ?: "cliente@shieldrive.com"


    LaunchedEffect(vistaActual, user) {
        user?.reload()?.addOnCompleteListener {
            user = auth.currentUser


            user?.uid?.let { uid ->
                db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            nombreMostrar = document.getString("nombre") ?: "Usuario ShielDrive"
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
                usuarioNombre = nombreMostrar,
                usuarioCorreo = correoMostrar,
                onNavigate = { nuevaVista -> vistaActual = nuevaVista },
                onCerrarSesion = {
                    auth.signOut()
                    onLogout()
                }
            )
        }
        "editar" -> {
            PantallaEditarPerfil(
                usuarioActual = user,
                nombreActual = nombreMostrar,
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
    }
}

@Composable
fun PantallaPerfilPrincipal(
    usuarioNombre: String,
    usuarioCorreo: String,
    onNavigate: (String) -> Unit,
    onCerrarSesion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = usuarioNombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text(text = usuarioCorreo, fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))


        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                ItemMenuPerfil(Icons.Default.Edit, "Editar Perfil") { onNavigate("editar") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.RateReview, "Mis Reseñas") { onNavigate("resenas") }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.History, "Historial de Reservas") { /* Próximamente */ }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))

                ItemMenuPerfil(Icons.Default.Settings, "Ajustes de la App") { /* Próximamente */ }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarPerfil(
    usuarioActual: com.google.firebase.auth.FirebaseUser?,
    nombreActual: String,
    onVolver: () -> Unit
) {
    var nombre by remember { mutableStateOf(if (nombreActual == "Usuario ShielDrive" || nombreActual == "Cargando...") "" else nombreActual) }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            Text("Nickname de Usuario", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej. ErickDriver") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Nueva Contraseña (Opcional)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Déjalo vacío para no cambiarla") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (usuarioActual == null) return@Button
                    isLoading = true

                    val nuevoNombre = nombre.trim()


                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nuevoNombre)
                        .build()

                    usuarioActual.updateProfile(profileUpdates).addOnCompleteListener { taskName ->
                        if (taskName.isSuccessful) {


                            db.collection("usuarios").document(usuarioActual.uid)
                                .update("nombre", nuevoNombre)
                                .addOnCompleteListener {


                                    if (password.isNotBlank()) {
                                        usuarioActual.updatePassword(password).addOnCompleteListener { taskPw ->
                                            isLoading = false
                                            if (taskPw.isSuccessful) {
                                                Toast.makeText(contexto, "Perfil y contraseña actualizados", Toast.LENGTH_SHORT).show()
                                                onVolver()
                                            } else {
                                                Toast.makeText(contexto, "Error al cambiar contraseña. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(contexto, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                        onVolver()
                                    }
                                }
                        } else {
                            isLoading = false
                            Toast.makeText(contexto, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMisResenas(
    misResenas: List<Resena>,
    onEliminarResena: (String) -> Unit,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reseñas", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
            if (misResenas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No has calificado ningún vehículo aún.", color = Color.Gray)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                misResenas.forEach { resena ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(resena.vehiculoNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                    (1..5).forEach { i ->
                                        Icon(
                                            Icons.Rounded.Star, null,
                                            tint = if (i <= resena.estrellas) Color(0xFFF59E0B) else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(resena.comentario, fontSize = 14.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { onEliminarResena(resena.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Red)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2970FF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = Color(0xFF2970FF), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = titulo, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}