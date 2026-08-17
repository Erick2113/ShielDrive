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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shieldrive.ui.theme.PrimaryDark
import com.example.shieldrive.ui.theme.PrimaryBlue
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


    val textFieldColorsBlancos = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color.White,
        focusedLeadingIconColor = Color.White,
        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
        focusedTrailingIconColor = Color.White,
        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.6f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryDark, PrimaryBlue)))
            .imePadding()
            .padding(top = 50.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "ShielDrive",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(8.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(32.dp))


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
                            color = Color.White
                        )
                        Text(
                            text = "Ingresa tus datos de contacto para tu perfil.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        OutlinedTextField(
                            value = correo,
                            onValueChange = { correo = it },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = textFieldColorsBlancos
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = { Text("Teléfono móvil") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = "Teléfono") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = textFieldColorsBlancos
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { pasoActual = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.5f)
                            ),
                            enabled = correoValido && telefonoValido
                        ) {
                            Text(
                                "Siguiente",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,

                                color = if(correoValido && telefonoValido) PrimaryDark else Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("¿Ya tienes cuenta? ", color = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.CenterVertically))
                            TextButton(
                                onClick = onIrALogin,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text("Inicia sesión", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Seguridad",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "Crea tu usuario y una contraseña robusta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        OutlinedTextField(
                            value = usuario,
                            onValueChange = { usuario = it },
                            label = { Text("Nombre de usuario") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = "Usuario") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = textFieldColorsBlancos
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Candado") },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Ver contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = textFieldColorsBlancos
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = confirmarContrasena,
                            onValueChange = { confirmarContrasena = it },
                            label = { Text("Confirmar Contraseña") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Candado") },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Ver contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = textFieldColorsBlancos
                        )

                        Spacer(modifier = Modifier.height(24.dp))


                        Text(text = "Tu contraseña debe contener:", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                        Spacer(modifier = Modifier.height(12.dp))
                        RequisitoCheck(texto = "Mínimo de 8 caracteres", cumplido = tieneLongitud)
                        RequisitoCheck(texto = "Letras minúsculas y mayúsculas", cumplido = tieneMayusMinus)
                        RequisitoCheck(texto = "Al menos 1 número", cumplido = tieneNumero)
                        RequisitoCheck(texto = "Al menos 1 símbolo", cumplido = tieneSimbolo)
                        RequisitoCheck(texto = "Las contraseñas coinciden", cumplido = contrasenasCoinciden)

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = {
                                if (contrasena != confirmarContrasena) {
                                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                viewModel.registrarUsuario(
                                    correo = correo,
                                    contrasena = contrasena,
                                    nombre = usuario,
                                    telefono = telefono,
                                    onExito = { esAdmin ->
                                        Toast.makeText(context, "¡Bienvenido a ShielDrive!", Toast.LENGTH_LONG).show()
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
                                containerColor = Color.White,
                                disabledContainerColor = Color.White.copy(alpha = 0.5f)
                            ),
                            enabled = !cargando && esContrasenaValida && usuarioValido && contrasenasCoinciden
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = PrimaryDark, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    "Finalizar Registro",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if(esContrasenaValida && usuarioValido && contrasenasCoinciden) PrimaryDark else Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { pasoActual = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("← Volver al paso anterior", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RequisitoCheck(texto: String, cumplido: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (cumplido) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (cumplido) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = if (cumplido) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (cumplido) FontWeight.Bold else FontWeight.Normal
        )
    }
}