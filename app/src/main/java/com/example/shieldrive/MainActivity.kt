package com.example.shieldrive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationCompat
import com.example.shieldrive.ui.navegacion.ShielDriveApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        iniciarEscuchadorGlobal(this)

        setContent {
            ShielDriveApp()
        }
    }


    private fun iniciarEscuchadorGlobal(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()


        val tiempoAppIniciada = System.currentTimeMillis()


        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {

                db.collection("notificaciones")
                    .whereEqualTo("usuarioId", uid)
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setSound(sonido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()


        notificationManager.notify(Random.nextInt(), notificacion)
    }
}