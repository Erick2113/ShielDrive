package com.example.shieldrive.ui.pantallas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shieldrive.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaNotificaciones(onVolver: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val context = LocalContext.current

    var listaNotificaciones by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())


    val fondoDegradado = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE0F2FE),
            Color(0xFFFFFFFF)
        )
    )

    LaunchedEffect(uid) {
        if (uid != null) {

            db.collection("notificaciones")
                .whereEqualTo("usuarioId", uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        listaNotificaciones = snapshot.documents.map { doc ->
                            doc.data?.plus("docId" to doc.id) ?: emptyMap()
                        }


                        snapshot.documents.forEach { doc ->
                            val leida = doc.getBoolean("leida") ?: true
                            if (!leida) {
                                db.collection("notificaciones").document(doc.id).update("leida", true)
                            }
                        }
                    }
                    cargando = false
                }
        } else {
            cargando = false
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoDegradado)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (cargando) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (listaNotificaciones.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = TextSecondary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Estás al día", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Aquí aparecerán tus alertas de reservas.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(listaNotificaciones, key = { it["docId"].toString() }) { noti ->
                            val titulo = noti["titulo"] as? String ?: "Aviso"
                            val mensaje = noti["mensaje"] as? String ?: ""
                            val fechaMillis = noti["fecha"] as? Long ?: 0L
                            val fechaString = if (fechaMillis > 0) formatoFecha.format(Date(fechaMillis)) else ""
                            val docId = noti["docId"] as? String ?: ""

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, null, tint = Primary)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(mensaje, fontSize = 14.sp, color = TextSecondary, lineHeight = 18.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(fechaString, fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.6f))
                                    }
                                    IconButton(
                                        onClick = {

                                            db.collection("notificaciones").document(docId).delete()
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                                                }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ErrorColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}