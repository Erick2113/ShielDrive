package com.example.shieldrive.ui.pantallas.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.viewmodel.FlotaViewModel
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarVehiculo(onVolver: () -> Unit, flotaViewModel: FlotaViewModel = viewModel()) {
    var pasoActual by remember { mutableIntStateOf(1) }
    val totalPasos = 3
    val progreso = pasoActual.toFloat() / totalPasos.toFloat()

    // Datos del Vehículo (Incluyendo Motor y Transmisión)
    var vin by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }
    var numAsientos by remember { mutableStateOf("5") }
    var transmision by remember { mutableStateOf("") }
    var motor by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Estados de UI
    var buscando by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    var errorVin by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { imageUri = uri }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Nuevo Vehículo", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onVolver, enabled = !guardando) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF2970FF),
                    trackColor = Color(0xFFF1F5F9)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "PASO $pasoActual DE $totalPasos",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2970FF),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AnimatedContent(
                targetState = pasoActual,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "PasoAnimacion"
            ) { paso ->
                when (paso) {
                    1 -> {
                        Column {
                            Text("Identificación VIN", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Escanea o escribe el VIN para autocompletar la ficha técnica.", color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                            OutlinedTextField(
                                value = vin,
                                onValueChange = { vin = it.uppercase(); errorVin = false },
                                label = { Text("Número VIN") },
                                placeholder = { Text("17 caracteres...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                modifier = Modifier.fillMaxWidth(),
                                isError = errorVin,
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            if (errorVin) {
                                Text("No encontramos este vehículo. Verifica el VIN.", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    buscando = true

                                    coroutineScope.launch {
                                        try {
                                            val url = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevin/$vin?format=json"
                                            val respuestaJson = withContext(Dispatchers.IO) { URL(url).readText() }
                                            val json = JSONObject(respuestaJson)
                                            val resultados = json.getJSONArray("Results")

                                            var mEncontrada = ""
                                            var modEncontrado = ""
                                            var aEncontrado = ""
                                            var asieEncontrado = "5"
                                            var transEncontrada = ""
                                            var motorEncontrado = ""

                                            for (i in 0 until resultados.length()) {
                                                val item = resultados.getJSONObject(i)
                                                val variable = item.optString("Variable")
                                                val valor = item.optString("Value")

                                                if (valor == "null" || valor.isNullOrEmpty()) continue

                                                when (variable) {
                                                    "Make" -> mEncontrada = valor
                                                    "Model" -> modEncontrado = valor
                                                    "Model Year" -> aEncontrado = valor
                                                    "Seats", "Number of Seats" -> asieEncontrado = valor
                                                    "Transmission Style", "Transmission Type" -> transEncontrada = valor
                                                    "Engine Configuration", "Displacement (L)" -> {
                                                        motorEncontrado = if (motorEncontrado.isEmpty()) valor else "$motorEncontrado $valor"
                                                    }
                                                }
                                            }

                                            if (mEncontrada.isNotEmpty()) {
                                                marca = mEncontrada
                                                modelo = modEncontrado
                                                anio = aEncontrado
                                                numAsientos = asieEncontrado
                                                if (transEncontrada.isNotEmpty()) transmision = transEncontrada
                                                if (motorEncontrado.isNotEmpty()) motor = motorEncontrado
                                                buscando = false
                                                pasoActual = 2
                                            } else {
                                                buscando = false
                                                errorVin = true
                                            }
                                        } catch (e: Exception) {
                                            buscando = false
                                            errorVin = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF)),
                                enabled = vin.isNotEmpty() && !buscando
                            ) {
                                if (buscando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text("Buscar Datos", fontWeight = FontWeight.Bold)
                            }

                            TextButton(onClick = { pasoActual = 2 }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text("Omitir y llenar manualmente", color = Color.Gray)
                            }
                        }
                    }
                    2 -> {
                        Column {
                            Text("Detalles técnicos", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Sube una foto clara y verifica los datos del auto.", color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUri != null) {
                                    AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
                                        Text("Añadir Foto", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), shape = RoundedCornerShape(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = anio, onValueChange = { anio = it }, label = { Text("Año") }, modifier = Modifier.weight(1f).padding(end = 8.dp), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = numAsientos, onValueChange = { numAsientos = it }, label = { Text("Asientos") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }

                            Spacer(modifier = Modifier.height(12.dp))


                            OutlinedTextField(value = transmision, onValueChange = { transmision = it }, label = { Text("Transmisión (Ej. Automática)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            OutlinedTextField(value = motor, onValueChange = { motor = it }, label = { Text("Motor (Ej. V6 3.0L)") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(12.dp))

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { pasoActual = 1 }, modifier = Modifier.weight(1f)) { Text("Atrás", color = Color.Gray) }
                                Button(
                                    onClick = { pasoActual = 3 },
                                    modifier = Modifier.weight(2f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2970FF)),
                                    enabled = marca.isNotEmpty() && modelo.isNotEmpty()
                                ) { Text("Siguiente", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                    3 -> {
                        Column {
                            Text("Precio y descripción", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Establece la tarifa y lo que hace especial a este vehículo.", color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

                            OutlinedTextField(
                                value = precio,
                                onValueChange = { precio = it },
                                label = { Text("Precio por día") },
                                prefix = { Text("$ ") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción") },
                                modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 16.dp),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { pasoActual = 2 }, enabled = !guardando, modifier = Modifier.weight(1f)) { Text("Atrás", color = Color.Gray) }
                                Button(
                                    onClick = {
                                        guardando = true
                                        val v = Vehiculo(
                                            vin = vin,
                                            marca = marca,
                                            modelo = modelo,
                                            anio = anio,
                                            precio = "$" + precio,
                                            asientos = numAsientos,
                                            transmision = transmision,
                                            motor = motor,
                                            descripcion = descripcion
                                        )
                                        flotaViewModel.guardarConFotoBase64(context, imageUri, v) { exito ->
                                            guardando = false
                                            if (exito) onVolver()
                                        }
                                    },
                                    modifier = Modifier.weight(2f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    enabled = precio.isNotEmpty() && !guardando
                                ) {
                                    if (guardando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    else Text("Guardar Vehículo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}