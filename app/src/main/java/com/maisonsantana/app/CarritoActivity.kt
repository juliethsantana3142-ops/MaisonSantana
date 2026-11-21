package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla del carrito de compras.
 * Los productos se guardan en el dispositivo (SharedPreferences "carrito"),
 * una línea por producto con el formato: nombre|precio|cantidad|talla|color.
 * Permite ver el carrito, cambiar la cantidad, quitar productos, vaciarlo
 * e ir al pago.
 */
class CarritoActivity : AppCompatActivity() {

    /** Se ejecuta al abrir la pantalla: configura los botones y dibuja el carrito. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        findViewById<TextView>(R.id.btnVolverCarrito).setOnClickListener { finish() }

        // Vacía todo el carrito
        findViewById<Button>(R.id.btnVaciar).setOnClickListener {
            getSharedPreferences("carrito", Context.MODE_PRIVATE).edit().clear().apply()
            mostrarCarrito()
        }

        // Pasa al pago si hay productos
        findViewById<Button>(R.id.btnIrPagar).setOnClickListener {
            if (leerItems().isEmpty()) {
                Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, PagoActivity::class.java))
            }
        }

        mostrarCarrito()
    }

    /** Cada vez que se vuelve a la pantalla se redibuja el carrito. */
    override fun onResume() {
        super.onResume()
        mostrarCarrito()
    }

    /** LEER: devuelve los productos guardados, cada uno como una lista de campos. */
    private fun leerItems(): MutableList<MutableList<String>> {
        val texto = getSharedPreferences("carrito", Context.MODE_PRIVATE)
            .getString("items", "") ?: ""
        return texto.split("\n")
            .filter { it.isNotBlank() }
            .map { it.split("|").toMutableList() }
            .toMutableList()
    }

    /** Guarda de nuevo la lista de productos del carrito. */
    private fun guardarItems(items: List<List<String>>) {
        val texto = items.joinToString("") { it.joinToString("|") + "\n" }
        getSharedPreferences("carrito", Context.MODE_PRIVATE).edit()
            .putString("items", texto).apply()
    }

    /**
     * Dibuja el carrito: una fila por producto con su detalle y los botones
     * para restar, sumar o quitar, y calcula el total.
     */
    private fun mostrarCarrito() {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorCarrito)
        contenedor.removeAllViews()
        var total = 0
        val items = leerItems()

        if (items.isEmpty()) {
            val vacio = TextView(this)
            vacio.text = "Tu carrito está vacío"
            vacio.setPadding(0, 40, 0, 0)
            contenedor.addView(vacio)
        }

        for ((indice, item) in items.withIndex()) {
            val nombre = item[0]
            val precio = item[1].toInt()
            val cantidad = item[2].toInt()
            val talla = item[3]
            val color = item.getOrElse(4) { "Blanco" }
            total += precio * cantidad

            val fila = TextView(this)
            fila.text = "$nombre\nTalla $talla  -  $color  x$cantidad  -  $" +
                    String.format("%,d", precio * cantidad) + " COP"
            fila.setPadding(0, 28, 0, 0)
            fila.setTextColor(getColor(R.color.text_brown))
            contenedor.addView(fila)

            // Botones: restar, sumar y quitar
            val acciones = LinearLayout(this)
            acciones.orientation = LinearLayout.HORIZONTAL
            acciones.addView(crearAccion("−") {
                if (cantidad > 1) {
                    items[indice][2] = (cantidad - 1).toString()
                    guardarItems(items)
                    mostrarCarrito()
                }
            })
            acciones.addView(crearAccion("+") {
                items[indice][2] = (cantidad + 1).toString()
                guardarItems(items)
                mostrarCarrito()
            })
            acciones.addView(crearAccion("Quitar") {
                items.removeAt(indice)
                guardarItems(items)
                mostrarCarrito()
            })
            contenedor.addView(acciones)
        }

        findViewById<TextView>(R.id.tvTotalCarrito).text =
            "TOTAL: $" + String.format("%,d", total) + " COP"
    }

    /** Crea un botón de texto dorado para las acciones de cada producto. */
    private fun crearAccion(texto: String, accion: () -> Unit): TextView {
        val vista = TextView(this)
        vista.text = texto
        vista.setTextColor(getColor(R.color.gold))
        vista.textSize = 16f
        vista.gravity = Gravity.CENTER
        vista.minWidth = (48 * resources.displayMetrics.density).toInt()
        vista.minHeight = (40 * resources.displayMetrics.density).toInt()
        vista.setPadding(12, 0, 12, 0)
        vista.setOnClickListener { accion() }
        return vista
    }
}