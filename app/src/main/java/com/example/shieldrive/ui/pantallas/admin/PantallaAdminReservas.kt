package com.example.shieldrive.ui.pantallas.admin

import androidx.compose.animation.animateColorAsState
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shieldrive.lanzarNotificacionPush
import com.example.shieldrive.viewmodel.ReservasAdminViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdminReservas(viewModel: ReservasAdminViewModel = viewModel()) {
    val reservas by viewModel.listaReservas.collectAsState()

    val reservasOcultasLocalmente = remember { mutableStateListOf<String>() }

    val reservasVisibles = reservas.filter { reserva ->
        reserva.id !in reservasOcultasLocalmente && !reserva.ocultaAdmin
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp)) {
        Text("Gestión de Reservas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
        Text("Solo puedes ocultar reservas ya procesadas o finalizadas", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))

        if (reservasVisibles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay reservas activas en este momento.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reservasVisibles, key = { it.id }) { reserva ->

                    // 🔒 REGLA: Solo se puede ocultar si NO está pendiente de autorización
                    val puedeOcultar = reserva.estado != "En proceso de autorización"

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart && puedeOcultar) {
                                reservasOcultasLocalmente.add(reserva.id)

                                FirebaseFirestore.getInstance()
                                    .collection("Reservas")
                                    .document(reserva.id)
                                    .update("ocultaAdmin", true)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = puedeOcultar, // Bloquea el swipe si está pendiente
                        backgroundContent = {
                            if (puedeOcultar) {
                                val color by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) ErrorColor else Color.Transparent,
                                    label = "ColorFondo"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Ocultar", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(reserva.vehiculoInfo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                        Text(
                                            text = reserva.estado,
                                            color = when(reserva.estado) {
                                                "En proceso de autorización" -> WarningColor
                                                "Confirmada" -> SuccessColor
                                                else -> ErrorColor
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Cliente: ${reserva.clienteNombre} (${reserva.clienteTelefono})", color = TextSecondary, fontSize = 14.sp)
                                    Text("Documento: ${reserva.documento}", color = TextSecondary, fontSize = 14.sp)
                                    Text("Fechas: ${reserva.fechaInicio} al ${reserva.fechaFin}", color = TextSecondary, fontSize = 14.sp)
                                    Text("Total: $${reserva.totalMonto}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = TextPrimary)

                                    if (reserva.estado == "En proceso de autorización") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {

                                            // BOTÓN RECHAZAR
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.procesarReserva(reserva.id, "Rechazada", reserva.vehiculoId, "Disponible")

                                                    lanzarNotificacionPush(
                                                        titulo = "Reserva Rechazada ❌",
                                                        mensaje = "Lo sentimos, tu reserva para el ${reserva.vehiculoInfo} no pudo ser aprobada.",
                                                        topicoDestino = "cliente_${reserva.userId}"
                                                    )
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorColor)
                                            ) { Text("Rechazar") }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // BOTÓN CONFIRMAR
                                            Button(
                                                onClick = {
                                                    viewModel.procesarReserva(reserva.id, "Confirmada", reserva.vehiculoId, "Disponible")

                                                    lanzarNotificacionPush(
                                                        titulo = "¡Reserva Aprobada! ✅🚗",
                                                        mensaje = "Tu ${reserva.vehiculoInfo} está listo. Revisa tu ticket en la app.",
                                                        topicoDestino = "cliente_${reserva.userId}"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                                            ) { Text("Confirmar", color = Color.White) }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}