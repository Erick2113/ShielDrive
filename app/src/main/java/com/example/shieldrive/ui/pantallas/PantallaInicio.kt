package com.example.shieldrive.ui.pantallas

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.shieldrive.R
import com.example.shieldrive.model.Reserva
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.ui.navegacion.Ruta
import com.example.shieldrive.viewmodel.FavoritosViewModel
import com.example.shieldrive.viewmodel.FlotaViewModel
import com.example.shieldrive.viewmodel.ReservasUsuarioViewModel
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel
import java.util.Locale

@Composable
fun PantallaInicio(
    navController: NavController,
    flotaViewModel: FlotaViewModel = viewModel(),
    reservasViewModel: ReservasUsuarioViewModel = viewModel(),
    favoritosViewModel: FavoritosViewModel = viewModel(),
    resenasViewModel: ResenasGlobalViewModel = viewModel()
) {
    val listaVehiculos = flotaViewModel.listaVehiculos
    val listaReservas = reservasViewModel.listaMisReservas

    PantallaInicioUsuario(
        listaVehiculos = listaVehiculos,
        listaReservas = listaReservas,
        favoritosViewModel = favoritosViewModel,
        resenasViewModel = resenasViewModel,
        onVehiculoClick = { vehiculo ->
            navController.navigate(Ruta.DetalleVehiculo.crearRuta(vehiculo.id))
        },
        onNotificacionesClick = {
            navController.navigate(Ruta.Notificaciones.ruta)
        },
        onCalificarAction = { reserva, estrellas, comentario ->
            reservasViewModel.calificarReserva(reserva, estrellas, comentario)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicioUsuario(
    listaVehiculos: List<Vehiculo>,
    listaReservas: List<Reserva> = emptyList(),
    favoritosViewModel: FavoritosViewModel,
    resenasViewModel: ResenasGlobalViewModel,
    onVehiculoClick: (Vehiculo) -> Unit,
    onNotificacionesClick: () -> Unit,
    onCalificarAction: (Reserva, Int, String) -> Unit = { _, _, _ -> }
) {
    val reservaParaCalificar = listaReservas.firstOrNull { it.estado == "Finalizada" && !it.calificada }
    var mostrarDialogoCalificacion by remember { mutableStateOf(false) }

    var queryBusqueda by remember { mutableStateOf("") }
    var marcaSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarSheetFiltros by remember { mutableStateOf(false) }
    var filtroPrecioMaximo by remember { mutableFloatStateOf(200f) }
    var filtroTransmision by remember { mutableStateOf("Todas") }

    val vehiculosFiltrados = listaVehiculos.filter { vehiculo ->

        val nombreCompleto = "${vehiculo.marca} ${vehiculo.modelo}"
        val coincideTexto = queryBusqueda.isEmpty() || nombreCompleto.contains(queryBusqueda, ignoreCase = true)
        val coincideMarca = marcaSeleccionada == null || vehiculo.marca.equals(marcaSeleccionada, ignoreCase = true)
        val precioNumerico = vehiculo.precio.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0f
        val coincidePrecio = precioNumerico <= filtroPrecioMaximo
        val coincideTransmision = filtroTransmision == "Todas" || vehiculo.transmision.equals(filtroTransmision, ignoreCase = true)

        coincideTexto && coincideMarca && coincidePrecio && coincideTransmision
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).verticalScroll(rememberScrollState())) {
        HeaderUsuario(onNotificacionesClick = onNotificacionesClick)

        SearchBarUsuario(
            query = queryBusqueda,
            onQueryChange = { queryBusqueda = it },
            onFilterClick = { mostrarSheetFiltros = true }
        )

        if (reservaParaCalificar != null) {
            SeccionRecordatorioCalificacion(reserva = reservaParaCalificar, onCalificarClick = { mostrarDialogoCalificacion = true })
        }

        BrandsSection(
            marcaSeleccionada = marcaSeleccionada,
            onMarcaClick = { marca -> marcaSeleccionada = if (marcaSeleccionada == marca) null else marca }
        )

        Text("Autos Disponibles", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp))

        if (vehiculosFiltrados.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No hay autos que coincidan con tus filtros.", color = Color.Gray)
            }
        } else {
            vehiculosFiltrados.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { vehiculo ->
                        Box(modifier = Modifier.weight(1f)) {
                            val resenasDelAuto = resenasViewModel.obtenerResenasPorVehiculo(vehiculo.id)
                            val numResenas = resenasDelAuto.size
                            val ratingReal = if (numResenas > 0) resenasDelAuto.sumOf { it.estrellas }.toDouble() / numResenas else 0.0

                            CarCardUsuario(
                                vehiculo = vehiculo,
                                rating = ratingReal,
                                numResenas = numResenas,
                                isFavorito = favoritosViewModel.esFavorito(vehiculo.id),
                                onFavoritoClick = { favoritosViewModel.toggleFavorito(vehiculo) },
                                onClick = { onVehiculoClick(vehiculo) }
                            )
                        }
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (mostrarSheetFiltros) {
        ModalBottomSheet(onDismissRequest = { mostrarSheetFiltros = false }) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Filtros de Búsqueda", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Precio Máximo por Día", fontWeight = FontWeight.Medium)
                    Text("$${filtroPrecioMaximo.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF2970FF))
                }
                Slider(value = filtroPrecioMaximo, onValueChange = { filtroPrecioMaximo = it }, valueRange = 10f..300f, steps = 29, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Text("Transmisión", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todas", "Automática", "Manual").forEach { tipo ->
                        FilterChip(selected = filtroTransmision == tipo, onClick = { filtroTransmision = tipo }, label = { Text(tipo) })
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { mostrarSheetFiltros = false }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF))) { Text("Aplicar Filtros") }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeaderUsuario(onNotificacionesClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { Text(text = "ShielDrive", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF2970FF)); Text(text = "Tu destino, nuestra ruta", fontSize = 14.sp, color = Color.Gray) }
        Row {
            IconButton(onClick = onNotificacionesClick) { Icon(Icons.Rounded.NotificationsNone, null, tint = Color.Black) }
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, null, tint = Color.Gray) }
        }
    }
}

