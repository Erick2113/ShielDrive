package com.example.shieldrive.ui.pantallas.admin

import android.graphics.BitmapFactory
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shieldrive.model.Vehiculo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionVehiculoBitacora(
    listaVehiculos: List<Vehiculo>,
    onVolver: () -> Unit,
    onVehiculoSeleccionado: (Vehiculo) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial por Vehículo", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(padding).padding(horizontal = 16.dp)) {
            Text("Selecciona un auto para ver su expediente", color = TextSecondary, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(listaVehiculos) { vehiculo ->
                    ItemVehiculoSinPrecio(vehiculo = vehiculo, onClick = { onVehiculoSeleccionado(vehiculo) })
                }
            }
        }
    }
}

@Composable
fun ItemVehiculoSinPrecio(vehiculo: Vehiculo, onClick: () -> Unit) {
    var decodedBitmap: android.graphics.Bitmap? = null
    if (vehiculo.urlImagen.isNotEmpty() && !vehiculo.urlImagen.startsWith("http")) {
        try {
            val imageBytes = Base64.decode(vehiculo.urlImagen, Base64.DEFAULT)
            decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) { }
    }


    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp).clickable { onClick() },
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
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = vehiculo.marca, color = TextSecondary, fontSize = 12.sp)
                Text(text = vehiculo.modelo, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextPrimary)
                Text(text = "Año: ${vehiculo.anio}", color = Primary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBitacoraEspecifica(vehiculoId: String, nombreVehiculo: String, onVolver: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var listaBitacoras by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    LaunchedEffect(vehiculoId) {
        db.collection("bitacoras")
            .whereEqualTo("vehiculoId", vehiculoId)
            .orderBy("fechaEntrada", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    listaBitacoras = snapshot.documents.mapNotNull { it.data }
                }
                cargando = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nombreVehiculo, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(padding).padding(16.dp)) {
            Text("Expediente del Vehículo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            } else if (listaBitacoras.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assignment, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Text("Este auto no tiene historial de viajes aún.", color = TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listaBitacoras) { bitacora ->
                        val cliente = bitacora["clienteNombre"] as? String ?: "Desconocido"
                        val observaciones = bitacora["observaciones"] as? String ?: "Sin observaciones"
                        val multa = (bitacora["multaCobrada"] as? Number)?.toInt() ?: 0
                        val salidaMillis = bitacora["fechaSalida"] as? Long ?: 0L
                        val entradaMillis = bitacora["fechaEntrada"] as? Long ?: 0L


                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Person, null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cliente, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Salió:", color = TextSecondary, fontSize = 12.sp)
                                        Text(if(salidaMillis>0) formatoFecha.format(Date(salidaMillis)) else "N/A", fontSize = 13.sp, color = TextPrimary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Regresó:", color = TextSecondary, fontSize = 12.sp)
                                        Text(if(entradaMillis>0) formatoFecha.format(Date(entradaMillis)) else "N/A", fontSize = 13.sp, color = TextPrimary)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0)) // DIVISOR GRIS CLARO
                                Text("Observaciones:", color = TextSecondary, fontSize = 12.sp)
                                Text(observaciones.ifBlank { "Todo en orden." }, fontSize = 14.sp, color = TextPrimary)

                                if (multa > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth().background(ErrorColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Multa por retraso:", color = ErrorColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("$$multa", color = ErrorColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
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