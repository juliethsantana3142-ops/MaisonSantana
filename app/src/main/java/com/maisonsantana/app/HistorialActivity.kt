package com.maisonsantana.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Historial de pedidos del Comprador.
 * Lee de Firestore (colección "ordenes") todas las compras del usuario que
 * inició sesión y las muestra de la más reciente a la más antigua.
 */
class HistorialActivity : AppCompatActivity() {

    // Base de datos y autenticación de Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Contenedor donde se dibuja una tarjeta por pedido
    private lateinit var contenedor: LinearLayout

    /** Se ejecuta al abrir la pantalla: configura el botón volver y carga los pedidos. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        contenedor = findViewById(R.id.contenedorPedidos)
        findViewById<View>(R.id.btnVolverHistorial).setOnClickListener { finish() }

        cargarPedidos()
    }

    /**
     * LEER: consulta las órdenes del usuario actual en Firestore y las
     * ordena por fecha, de la más nueva a la más antigua.
     */
    @Suppress("UNCHECKED_CAST")
    private fun cargarPedidos() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("ordenes").whereEqualTo("uid", uid).get()
            .addOnSuccessListener { resultado ->
                contenedor.removeAllViews()

                if (resultado.isEmpty) {
                    contenedor.addView(crearTexto("Aún no has realizado pedidos", false, 0))
                    return@addOnSuccessListener
                }

                val formato = SimpleDateFormat("dd/MM/yyyy  h:mm a", Locale("es", "CO"))
                val moneda = NumberFormat.getInstance(Locale("es", "CO"))

                val ordenes = resultado.documents.sortedByDescending {
                    it.getTimestamp("fecha")?.seconds ?: 0L
                }

                for (orden in ordenes) {
                    val tarjeta = LinearLayout(this)
                    tarjeta.orientation = LinearLayout.VERTICAL
                    tarjeta.background = ContextCompat.getDrawable(this, R.drawable.bg_input)
                    tarjeta.setPadding(dp(16), dp(14), dp(16), dp(14))
                    val parametros = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    parametros.topMargin = dp(16)
                    tarjeta.layoutParams = parametros

                    val fecha = orden.getTimestamp("fecha")?.toDate()
                    val textoFecha = if (fecha != null) formato.format(fecha) else "Sin fecha"
                    val total = orden.getLong("total") ?: 0L

                    tarjeta.addView(crearTexto(textoFecha, true, 0))
                    tarjeta.addView(crearTexto("Estado: " + (orden.getString("estado") ?: ""), false, 4))

                    // Productos comprados en la orden
                    val items = orden.get("items") as? List<Map<String, Any>> ?: emptyList()
                    for (item in items) {
                        val cantidad = (item["cantidad"] as? Number)?.toInt() ?: 1
                        val precio = (item["precio"] as? Number)?.toLong() ?: 0L
                        tarjeta.addView(
                            crearTexto(
                                "• ${item["nombre"]} (Talla ${item["talla"]}, ${item["color"]}) x$cantidad - $" +
                                        moneda.format(precio * cantidad),
                                false, 6
                            )
                        )
                    }

                    tarjeta.addView(crearTexto("Envío a: " + (orden.getString("direccion") ?: ""), false, 8))
                    tarjeta.addView(crearTexto("Pago: " + (orden.getString("metodo") ?: ""), false, 2))
                    tarjeta.addView(crearTexto("TOTAL: $" + moneda.format(total) + " COP", true, 8))

                    contenedor.addView(tarjeta)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudieron cargar los pedidos", Toast.LENGTH_SHORT).show()
            }
    }

    /** Crea un texto con el estilo de la app; en negrita si es título. */
    private fun crearTexto(texto: String, negrita: Boolean, margenSuperior: Int): TextView {
        val vista = TextView(this)
        vista.text = texto
        vista.textSize = 13f
        vista.setTextColor(getColor(if (negrita) R.color.gold else R.color.text_brown))
        vista.typeface = ResourcesCompat.getFont(
            this,
            if (negrita) R.font.inria_serif_bold else R.font.inria_serif_regular
        )
        vista.setPadding(0, dp(margenSuperior), 0, 0)
        return vista
    }

    /** Convierte una medida en dp a píxeles según la pantalla del dispositivo. */
    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}