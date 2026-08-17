package com.example.shieldrive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
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
import androidx.core.app.NotificationCompat
import com.example.shieldrive.ui.navegacion.ShielDriveApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    // Variable para controlar que no se dupliquen las alertas
    private var escuchadorNotificaciones: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        iniciarEscuchadorGlobal(this)

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

    private fun iniciarEscuchadorGlobal(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Memoria interna del teléfono para guardar el UID
        val preferencias = context.getSharedPreferences("MemoriaShielDrive", Context.MODE_PRIVATE)

        val tiempoAppIniciada = System.currentTimeMillis()

        auth.addAuthStateListener { firebaseAuth ->
            val uidActual = firebaseAuth.currentUser?.uid

            // Si hay sesión iniciada, guardamos su UID en la memoria del teléfono
            if (uidActual != null) {
                preferencias.edit().putString("ULTIMO_UID_ACTIVO", uidActual).apply()
            }

            // TRUCO: Buscamos el UID actual, y si cerró sesión, sacamos el que guardamos en memoria
            val uidParaEscuchar = uidActual ?: preferencias.getString("ULTIMO_UID_ACTIVO", null)

            if (uidParaEscuchar != null) {
                // Borramos el escuchador anterior por si cambió de cuenta para que no suene doble
                escuchadorNotificaciones?.remove()

                // Iniciamos la escucha ininterrumpida
                escuchadorNotificaciones = db.collection("notificaciones")
                    .whereEqualTo("usuarioId", uidParaEscuchar)
                    .whereEqualTo("leida", false)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener

                        for (cambio in snapshot.documentChanges) {
                            if (cambio.type == DocumentChange.Type.ADDED) {
                                val fechaNotif = cambio.document.getLong("fecha") ?: 0L

                                if (fechaNotif >= tiempoAppIniciada) {
                                    val titulo = cambio.document.getString("titulo") ?: "Nueva Alerta"
                                    val mensaje = cambio.document.getString("mensaje") ?: "Abre ShielDrive para ver los detalles."

                                    mostrarNotificacionTelefono(context, titulo, mensaje)
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun mostrarNotificacionTelefono(context: Context, titulo: String, mensaje: String) {
        val channelId = "shieldrive_alertas_global"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alertas ShielDrive", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificacion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setSound(sonido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(Random.nextInt(), notificacion)
    }
}

@Composable
fun SolicitarPermisosIniciales() {
    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val notifConcedida = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true

        if (notifConcedida) {
            FirebaseMessaging.getInstance().subscribeToTopic("general")
        }
    }

    LaunchedEffect(Unit) {
        val permisosASolicitar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
        launcherPermisos.launch(permisosASolicitar)
    }
}