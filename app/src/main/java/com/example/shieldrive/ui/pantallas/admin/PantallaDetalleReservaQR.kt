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
import com.example.shieldrive.ui.theme.*
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
                reservaData = doc.data?.plus("docId" to doc.id)

                val estado = doc.getString("estado") ?: ""
                if (estado == "En uso") {
                    val fechaFinStr = doc.getString("fechaFin") ?: ""
                    try {
                        val fechaFinMillis = formatoFechaCorta.parse(fechaFinStr)?.time ?: 0L
                        val limiteEntregaMillis = fechaFinMillis + (12 * 60 * 60 * 1000)
                        val ahora = System.currentTimeMillis()

                        val diferencia = ahora - limiteEntregaMillis
                        if (diferencia > 0) {
                            val horas = (diferencia / (1000 * 60 * 60)).toInt()
                            if (horas > 1) {
                                horasTarde = horas - 1
                                multaTotal = horasTarde * 5
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al buscar reserva", Toast.LENGTH_LONG).show()
        } finally {
            cargando = false
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner QR", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Transparent).padding(24.dp).verticalScroll(rememberScrollState())) {

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else if (reservaData == null) {
                Text("No se encontró ninguna reserva con este código.", color = ErrorColor, fontSize = 18.sp)
            } else {
                val estado = reservaData!!["estado"] as? String ?: ""
                val docId = reservaData!!["docId"] as String
                val vehiculoId = reservaData!!["vehiculoId"] as? String ?: ""
                val clienteNombre = reservaData!!["clienteNombre"] as? String ?: ""
                // Traemos el ID del usuario y la fecha fin para las notificaciones
                val usuarioId = reservaData!!["userId"] as? String ?: reservaData!!["usuarioId"] as? String ?: ""
                val fechaFinStr = reservaData!!["fechaFin"] as? String ?: ""

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TICKET: $idReserva", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Person, null, tint = Primary); Spacer(modifier = Modifier.width(8.dp))
                            Text(clienteNombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DirectionsCar, null, tint = SuccessColor); Spacer(modifier = Modifier.width(8.dp))
                            Text(reservaData!!["vehiculoInfo"] as? String ?: "", fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fechas: ${reservaData!!["fechaInicio"]} al $fechaFinStr", color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                if (estado != "En uso" && estado != "Finalizada") {
                    Text("Acción Requerida: ENTREGAR", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Al confirmar la entrega, se le notificará al cliente la fecha y hora límite de devolución.", color = TextSecondary)

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


                            val notificacionSalida = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "usuarioId" to usuarioId,
                                "titulo" to "🚗 ¡Vehículo Entregado con Éxito!",
                                "mensaje" to "Tu viaje ha iniciado hoy $ahoraString. Debes devolver el auto antes de las 12:00 PM del $fechaFinStr o se aplicarán recargos.",
                                "fecha" to ahoraMillis,
                                "leida" to false
                            )

                            db.collection("Reservas").document(docId).update(updatesReserva).addOnSuccessListener {
                                if (usuarioId.isNotEmpty()) {
                                    db.collection("notificaciones").document(notificacionSalida["id"] as String).set(notificacionSalida)
                                }
                                Toast.makeText(context, "Vehículo entregado y notificación enviada", Toast.LENGTH_LONG).show()
                                onFinalizado()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Color(0xFFE2E8F0)),
                        enabled = !procesando
                    ) {
                        if (procesando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Confirmar Entrega y Notificar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }


                else if (estado == "En uso") {
                    val fechaSalidaMillis = reservaData!!["fechaEntregaReal"] as? Long ?: 0L
                    val salidaString = if (fechaSalidaMillis > 0) formatoFechaHora.format(Date(fechaSalidaMillis)) else "Desconocida"

                    Text("Acción Requerida: DEVOLUCIÓN", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SuccessColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Salida registrada: $salidaString", color = TextSecondary)

                    if (multaTotal > 0) {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = ErrorColor.copy(alpha = 0.1f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = ErrorColor); Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Entrega Tardía ($horasTarde horas)", color = ErrorColor, fontWeight = FontWeight.Bold)
                                    Text("Multa a cobrar: $$multaTotal", color = ErrorColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
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

                            // Notificación de viaje finalizado
                            val notificacionFin = mapOf(
                                "id" to UUID.randomUUID().toString(),
                                "usuarioId" to usuarioId,
                                "titulo" to "✅ Vehículo Devuelto",
                                "mensaje" to "Has completado tu alquiler con éxito. ¡Gracias por confiar en ShielDrive!",
                                "fecha" to ahoraMillis,
                                "leida" to false
                            )

                            db.collection("Reservas").document(docId).update(updatesReserva).addOnSuccessListener {
                                db.collection("vehiculos").document(vehiculoId).update("estado", "Disponible")
                                db.collection("bitacoras").document(bitacora["id"] as String).set(bitacora)

                                if (usuarioId.isNotEmpty()) {
                                    db.collection("notificaciones").document(notificacionFin["id"] as String).set(notificacionFin)
                                }

                                Toast.makeText(context, "Vehículo recibido y bitácora guardada", Toast.LENGTH_LONG).show()
                                onFinalizado()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor, disabledContainerColor = Color(0xFFE2E8F0)),
                        enabled = !procesando
                    ) {
                        if (procesando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Aceptar Carro y Finalizar", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }


                else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Esta reserva ya fue finalizada.", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}