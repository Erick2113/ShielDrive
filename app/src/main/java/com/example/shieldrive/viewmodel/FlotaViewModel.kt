package com.example.shieldrive.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shieldrive.model.Vehiculo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL

class FlotaViewModel : ViewModel() {
    val listaVehiculos = mutableStateListOf<Vehiculo>()
    private val db = FirebaseFirestore.getInstance()

    init {
        cargarVehiculosDesdeFirebase()
    }

    private fun cargarVehiculosDesdeFirebase() {
        db.collection("flota").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                listaVehiculos.clear()
                for (documento in snapshot.documents) {
                    val vehiculo: Vehiculo? = documento.toObject(Vehiculo::class.java)
                    if (vehiculo != null) {
                        listaVehiculos.add(vehiculo.copy(id = documento.id))
                    }
                }
            }
        }
    }

    fun decodificarVIN(vin: String, onExito: (String, String, String, String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val url = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevin/$vin?format=json"
                val respuestaJson = withContext(Dispatchers.IO) { URL(url).readText() }
                val json = JSONObject(respuestaJson)
                val resultados = json.getJSONArray("Results")

                var marca = ""
                var modelo = ""
                var anio = ""
                var asientos = "5"

                for (i in 0 until resultados.length()) {
                    val item = resultados.getJSONObject(i)
                    val variable = item.optString("Variable")
                    val valor = item.optString("Value")

                    if (valor == "null" || valor.isNullOrEmpty()) continue

                    when (variable) {
                        "Make" -> marca = valor
                        "Model" -> modelo = valor
                        "Model Year" -> anio = valor
                        "Seats", "Number of Seats" -> asientos = valor
                    }
                }

                if (marca.isNotEmpty()) onExito(marca, modelo, anio, asientos) else onError()
            } catch (e: Exception) { onError() }
        }
    }

    fun guardarConFotoBase64(context: Context, uri: Uri?, vehiculo: Vehiculo, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var vehiculoFinal = vehiculo

            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Procesando imagen para subida rápida...", android.widget.Toast.LENGTH_LONG).show()
            }

            try {
                if (uri != null) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }

                    var sampleSize = 1
                    while (options.outWidth / sampleSize > 500 || options.outHeight / sampleSize > 500) { sampleSize *= 2 }

                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = context.contentResolver.openInputStream(uri).use {
                        BitmapFactory.decodeStream(it, null, decodeOptions)
                    }

                    if (bitmap != null) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val resized = Bitmap.createScaledBitmap(bitmap, 500, (500 / ratio).toInt(), true)

                        val outputStream = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.JPEG, 20, outputStream)
                        val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                        vehiculoFinal = vehiculo.copy(urlImagen = base64String)
                    }
                }

                withContext(Dispatchers.Main) {
                    guardarVehiculoEnFirebase(context, vehiculoFinal, onResultado)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    onResultado(false)
                }
            }
        }
    }

    fun guardarVehiculoEnFirebase(context: Context, vehiculo: Vehiculo, onResultado: (Boolean) -> Unit) {
        val coleccion = db.collection("flota")
        val docRef = if (vehiculo.id.isNotEmpty()) coleccion.document(vehiculo.id) else coleccion.document()
        val vehiculoConId = vehiculo.copy(id = docRef.id)

        docRef.set(vehiculoConId)
            .addOnSuccessListener {
                android.widget.Toast.makeText(context, "¡Guardado con éxito!", android.widget.Toast.LENGTH_LONG).show()
                onResultado(true)
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(context, "Error de red: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                onResultado(false)
            }
    }


    fun actualizarEstadoVehiculo(vehiculoId: String, nuevoEstado: String) {
        db.collection("flota").document(vehiculoId)
            .update("estado", nuevoEstado)
    }
}