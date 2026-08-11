package com.example.shieldrive.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shieldrive.viewmodel.AuthViewModel

@Composable
fun PantallaLogin(
    viewModel: AuthViewModel = viewModel(),
    onLoginExitoso: (esAdmin: Boolean) -> Unit,
    onIrARegistro: () -> Unit,
    onIrARecuperar: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }

    val cargando by viewModel.cargando.collectAsState()
    val context = LocalContext.current
    val camposLlenos = correo.isNotBlank() && contrasena.isNotBlank()

    val colorPrimario = Color(0xFF2970FF)
    val colorFondo = Color(0xFFFFFFFF)
    val colorGrisClaro = Color(0xFFF3F4F6)
    val colorNegroClaro = Color(0xFF4B5563)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .imePadding()
            .padding(top = 50.dp, bottom = 16.dp)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "ShielDrive",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = colorPrimario,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "¡Bienvenido de nuevo!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.Black
        )

        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Candado", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (contrasenaVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            trailingIcon = {
                val imagen = if (contrasenaVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { contrasenaVisible = !contrasenaVisible }) {
                    Icon(imageVector = imagen, contentDescription = "Alternar visibilidad", tint = Color.Gray)
                }
            }
        )


        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(
                onClick = onIrARecuperar
            ) {
                Text(text = "¿Olvidaste tu contraseña?", color = colorNegroClaro, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (camposLlenos) {
                    viewModel.iniciarSesion(
                        correo = correo,
                        contrasena = contrasena,
                        onExito = { esAdmin ->
                            Toast.makeText(context, "¡Sesión iniciada!", Toast.LENGTH_SHORT).show()
                            onLoginExitoso(esAdmin)
                        },
                        onError = { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            enabled = !cargando && camposLlenos,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorPrimario,
                disabledContainerColor = colorGrisClaro
            )
        ) {
            if (cargando) {
                CircularProgressIndicator(color = colorPrimario, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Iniciar Sesión",
                    color = if (camposLlenos) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿No tienes cuenta? ", color = colorNegroClaro)
            TextButton(onClick = onIrARegistro) {
                Text(text = "Regístrate aquí", color = colorNegroClaro, fontWeight = FontWeight.Bold)
            }
        }
    }
}