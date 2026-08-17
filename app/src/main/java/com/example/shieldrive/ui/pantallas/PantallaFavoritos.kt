package com.example.shieldrive.ui.pantallas

import androidx.compose.foundation.background
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.shieldrive.ui.navegacion.Ruta
import com.example.shieldrive.viewmodel.FavoritosViewModel
import com.example.shieldrive.viewmodel.FlotaViewModel
import com.example.shieldrive.viewmodel.ResenasGlobalViewModel

@Composable
fun PantallaFavoritos(
    navController: NavController,
    favoritosViewModel: FavoritosViewModel = viewModel(),
    flotaViewModel: FlotaViewModel = viewModel(),
    resenasViewModel: ResenasGlobalViewModel = viewModel() // AGREGAMOS EL VIEWMODEL DE RESEÑAS
) {
    val todosLosCarros = flotaViewModel.listaVehiculos
    val misFavoritos = favoritosViewModel.getVehiculosFavoritos(todosLosCarros)

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(16.dp)) {

        Text("Mis Favoritos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (misFavoritos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes vehículos en favoritos.", color = TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(misFavoritos) { vehiculo ->


                    val resenasDelAuto = resenasViewModel.obtenerResenasPorVehiculo(vehiculo.id)
                    val numResenas = resenasDelAuto.size
                    val ratingReal = if (numResenas > 0) resenasDelAuto.sumOf { it.estrellas }.toDouble() / numResenas else 0.0

                    CarCardUsuario(
                        vehiculo = vehiculo,
                        rating = ratingReal,
                        numResenas = numResenas,
                        isFavorito = true,
                        onFavoritoClick = { favoritosViewModel.toggleFavorito(vehiculo) },
                        onClick = { navController.navigate(Ruta.DetalleVehiculo.crearRuta(vehiculo.id)) }
                    )
                }
            }
        }
    }
}