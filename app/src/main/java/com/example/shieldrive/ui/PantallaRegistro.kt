package com.example.shieldrive.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
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
fun PantallaRegistro(
    viewModel: AuthViewModel = viewModel(),
    onRegistroExitoso: (Boolean) -> Unit,
    onIrALogin: () -> Unit
) {

    var pasoActual by remember { mutableIntStateOf(1) }


    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }

    var contrasena by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }


    var confirmarContrasena by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val cargando by viewModel.cargando.collectAsState()
    val context = LocalContext.current


    val correoValido = correo.isNotBlank() && correo.contains("@")
    val telefonoValido = telefono.length >= 8


    val tieneLongitud = contrasena.length >= 8
    val tieneMayusMinus = contrasena.any { it.isUpperCase() } && contrasena.any { it.isLowerCase() }
    val tieneNumero = contrasena.any { it.isDigit() }
    val tieneSimbolo = contrasena.any { !it.isLetterOrDigit() }
    val esContrasenaValida = tieneLongitud && tieneMayusMinus && tieneNumero && tieneSimbolo
    val usuarioValido = usuario.isNotBlank()


    val contrasenasCoinciden = contrasena.isNotEmpty() && contrasena == confirmarContrasena

    val progreso = if (pasoActual == 1) 0.5f else 1f


    val colorPrimario = Color(0xFF2970FF)
    val colorFondo = Color(0xFFFFFFFF)
    val colorGrisClaro = Color(0xFFF3F4F6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .imePadding() // Sube el contenido cuando aparece el teclado
            // Añadimos 50.dp de padding superior para que la barra NO quede muy arriba
            .padding(top = 50.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Text(
            text = "ShielDrive",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = colorPrimario,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))


        LinearProgressIndicator(
            progress = progreso,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp),
            color = colorPrimario,
            trackColor = colorGrisClaro,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Contenido scrolleable
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = pasoActual,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "TransicionPasos"
            ) { paso ->
                if (paso == 1) {

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "¡Comencemos!",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.Black
                        )
                        Text(
                            text = "Ingresa tus datos de contacto para tu perfil.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email", tint = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = { Text("Teléfono móvil") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = "Teléfono", tint = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { pasoActual = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPrimario,
                                disabledContainerColor = colorGrisClaro
                            ),
                            enabled = correoValido && telefonoValido
                        ) {
                            Text("Siguiente", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(correoValido && telefonoValido) Color.White else Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("¿Ya tienes cuenta? ", color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                            TextButton(onClick = onIrALogin) {
                                Text("Inicia sesión", color = colorPrimario, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Seguridad",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.Black
                        )
                        Text(
                            text = "Crea tu usuario y una contraseña robusta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        OutlinedTextField(
                            value = usuario,
                            onValueChange = { usuario = it },
                            label = { Text("Nombre de usuario") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = "Usuario", tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Candado", tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Ver contraseña",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = confirmarContrasena,
                            onValueChange = { confirmarContrasena = it },
                            label = { Text("Confirmar Contraseña") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Candado", tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Ver contraseña",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))


                        Text(text = "Tu contraseña debe contener:", style = MaterialTheme.typography.labelLarge, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        RequisitoCheck(texto = "Mínimo de 8 caracteres", cumplido = tieneLongitud, colorPrimario = colorPrimario)
                        RequisitoCheck(texto = "Letras minúsculas y mayúsculas", cumplido = tieneMayusMinus, colorPrimario = colorPrimario)
                        RequisitoCheck(texto = "Al menos 1 número", cumplido = tieneNumero, colorPrimario = colorPrimario)
                        RequisitoCheck(texto = "Al menos 1 símbolo", cumplido = tieneSimbolo, colorPrimario = colorPrimario)

                        // NUEVO: Requisito de coincidencia
                        RequisitoCheck(texto = "Las contraseñas coinciden", cumplido = contrasenasCoinciden, colorPrimario = colorPrimario)

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = {

                                if (contrasena != confirmarContrasena) {
                                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.registrarUsuario(
                                    correo = correo,
                                    contrasena = contrasena,
                                    nombre = usuario,
                                    telefono = telefono,
                                    onExito = { esAdmin ->
                                        Toast.makeText(context, "¡Bienvenido a ShielDrive!", Toast.LENGTH_SHORT).show()
                                        onRegistroExitoso(esAdmin)
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPrimario,
                                disabledContainerColor = colorGrisClaro
                            ),

                            enabled = !cargando && esContrasenaValida && usuarioValido && contrasenasCoinciden
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = colorPrimario, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Finalizar Registro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if(esContrasenaValida && usuarioValido && contrasenasCoinciden) Color.White else Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = { pasoActual = 1 }, modifier = Modifier.fillMaxWidth()) {
                            Text("← Volver al paso anterior", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequisitoCheck(texto: String, cumplido: Boolean, colorPrimario: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (cumplido) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (cumplido) colorPrimario else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (cumplido) Color.Black else Color.Gray,
            fontWeight = if (cumplido) FontWeight.Medium else FontWeight.Normal
        )
    }
}