@Composable
fun SearchBarUsuario(query: String, onQueryChange: (String) -> Unit, onFilterClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = query, onValueChange = onQueryChange, placeholder = { Text("Busca tu auto ideal...", color = Color.Gray) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color(0xFF2970FF)), singleLine = true)
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.size(56.dp).background(Color(0xFF1E293B), RoundedCornerShape(16.dp)).clickable { onFilterClick() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tune, null, tint = Color.White) }
    }
}

@Composable
fun BrandsSection(marcaSeleccionada: String?, onMarcaClick: (String) -> Unit) {
    val marcas = listOf("Toyota", "Nissan", "Mazda", "Mitsubishi", "Honda", "Hyundai")
    Column(modifier = Modifier.padding(top = 28.dp)) {
        Text(text = "Marcas", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(marcas) { marca ->
                val estaSeleccionada = marcaSeleccionada == marca
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = if (estaSeleccionada) Color(0xFF2970FF) else Color.White, shadowElevation = 2.dp) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { onMarcaClick(marca) }) {
                            val logoRes = when (marca) { "Toyota" -> R.drawable.logo_toyota "Nissan" -> R.drawable.logo_nissan "Mazda" -> R.drawable.logo_mazda "Mitsubishi" -> R.drawable.logo_mitsubishi "Honda" -> R.drawable.logo_honda "Hyundai" -> R.drawable.logo_hyundai else -> null }
                            if (logoRes != null) { Image(painter = painterResource(id = logoRes), contentDescription = marca, modifier = Modifier.padding(12.dp).fillMaxSize(), contentScale = ContentScale.Fit) } else { Text(text = marca.take(1), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (estaSeleccionada) Color.White else Color(0xFF2970FF)) }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = marca, fontSize = 12.sp, fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.Medium, color = if (estaSeleccionada) Color(0xFF2970FF) else Color(0xFF475569))
                }
            }
        }
    }
}


@Composable
fun CarCardUsuario(vehiculo: Vehiculo, rating: Double, numResenas: Int, isFavorito: Boolean, onFavoritoClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(24.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                val decodedBitmap = remember(vehiculo.urlImagen) { if (vehiculo.urlImagen.isNotEmpty() && !vehiculo.urlImagen.startsWith("http")) { try { val imageBytes = Base64.decode(vehiculo.urlImagen, Base64.DEFAULT); BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) } catch (e: Exception) { null } } else null }
                if (vehiculo.urlImagen.startsWith("http")) { AsyncImage(model = vehiculo.urlImagen, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } else if (decodedBitmap != null) { Image(bitmap = decodedBitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } else { Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.DirectionsCar, null, tint = Color.LightGray) } }

                // Botón de Favorito
                Box(modifier = Modifier.padding(10.dp).size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).align(Alignment.TopEnd).clickable { onFavoritoClick() }, contentAlignment = Alignment.Center) { Icon(imageVector = if (isFavorito) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorito", modifier = Modifier.size(18.dp), tint = Color.Red) }

                // NUEVO: Etiqueta de estado si no está disponible
                if (vehiculo.estado != "Disponible") {
                    val colorEstado = if (vehiculo.estado == "En proceso") Color(0xFFF59E0B) else Color.Red
                    Box(modifier = Modifier.padding(10.dp).background(colorEstado, RoundedCornerShape(8.dp)).align(Alignment.TopStart).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(vehiculo.estado, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Text(" ${String.format(Locale.getDefault(), "%.1f", rating)} ($numResenas)", fontSize = 12.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(" El Salvador", fontSize = 11.sp, color = Color.Gray) }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Groups, null, tint = Color.Gray, modifier = Modifier.size(14.dp)); Text(" ${vehiculo.asientos} Asientos", fontSize = 11.sp, color = Color.Gray) }; Text(text = "${vehiculo.precio}/día", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF10B981)) }
            }
        }
    }
}

@Composable
fun SeccionRecordatorioCalificacion(reserva: Reserva, onCalificarClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2970FF).copy(alpha = 0.08f)), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(44.dp)); Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text("¿Cómo estuvo tu viaje?", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)); Text("Califica el ${reserva.vehiculoInfo}", fontSize = 13.sp, color = Color.Gray) }
            Button(onClick = onCalificarClick, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF))) { Text("Calificar") }
        }
    }
}