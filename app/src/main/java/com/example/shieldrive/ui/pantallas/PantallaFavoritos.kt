package com.example.shieldrive.ui.pantallas

import androidx.compose.foundation.background
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

@Composable
fun PantallaFavoritos(
    navController: NavController,
    favoritosViewModel: FavoritosViewModel = viewModel(),
    flotaViewModel: FlotaViewModel = viewModel()
) {

    val todosLosCarros = flotaViewModel.listaVehiculos
    val misFavoritos = favoritosViewModel.getVehiculosFavoritos(todosLosCarros)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(16.dp)) {

        Text("Mis Favoritos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        if (misFavoritos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no tienes vehículos en favoritos.", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(misFavoritos) { vehiculo ->

                    CarCardUsuario(
                        vehiculo = vehiculo,
                        rating = vehiculo.rating,
                        numResenas = vehiculo.numResenas,
                        isFavorito = true,
                        onFavoritoClick = { favoritosViewModel.toggleFavorito(vehiculo) },
                        onClick = { navController.navigate(Ruta.DetalleVehiculo.crearRuta(vehiculo.id)) }
                    )
                }
            }
        }
    }
}