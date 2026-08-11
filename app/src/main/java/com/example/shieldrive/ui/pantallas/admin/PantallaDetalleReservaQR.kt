package com.example.shieldrive.ui.pantallas.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleReservaQR(
    idReserva: String,
    onVolver: () -> Unit,
    onFinalizado: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var reservaData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var procesando by remember { mutableStateOf(false) }

    // Variables para la devolución
    var observaciones by remember { mutableStateOf("") }
    var multaTotal by remember { mutableIntStateOf(0) }
    var horasTarde by remember { mutableIntStateOf(0) }

    val formatoFechaHora = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    val formatoFechaCorta = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Cargar la reserva al abrir la pantalla
    LaunchedEffect(idReserva) {
        try {
            val query = db.collection("Reservas").whereEqualTo("id", idReserva).get().await()
            if (!query.isEmpty) {
                val doc = query.documents.first()
                reservaData = doc.data?.plus("docId" to doc.id) // Guardamos el ID del documento de Firebase

                // Si el estado es "En uso", calculamos multas
                val estado = doc.getString("estado") ?: ""
                if (estado == "En uso") {
                    val fechaFinStr = doc.getString("fechaFin") ?: ""
                    try {
                        val fechaFinMillis = formatoFechaCorta.parse(fechaFinStr)?.time ?: 0L
                        // Asumimos que la hora límite de entrega son las 12:00 PM (mediodía) de la fecha final
                        val limiteEntregaMillis = fechaFinMillis + (12 * 60 * 60 * 1000)
                        val ahora = System.currentTimeMillis()

                        val diferencia = ahora - limiteEntregaMillis
                        if (diferencia > 0) {
                            val horas = (diferencia / (1000 * 60 * 60)).toInt()
                            if (horas > 1) { // 1 hora de tolerancia
                                horasTarde = horas - 1
                                multaTotal = horasTarde * 5 // $5 por hora extra
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al buscar reserva", Toast.LENGTH_SHORT).show()
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner QR", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (reservaData == null) {
                Text("No se encontró ninguna reserva con este código.", color = Color.Red, fontSize = 18.sp)
            } else {
                val estado = reservaData!!["estado"] as? String ?: ""
                val docId = reservaData!!["docId"] as String
                val vehiculoId = reservaData!!["vehiculoId"] as? String ?: ""
                val clienteNombre = reservaData!!["clienteNombre"] as? String ?: ""

                // TARJETA DE INFORMACIÓN GENERAL
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TICKET: $idReserva", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, null, tint = Color(0xFF2970FF)); Spacer(modifier = Modifier.width(8.dp))
                            Text(clienteNombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DirectionsCar, null, tint = Color(0xFF10B981)); Spacer(modifier = Modifier.width(8.dp))
                            Text(reservaData!!["vehiculoInfo"] as? String ?: "", fontWeight = FontWeight.Medium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fechas: ${reservaData!!["fechaInicio"]} al ${reservaData!!["fechaFin"]}", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==========================================
                // CASO 1: ENTREGAR VEHÍCULO (Salida)
                // ==========================================
                if (estado != "En uso" && estado != "Finalizada") {
                    Text("Acción Requerida: ENTREGAR", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF2970FF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("El cliente está listo para llevarse el vehículo. Al confirmar, se iniciará el conteo de tiempo.", color = Color.Gray)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            procesando = true
                            val ahoraMillis = System.currentTimeMillis()
                            val ahoraString = formatoFechaHora.format(Date(ahoraMillis))

                            val updatesReserva = mapOf(
                                "estado" to "En uso",
                                "fechaEntregaReal" to ahoraMillis
                            )

                            val nuevaNotificacion = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "usuarioId" to (reservaData!!["usuarioId"] as? String ?: ""),
                                "titulo" to "Vehículo Entregado",
                                "mensaje" to "Tu reserva ha iniciado el $ahoraString. Recuerda devolver el auto antes de las 12:00 PM de tu fecha final o se aplicarán cargos.",
                                "fecha" to ahoraMillis,
                                "leida" to false
                            )

                            // Actualizamos Reserva, luego mandamos Notificación
                            db.collection("Reservas").document(docId).update(updatesReserva).addOnSuccessListener {
                                db.collection("notificaciones").document(nuevaNotificacion["id"] as String).set(nuevaNotificacion)
                                Toast.makeText(context, "Vehículo entregado con éxito", Toast.LENGTH_LONG).show()
                                onFinalizado()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF)),
                        enabled = !procesando
                    ) {
                        if (procesando) CircularProgressIndicator(color = Color.White) else Text("Confirmar Entrega de Vehículo", fontWeight = FontWeight.Bold)
                    }
                }

                // ==========================================
                // CASO 2: RECIBIR VEHÍCULO (Entrada)
                // ==========================================
                else if (estado == "En uso") {
                    val fechaSalidaMillis = reservaData!!["fechaEntregaReal"] as? Long ?: 0L
                    val salidaString = if (fechaSalidaMillis > 0) formatoFechaHora.format(Date(fechaSalidaMillis)) else "Desconocida"

                    Text("Acción Requerida: DEVOLUCIÓN", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Salida registrada: $salidaString", color = Color.Gray)

                    if (multaTotal > 0) {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = Color.Red); Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Entrega Tardía ($horasTarde horas)", color = Color.Red, fontWeight = FontWeight.Bold)
                                    Text("Multa a cobrar: $$multaTotal", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = observaciones,
                        onValueChange = { observaciones = it },
                        label = { Text("Observaciones del vehículo (Daños, gasolina, etc)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            procesando = true
                            val ahoraMillis = System.currentTimeMillis()

                            val updatesReserva = mapOf("estado" to "Finalizada")

                            val bitacora = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "reservaId" to idReserva,
                                "vehiculoId" to vehiculoId,
                                "clienteNombre" to clienteNombre,
                                "fechaSalida" to fechaSalidaMillis,
                                "fechaEntrada" to ahoraMillis,
                                "observaciones" to observaciones,
                                "multaCobrada" to multaTotal
                            )

                            // Actualizamos Reserva -> Liberamos Vehículo -> Guardamos Bitácora
                            db.collection("Reservas").document(docId).update(updatesReserva).addOnSuccessListener {
                                db.collection("vehiculos").document(vehiculoId).update("estado", "Disponible")
                                db.collection("bitacoras").document(bitacora["id"] as String).set(bitacora)

                                Toast.makeText(context, "Vehículo recibido y bitácora guardada", Toast.LENGTH_LONG).show()
                                onFinalizado()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        enabled = !procesando
                    ) {
                        if (procesando) CircularProgressIndicator(color = Color.White) else Text("Aceptar Carro y Finalizar", fontWeight = FontWeight.Bold)
                    }
                }

                // ==========================================
                // CASO 3: YA ESTÁ FINALIZADA
                // ==========================================
                else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Esta reserva ya fue finalizada.", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}