package com.example.shieldrive.ui.pantallas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleVehiculo(
    vehiculo: Vehiculo,
    resenasViewModel: ResenasGlobalViewModel,
    onVolver: () -> Unit,
    onReservarClick: (Vehiculo) -> Unit
) {

    val resenasDelAuto = resenasViewModel.obtenerResenasPorVehiculo(vehiculo.id)
    val yaCalifico = resenasViewModel.yaCalificoVehiculo(vehiculo.id)

    val cantidadResenas = resenasDelAuto.size
    val ratingReal = if (cantidadResenas > 0) resenasDelAuto.sumOf { it.estrellas }.toDouble() / cantidadResenas else 0.0

    val isDisponible = vehiculo.estado == "Disponible"

    val textoBoton = when (vehiculo.estado) {
        "Disponible" -> "Reservar Ahora"
        "En proceso" -> "En proceso de reserva..."
        else -> "No disponible"
    }

    val colorEstado = when(vehiculo.estado) {
        "Disponible" -> SuccessColor
        "En proceso" -> WarningColor
        else -> ErrorColor
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary) }
                },
                actions = {
                    IconButton(onClick = { /* Opciones */ }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { if (isDisponible) onReservarClick(vehiculo) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDisponible) Primary else TextSecondary),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isDisponible
                ) {
                    Text(textoBoton, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (isDisponible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                val decodedBitmap = remember(vehiculo.urlImagen) {
                    if (vehiculo.urlImagen.isNotEmpty() && !vehiculo.urlImagen.startsWith("http")) {
                        try {
                            val imageBytes = Base64.decode(vehiculo.urlImagen, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) { null }
                    } else null
                }

                if (vehiculo.urlImagen.startsWith("http")) {
                    AsyncImage(model = vehiculo.urlImagen, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else if (decodedBitmap != null) {
                    Image(bitmap = decodedBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${vehiculo.marca} ${vehiculo.modelo} ${vehiculo.anio}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = colorEstado.copy(alpha = 0.1f), modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = vehiculo.estado,
                            color = colorEstado,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = WarningColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${String.format(Locale.getDefault(), "%.1f", ratingReal)} ($cantidadResenas Reseñas)", fontSize = 14.sp, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.AirlineSeatReclineNormal, label = "Asientos", value = if (vehiculo.asientos.isNotEmpty()) vehiculo.asientos else "N/D")
                    FeatureCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Settings, label = "Transmisión", value = vehiculo.transmision.ifEmpty { "N/D" })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.Speed, label = "Motor", value = vehiculo.motor.ifEmpty { "N/D" })
                    FeatureCard(modifier = Modifier.weight(1f), icon = Icons.Rounded.CalendarToday, label = "Año", value = vehiculo.anio.ifEmpty { "N/D" })
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Descripción", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = vehiculo.descripcion.ifEmpty { "Sin descripción disponible." }, fontSize = 14.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Reseñas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                if (resenasDelAuto.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no hay reseñas para este vehículo.", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(resenasDelAuto) { resena ->


                            val idUsuario = try {
                                resena::class.java.getDeclaredField("usuarioId").apply { isAccessible = true }.get(resena) as? String ?: ""
                            } catch (e: Exception) {
                                try {
                                    resena::class.java.getDeclaredField("userId").apply { isAccessible = true }.get(resena) as? String ?: ""
                                } catch (e2: Exception) { "" }
                            }

                            ResenaCard(
                                autor = resena.autor,
                                comentario = resena.comentario,
                                estrellas = resena.estrellas,
                                usuarioId = idUsuario
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(24.dp))

                if (yaCalifico) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Ya has calificado este vehículo. Puedes eliminar tu reseña desde tu Perfil para calificar de nuevo.",
                            modifier = Modifier.padding(16.dp),
                            color = Color.DarkGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    var ratingInput by remember { mutableIntStateOf(0) }
                    var comentarioInput by remember { mutableStateOf("") }

                    Text("Agregar una reseña", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= ratingInput) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null,
                                tint = if (index <= ratingInput) WarningColor else TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp).clickable { ratingInput = index }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = comentarioInput,
                        onValueChange = { comentarioInput = it },
                        placeholder = { Text("Escribe tu experiencia con el vehículo...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Primary,
                            cursorColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            resenasViewModel.agregarResena(vehiculo.id, "${vehiculo.marca} ${vehiculo.modelo}", ratingInput, comentarioInput)
                        },
                        enabled = ratingInput > 0 && comentarioInput.isNotBlank(),
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Publicar Reseña", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun FeatureCard(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Card(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun ResenaCard(autor: String, comentario: String, estrellas: Int, usuarioId: String) {
    var fotoBase64 by remember { mutableStateOf("") }


    LaunchedEffect(usuarioId) {
        if (usuarioId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("usuarios").document(usuarioId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        fotoBase64 = doc.getString("fotoBase64") ?: ""
                    }
                }
        }
    }

    Card(modifier = Modifier.width(260.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                    val bitmap = decodificarBase64ParaResenas(fotoBase64)
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(autor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Row { (1..5).forEach { i -> Icon(Icons.Rounded.Star, contentDescription = null, tint = if (i <= estrellas) WarningColor else TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(12.dp)) } }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(comentario, fontSize = 13.sp, color = TextSecondary, maxLines = 2)
        }
    }
}


fun decodificarBase64ParaResenas(base64Str: String): Bitmap? {
    if (base64Str.isEmpty()) return null
    return try {
        val imageBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) { null }
}