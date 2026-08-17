package com.example.shieldrive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.shieldrive.ui.navegacion.ShielDriveApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Crear el canal del sistema operativo requerido por Android (Prioridad Alta)
        crearCanalNotificaciones(this)

        // 2. Suscribir reactivamente al canal según la sesión activa
        configurarSuscripcionTopics()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SolicitarPermisosIniciales()
                ShielDriveApp()
            }
        }
    }

    private fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "shieldrive_notifications"
            val channelName = "Notificaciones ShielDrive"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canal principal para alertas y reservas"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun configurarSuscripcionTopics() {
        val auth = FirebaseAuth.getInstance()
        val messaging = FirebaseMessaging.getInstance()

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                if (user.email.equals("admin@shieldrive.com", ignoreCase = true)) {
                    messaging.subscribeToTopic("admin").addOnSuccessListener {
                        println("Suscrito exitosamente al tópico: admin")
                    }
                } else {
                    messaging.subscribeToTopic("cliente_${user.uid}").addOnSuccessListener {
                        println("Suscrito exitosamente al tópico: cliente_${user.uid}")
                    }
                }
            }
        }
    }
}

@Composable
fun SolicitarPermisosIniciales() {
    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permisos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
        launcherPermisos.launch(permisos)
    }
}


fun lanzarNotificacionPush(titulo: String, mensaje: String, topicoDestino: String) {
    Thread {
        try {
            val url = java.net.URL("https://script.google.com/macros/s/AKfycby3bMtmPT16NwFQl7kAwSwiTZEEe_M778TtQqo-I56PYiQumFiLPyHsJUhBueIDMpqq/exec")
            val conexion = url.openConnection() as java.net.HttpURLConnection
            conexion.requestMethod = "POST"
            conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conexion.doOutput = true

            val json = """{"titulo": "$titulo", "mensaje": "$mensaje", "topico": "$topicoDestino"}"""

            conexion.outputStream.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            println("Respuesta servidor Push ($topicoDestino): ${conexion.responseCode}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}