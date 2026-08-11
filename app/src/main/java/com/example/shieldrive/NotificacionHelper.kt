package com.example.shieldrive

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.shieldrive.R

object NotificacionHelper {
    private const val CHANNEL_ID = "canal_reservas_shieldrive"

    fun mostrarNotificacion(context: Context, titulo: String, mensaje: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // En Android 8.0 (Oreo) o superior, es OBLIGATORIO crear un canal de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Actualizaciones de Reserva",
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que suene y salga el banner emergente
            )
            manager.createNotificationChannel(channel)
        }

        // Construimos la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este ícono exista en tus recursos (drawable)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Lanzamos la notificación con un ID único para que no se sobreescriban si llegan varias
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}