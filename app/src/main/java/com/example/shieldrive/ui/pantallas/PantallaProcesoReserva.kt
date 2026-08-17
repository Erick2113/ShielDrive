package com.example.shieldrive.ui.pantallas

import android.Manifest
import com.example.shieldrive.ui.theme.*
import androidx.compose.material3.MaterialTheme
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shieldrive.model.Vehiculo
import com.example.shieldrive.lanzarNotificacionPush // IMPORTACIÓN DE TU MISIL SERVERLESS
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProcesoReserva(
    vehiculo: Vehiculo,
    onVolver: () -> Unit,
    onConfirmar: (nombre: String, telefono: String, documento: String, fechaInicio: String, fechaFin: String, idReserva: String, total: String) -> Unit,
    onCambiarEstadoVehiculo: (String) -> Unit
) {
    var pasoActual by remember { mutableIntStateOf(1) }
    var reservaCompletada by remember { mutableStateOf(false) }

    var tiempoRestante by remember { mutableIntStateOf(180) }
    val contexto = LocalContext.current

    var fechaInicioMillis by remember { mutableStateOf<Long?>(null) }
    var fechaFinMillis by remember { mutableStateOf<Long?>(null) }
    var mostrarCalendarioInicio by remember { mutableStateOf(false) }
    var mostrarCalendarioFin by remember { mutableStateOf(false) }
    var fechasBloqueadas by remember { mutableStateOf<List<LongRange>>(emptyList()) }

    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    LaunchedEffect(vehiculo.id) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Reservas")
            .whereEqualTo("vehiculoId", vehiculo.id)
            .get()
            .addOnSuccessListener { snapshot ->
                val rangosOcupados = mutableListOf<LongRange>()
                for (doc in snapshot.documents) {
                    val estado = doc.getString("estado") ?: ""
                    if (estado != "Finalizada" && estado != "Cancelada" && estado != "Rechazada") {
                        val strInicio = doc.getString("fechaInicio") ?: ""
                        val strFin = doc.getString("fechaFin") ?: ""
                        try {
                            val inicioMillis = formatoFecha.parse(strInicio)?.time ?: 0L
                            val finMillis = formatoFecha.parse(strFin)?.time ?: 0L
                            if (inicioMillis > 0 && finMillis > 0) {
                                rangosOcupados.add(inicioMillis..finMillis)
                            }
                        } catch (e: Exception) { }
                    }
                }
                fechasBloqueadas = rangosOcupados
            }
    }

    LaunchedEffect(key1 = pasoActual) {
        if (pasoActual < 3) {
            while (tiempoRestante > 0) {
                delay(1000L)
                tiempoRestante--
            }
            if (tiempoRestante == 0 && !reservaCompletada) {
                Toast.makeText(contexto, "Tiempo de reserva agotado.", Toast.LENGTH_LONG).show()
                onVolver()
            }
        }
    }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var numeroDocumento by remember { mutableStateOf("") }

    var estadoValidacion by remember { mutableStateOf("pendiente") }
    var mostrarDialogoOrigen by remember { mutableStateOf(false) }
    var idReservaGenerado by remember { mutableStateOf("") }

    val launcherGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            estadoValidacion = "analizando"
            procesarConGoogleMLKit(InputImage.fromFilePath(contexto, uri)) { esValido, docExtraido ->
                estadoValidacion = if (esValido) "aprobado" else "rechazado"
                if (esValido && docExtraido.isNotEmpty()) numeroDocumento = docExtraido
            }
        }
    }

    val launcherCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            estadoValidacion = "analizando"
            procesarConGoogleMLKit(InputImage.fromBitmap(bitmap, 0)) { esValido, docExtraido ->
                estadoValidacion = if (esValido) "aprobado" else "rechazado"
                if (esValido && docExtraido.isNotEmpty()) numeroDocumento = docExtraido
            }
        }
    }

    val launcherPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launcherCamara.launch(null) else Toast.makeText(contexto, "Se necesita permiso de cámara.", Toast.LENGTH_LONG).show()
    }

    val fechaInicioTexto = fechaInicioMillis?.let { formatoFecha.format(Date(it)) } ?: ""
    val fechaFinTexto = fechaFinMillis?.let { formatoFecha.format(Date(it)) } ?: ""

    var diasReserva = 0L
    if (fechaInicioMillis != null && fechaFinMillis != null) {
        val diff = fechaFinMillis!! - fechaInicioMillis!!
        diasReserva = TimeUnit.MILLISECONDS.toDays(diff)
        if (diasReserva == 0L) diasReserva = 1L
    }

    val precioNumerico = vehiculo.precio.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 0f
    val totalAPagar = precioNumerico * diasReserva

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color(0xFFF1F5F9),
        focusedBorderColor = Primary,
        cursorColor = Primary,
        focusedLabelColor = Primary,
        disabledTextColor = TextPrimary,
        disabledLabelColor = TextSecondary,
        disabledLeadingIconColor = TextSecondary,
        unfocusedLeadingIconColor = TextSecondary,
        focusedLeadingIconColor = Primary
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (pasoActual == 1) "Reserva" else if (pasoActual == 2) "Confirmación" else "Ticket", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { if (pasoActual != 3) IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                Button(
                    onClick = {
                        when (pasoActual) {
                            1 -> if (nombre.isNotBlank() && telefono.isNotBlank() && numeroDocumento.isNotBlank() && fechaInicioMillis != null && fechaFinMillis != null && estadoValidacion == "aprobado") pasoActual = 2
                            2 -> {
                                idReservaGenerado = "RES-" + (100000..999999).random().toString()
                                reservaCompletada = true
                                pasoActual = 3
                            }
                            3 -> {
                                // 1. Confirma la reserva en tu Firestore
                                onConfirmar(nombre, telefono, numeroDocumento, fechaInicioTexto, fechaFinTexto, idReservaGenerado, totalAPagar.toString())

                                // 2. 🔥 DISPARA LA ALERTA AL ADMINISTRADOR 🔥
                                lanzarNotificacionPush(
                                    titulo = "¡Nueva Reserva! \uD83D\uDE97",
                                    mensaje = "$nombre ha reservado el ${vehiculo.marca} ${vehiculo.modelo}.",
                                    topicoDestino = "admin"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(16.dp),
                    enabled = if (pasoActual == 1) nombre.isNotBlank() && telefono.isNotBlank() && numeroDocumento.isNotBlank() && fechaInicioMillis != null && fechaFinMillis != null && estadoValidacion == "aprobado" else true
                ) {
                    Text(
                        if (pasoActual == 1) "Continuar" else if (pasoActual == 2) "Generar Ticket" else "Finalizar y Volver",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pasoActual == 1 && !(nombre.isNotBlank() && telefono.isNotBlank() && numeroDocumento.isNotBlank() && fechaInicioMillis != null && fechaFinMillis != null && estadoValidacion == "aprobado")) TextSecondary else Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))

            if (pasoActual < 3) {
                val minutos = tiempoRestante / 60
                val segundos = tiempoRestante % 60
                Row(modifier = Modifier.fillMaxWidth().background(ErrorColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = ErrorColor); Spacer(modifier = Modifier.width(8.dp))
                    Text("Tiempo para reservar: ${String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)}", color = ErrorColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = TextPrimary); HorizontalDivider(modifier = Modifier.width(40.dp).padding(horizontal = 8.dp), color = TextPrimary, thickness = 2.dp)
                Icon(Icons.Rounded.CheckCircle, null, tint = if (pasoActual >= 2) TextPrimary else TextSecondary.copy(alpha = 0.5f))
                if (pasoActual == 3) { HorizontalDivider(modifier = Modifier.width(40.dp).padding(horizontal = 8.dp), color = TextPrimary, thickness = 2.dp); Icon(Icons.Rounded.QrCodeScanner, null, tint = SuccessColor) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (pasoActual == 1) {
                Text("Datos Personales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = textFieldColors)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, leadingIcon = { Icon(Icons.Rounded.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = textFieldColors)

                    OutlinedTextField(
                        value = numeroDocumento,
                        onValueChange = { },
                        label = { Text("DUI/Licencia") },
                        leadingIcon = { Icon(Icons.Rounded.Badge, null) },
                        readOnly = true,
                        enabled = false,
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Fechas de Alquiler", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { mostrarCalendarioInicio = true }) { OutlinedTextField(value = fechaInicioTexto, onValueChange = { }, label = { Text("Inicio") }, leadingIcon = { Icon(Icons.Rounded.DateRange, null) }, readOnly = true, enabled = false, colors = textFieldColors, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                    Box(modifier = Modifier.weight(1f).clickable { if (fechaInicioMillis != null) mostrarCalendarioFin = true else Toast.makeText(contexto, "Elige la fecha de Inicio primero", Toast.LENGTH_LONG).show() }) { OutlinedTextField(value = fechaFinTexto, onValueChange = { }, label = { Text("Fin") }, leadingIcon = { Icon(Icons.Rounded.DateRange, null) }, readOnly = true, enabled = false, colors = textFieldColors, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Verificación de Identidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (estadoValidacion) {
                            "pendiente" -> OutlinedButton(onClick = { mostrarDialogoOrigen = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)) { Icon(Icons.Rounded.VerifiedUser, null); Spacer(modifier = Modifier.width(8.dp)); Text("Escanear DUI / Licencia") }
                            "analizando" -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Primary); Spacer(modifier = Modifier.width(12.dp)); Text("Analizando texto...", fontWeight = FontWeight.Medium, color = TextPrimary) }
                            "aprobado" -> Row(modifier = Modifier.fillMaxWidth().background(SuccessColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.CheckCircle, null, tint = SuccessColor); Spacer(modifier = Modifier.width(8.dp)); Text("¡Identidad Verificada!", fontWeight = FontWeight.Bold, color = Color(0xFF047857)) }
                                TextButton(onClick = { estadoValidacion = "pendiente"; numeroDocumento = "" }, colors = ButtonDefaults.textButtonColors(contentColor = Primary)) { Text("Cambiar") }
                            }
                            "rechazado" -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text("DOCUMENTO INVÁLIDO", fontWeight = FontWeight.Bold, color = ErrorColor); Button(onClick = { mostrarDialogoOrigen = true }, colors = ButtonDefaults.buttonColors(containerColor = ErrorColor), modifier = Modifier.padding(top=8.dp)) { Text("Intentar de nuevo", color = Color.White) } }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else if (pasoActual == 2) {
                Text("Resumen de Reserva", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("Fechas: $fechaInicioTexto al $fechaFinTexto ($diasReserva días)", color = TextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp)); HorizontalDivider(color = Color(0xFFE2E8F0)); Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Cliente:", color = TextSecondary); Text(nombre, fontWeight = FontWeight.Medium, color = TextPrimary) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Documento:", color = TextSecondary); Text(numeroDocumento, fontWeight = FontWeight.Bold, color = TextPrimary) }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Total a pagar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary); Text("$${String.format(Locale.getDefault(), "%.2f", totalAPagar)}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = SuccessColor) }
                Spacer(modifier = Modifier.height(40.dp))
            } else if (pasoActual == 3) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡Reserva Exitosa!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = SuccessColor)
                Text("Presenta este código al recoger el vehículo", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TICKET DE RESERVA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 2.sp); Spacer(modifier = Modifier.height(8.dp))
                        Text(idReservaGenerado, fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrimary); Spacer(modifier = Modifier.height(24.dp))
                        val qrBitmap = generarQR(idReservaGenerado)
                        if (qrBitmap != null) Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(modifier = Modifier.height(24.dp)); HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 2.dp); Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Vehículo:", color = TextSecondary, fontSize = 14.sp); Text("${vehiculo.marca} ${vehiculo.modelo}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary) }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (mostrarDialogoOrigen) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoOrigen = false },
            containerColor = Color.White,
            title = { Text("Subir Documento", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Elige desde dónde quieres subir la foto.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoOrigen = false; launcherPermisoCamara.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Cámara", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoOrigen = false; launcherGaleria.launch("image/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) { Text("Galería") }
            }
        )
    }

    val hoyUTC = remember {
        java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }


    if (mostrarCalendarioInicio) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaInicioMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (utcTimeMillis < hoyUTC) return false
                    return fechasBloqueadas.none { utcTimeMillis in it }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendarioInicio = false },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            confirmButton = {
                TextButton(
                    onClick = {
                        fechaInicioMillis = datePickerState.selectedDateMillis
                        if (fechaFinMillis != null && fechaFinMillis!! < fechaInicioMillis!!) {
                            fechaFinMillis = null
                        }
                        mostrarCalendarioInicio = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary)
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarCalendarioInicio = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text("Cancelar") }
            }
        )
        {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Primary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = Primary,
                    selectedYearContainerColor = Primary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = Primary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = Primary,
                    todayDateBorderColor = Primary
                )
            )
        }
    }


    if (mostrarCalendarioFin) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaFinMillis ?: fechaInicioMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val inicio = fechaInicioMillis ?: return false
                    if (utcTimeMillis < inicio) return false
                    val proximaReservaBloqueada = fechasBloqueadas
                        .map { it.first }
                        .filter { it > inicio }
                        .minOrNull()

                    if (proximaReservaBloqueada != null && utcTimeMillis >= proximaReservaBloqueada) {
                        return false
                    }
                    return true
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendarioFin = false },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            confirmButton = {
                TextButton(
                    onClick = {
                        fechaFinMillis = datePickerState.selectedDateMillis
                        mostrarCalendarioFin = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary)
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarCalendarioFin = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Primary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = Primary,
                    selectedYearContainerColor = Primary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = Primary,
                    selectedDayContentColor = Color.White,
                    todayContentColor = Primary,
                    todayDateBorderColor = Primary
                )
            )
        }
    }
}

fun procesarConGoogleMLKit(image: InputImage, onResultado: (esValido: Boolean, docExtraido: String) -> Unit) {
    try {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener { visionText ->
            val texto = visionText.text.lowercase(Locale.getDefault())

            val matchDui = Regex("\\d{8}[-\\s]?\\d").find(texto)
            val palabrasClave = listOf("licencia", "conducir", "salvador", "dui", "documento único", "república")
            val tienePalabraClave = palabrasClave.any { texto.contains(it) }

            if (matchDui != null) {
                val duiLimpio = matchDui.value.replace(" ", "").replace("-", "")
                val duiFormateado = if (duiLimpio.length == 9) "${duiLimpio.substring(0,8)}-${duiLimpio.substring(8)}" else matchDui.value
                onResultado(true, duiFormateado)
            } else if (tienePalabraClave) {
                onResultado(true, "Documento Verificado")
            } else {
                onResultado(false, "")
            }
        }.addOnFailureListener { onResultado(false, "") }
    } catch (e: Exception) {
        onResultado(false, "")
    }
}

fun generarQR(contenido: String): Bitmap? {
    return try {
        val size = 512
        val bitMatrix = MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        bmp
    } catch (e: Exception) { null }
}