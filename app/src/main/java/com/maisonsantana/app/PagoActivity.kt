package com.maisonsantana.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

/**
 * Pantalla de pago.
 * Muestra el resumen del pedido, permite escribir la dirección o capturarla
 * con la ubicación del dispositivo y elegir el método de pago.
 * Al confirmar, abre la pasarela ePayco; si la transacción es ACEPTADA,
 * guarda la orden en la colección "ordenes" de Firestore con el número de
 * transacción y vacía el carrito.
 */
class PagoActivity : AppCompatActivity() {

    /** Datos del pedido que esperan la respuesta de la pasarela. */
    private data class Pedido(
        val items: List<List<String>>,
        val subtotal: Int,
        val envio: Int,
        val total: Int,
        val direccion: String,
        val metodo: String
    )

    // Servicios de Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Ubicación capturada (si el usuario usó el botón de ubicación)
    private var latitud: Double? = null
    private var longitud: Double? = null

    // Vistas y pedido pendiente de confirmación
    private lateinit var etDireccion: EditText
    private lateinit var btnProcesar: Button
    private var pedidoPendiente: Pedido? = null

    // Solicita el permiso de ubicación y, si se concede, obtiene la posición
    private val permisoUbicacion =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                obtenerUbicacion()
            } else {
                mostrar("Permiso de ubicación denegado. Escribe la dirección manualmente")
            }
        }

    // Recibe el resultado de la pasarela de pagos
    private val resultadoPasarela =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            val pedido = pedidoPendiente
            val datos = resultado.data
            if (pedido == null || datos == null) {
                mostrar("Pago cancelado")
                btnProcesar.isEnabled = true
                return@registerForActivityResult
            }
            val estado = datos.getStringExtra("estado") ?: "ERROR"
            val transaccion = datos.getStringExtra("transaccionId") ?: ""
            if (estado == "APPROVED") {
                guardarOrden(pedido, transaccion, estado)
            } else {
                mostrar("Pago no aprobado (estado: $estado)")
                btnProcesar.isEnabled = true
            }
        }

    /**
     * Se ejecuta al abrir la pantalla: calcula el resumen del carrito,
     * agrega el botón de ubicación y configura el botón de procesar el pago.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        val prefsCarrito = getSharedPreferences("carrito", Context.MODE_PRIVATE)
        val texto = prefsCarrito.getString("items", "") ?: ""
        val items = texto.split("\n").filter { it.isNotBlank() }.map { it.split("|") }

        // Resumen del pedido y subtotal
        var subtotal = 0
        val resumen = StringBuilder()
        for (item in items) {
            val precio = item[1].toInt()
            val cantidad = item[2].toInt()
            val color = item.getOrElse(4) { "Blanco" }
            subtotal += precio * cantidad
            resumen.append(item[0]).append(" (Talla ").append(item[3])
                .append(", ").append(color).append(", x").append(cantidad).append("): $")
                .append(String.format("%,d", precio * cantidad)).append(" COP\n")
        }

        val envio = if (items.isEmpty()) 0 else 15000
        val total = subtotal + envio

        findViewById<TextView>(R.id.tvProducto).text =
            if (items.isEmpty()) "Tu carrito está vacío" else resumen.toString().trim()
        findViewById<TextView>(R.id.tvEnvio).text =
            "Envío a Domicilio: $" + String.format("%,d", envio) + " COP"
        findViewById<TextView>(R.id.tvTotal).text =
            "TOTAL A PAGAR: $" + String.format("%,d", total) + " COP"

        etDireccion = findViewById(R.id.etDireccion)
        agregarBotonUbicacion()

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }

        // Confirmar el pago: valida y abre la pasarela ePayco
        btnProcesar = findViewById(R.id.btnProcesar)
        btnProcesar.setOnClickListener {
            val direccion = etDireccion.text.toString().trim()
            val idMetodo = findViewById<RadioGroup>(R.id.rgPago).checkedRadioButtonId

            if (items.isEmpty()) {
                mostrar("Tu carrito está vacío")
            } else if (direccion.isBlank()) {
                mostrar("Escribe la dirección de envío")
            } else if (idMetodo == -1) {
                mostrar("Selecciona un método de pago")
            } else {
                val metodo = findViewById<RadioButton>(idMetodo).text.toString()
                btnProcesar.isEnabled = false
                pedidoPendiente = Pedido(items, subtotal, envio, total, direccion, metodo)
                val intent = Intent(this, PagoEpaycoActivity::class.java)
                intent.putExtra("total", total)
                resultadoPasarela.launch(intent)
            }
        }

        // Barra de navegación inferior
        findViewById<TextView>(R.id.navHome).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navSearch).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            mostrar("Ya estás en el pago")
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    /**
     * CREAR: guarda la orden aprobada en Firestore con los productos, el valor,
     * la dirección, la ubicación (si existe), el método de pago, el número de
     * transacción de ePayco y la fecha. Vacía el carrito y abre el historial.
     */
    private fun guardarOrden(pedido: Pedido, transaccion: String, estadoPasarela: String) {
        val sesion = getSharedPreferences("sesion", Context.MODE_PRIVATE)

        // Lista de productos de la orden
        val productos = pedido.items.map {
            hashMapOf<String, Any>(
                "nombre" to it[0],
                "precio" to it[1].toInt(),
                "cantidad" to it[2].toInt(),
                "talla" to it[3],
                "color" to it.getOrElse(4) { "Blanco" }
            )
        }

        val orden = hashMapOf<String, Any>(
            "uid" to (auth.currentUser?.uid ?: ""),
            "correo" to (sesion.getString("correo", "") ?: ""),
            "nombre" to (sesion.getString("nombre", "") ?: ""),
            "items" to productos,
            "subtotal" to pedido.subtotal,
            "envio" to pedido.envio,
            "total" to pedido.total,
            "direccion" to pedido.direccion,
            "metodo" to pedido.metodo,
            "pasarela" to "ePayco",
            "transaccionId" to transaccion,
            "estadoPasarela" to estadoPasarela,
            "estado" to "Pagado",
            "fecha" to FieldValue.serverTimestamp()
        )
        // La ubicación solo se guarda si el usuario la capturó
        latitud?.let { orden["latitud"] = it }
        longitud?.let { orden["longitud"] = it }

        db.collection("ordenes").add(orden)
            .addOnSuccessListener {
                getSharedPreferences("carrito", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(this, "¡Pago aprobado! Transacción: $transaccion", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, HistorialActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                mostrar("No se pudo registrar el pedido: ${it.message}")
                btnProcesar.isEnabled = true
            }
    }

    /**
     * Agrega, debajo del campo de dirección, el botón "Usar mi ubicación actual".
     */
    private fun agregarBotonUbicacion() {
        val padre = etDireccion.parent as LinearLayout

        val boton = Button(this)
        boton.text = "📍 Usar mi ubicación actual"
        boton.isAllCaps = false
        boton.stateListAnimator = null
        boton.background = ContextCompat.getDrawable(this, R.drawable.bg_button_gray)
        boton.backgroundTintList = null
        boton.typeface = ResourcesCompat.getFont(this, R.font.bree_serif)
        boton.setTextColor(getColor(R.color.text_dark))

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (44 * resources.displayMetrics.density).toInt()
        )
        parametros.topMargin = (8 * resources.displayMetrics.density).toInt()
        boton.layoutParams = parametros

        boton.setOnClickListener { pedirUbicacion() }
        padre.addView(boton, padre.indexOfChild(etDireccion) + 1)
    }

    /** Si ya hay permiso de ubicación obtiene la posición; si no, lo solicita. */
    private fun pedirUbicacion() {
        val permiso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permiso == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Obtiene la ubicación actual del dispositivo con los servicios de Google
     * y la convierte en una dirección escrita.
     */
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {
        mostrar("Obteniendo tu ubicación...")
        val cliente = LocationServices.getFusedLocationProviderClient(this)
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { ubicacion ->
                if (ubicacion == null) {
                    mostrar("No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo")
                    return@addOnSuccessListener
                }
                latitud = ubicacion.latitude
                longitud = ubicacion.longitude
                convertirEnDireccion(ubicacion.latitude, ubicacion.longitude)
            }
            .addOnFailureListener {
                mostrar("No se pudo obtener la ubicación")
            }
    }

    /**
     * Convierte unas coordenadas en una dirección (geocodificación inversa)
     * y la escribe en el campo de dirección. Si falla, escribe las coordenadas.
     */
    private fun convertirEnDireccion(lat: Double, lng: Double) {
        Thread {
            val direccion = try {
                @Suppress("DEPRECATION")
                val lista = Geocoder(this, Locale("es", "CO")).getFromLocation(lat, lng, 1)
                lista?.firstOrNull()?.getAddressLine(0)
            } catch (e: Exception) {
                null
            }
            runOnUiThread {
                etDireccion.setText(direccion ?: "Lat $lat, Lng $lng")
            }
        }.start()
    }

    /** Muestra un mensaje corto en pantalla. */
    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}