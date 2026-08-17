package com.example.shieldrive.ui.pantallas

import androidx.compose.foundation.Image
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shieldrive.model.Reserva
import com.example.shieldrive.viewmodel.ReservasUsuarioViewModel

@Composable
fun PantallaReservas(reservasViewModel: ReservasUsuarioViewModel) {

    val misReservas = reservasViewModel.listaMisReservas
    var reservaSeleccionada by remember { mutableStateOf<Reserva?>(null) }


    val reservasVisibles = misReservas.filter { !it.archivada }


    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp)) {
        Text("Mis Reservas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 16.dp))
        Text("Toca una reserva para ver tu ticket QR", fontSize = 14.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))

        if (reservasVisibles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes reservas activas.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reservasVisibles) { reserva ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { reservaSeleccionada = reserva },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reserva.vehiculoInfo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text("${reserva.fechaInicio} al ${reserva.fechaFin}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {

                                val colorEstado = when(reserva.estado) {
                                    "Finalizada" -> TextSecondary
                                    "Rechazada" -> MaterialTheme.colorScheme.error
                                    else -> SuccessColor
                                }
                                Text(reserva.estado, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colorEstado)
                                Text("$${reserva.totalMonto}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        }
    }

    reservaSeleccionada?.let { reserva ->
        Dialog(onDismissRequest = { reservaSeleccionada = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TICKET DE RESERVA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp)
                        IconButton(onClick = { reservaSeleccionada = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, null, tint = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(reserva.id, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(24.dp))

                    val qrBitmap = generarQR(reserva.id)
                    if (qrBitmap != null) {
                        Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.background, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vehículo:", color = TextSecondary, fontSize = 14.sp)
                        Text(reserva.vehiculoInfo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Fechas:", color = TextSecondary, fontSize = 14.sp)
                        Text("${reserva.fechaInicio} - ${reserva.fechaFin}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }


                    if (reserva.estado == "Finalizada" || reserva.estado == "Rechazada") {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = {
                                reservasViewModel.archivarReserva(reserva.id)
                                reservaSeleccionada = null // Cerramos el dialog al borrar
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar de mis reservas")
                        }
                    }
                }
            }
        }
    }
}