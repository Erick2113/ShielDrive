package com.example.shieldrive.ui.pantallas.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shieldrive.viewmodel.ReservasAdminViewModel

@Composable
fun PantallaAdminReservas(viewModel: ReservasAdminViewModel = viewModel()) {
    val reservas by viewModel.listaReservas.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(16.dp)) {
        Text("Gestión de Reservas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 16.dp))
        Text("Acepta o rechaza las solicitudes de clientes", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        if (reservas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay reservas activas en este momento.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reservas) { reserva ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(reserva.vehiculoInfo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                                Text(
                                    text = reserva.estado,
                                    color = when(reserva.estado) {
                                        "En proceso de autorización" -> Color(0xFFF59E0B)
                                        "Confirmada" -> Color(0xFF10B981)
                                        else -> Color.Red
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cliente: ${reserva.clienteNombre} (${reserva.clienteTelefono})", color = Color.Gray, fontSize = 14.sp)
                            Text("Documento: ${reserva.documento}", color = Color.Gray, fontSize = 14.sp)
                            Text("Fechas: ${reserva.fechaInicio} al ${reserva.fechaFin}", color = Color.Gray, fontSize = 14.sp)
                            Text("Total: $${reserva.totalMonto}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1E293B))

                            if (reserva.estado == "En proceso de autorización") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {

                                            viewModel.procesarReserva(reserva.id, "Rechazada", reserva.vehiculoId, "Disponible")
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                    ) { Text("Rechazar") }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {

                                            viewModel.procesarReserva(reserva.id, "Confirmada", reserva.vehiculoId, "No disponible")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) { Text("Confirmar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}