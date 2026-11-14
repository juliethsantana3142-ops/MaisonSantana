package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class CarritoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        findViewById<TextView>(R.id.btnVolverCarrito).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnVaciar).setOnClickListener {
            getSharedPreferences("carrito", Context.MODE_PRIVATE).edit().clear().apply()
            mostrarCarrito()
        }

        findViewById<Button>(R.id.btnIrPagar).setOnClickListener {
            if (leerItems().isEmpty()) {
                Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, PagoActivity::class.java))
            }
        }

        mostrarCarrito()
    }

    override fun onResume() {
        super.onResume()
        mostrarCarrito()
    }

    private fun leerItems(): List<List<String>> {
        val texto = getSharedPreferences("carrito", Context.MODE_PRIVATE)
            .getString("items", "") ?: ""
        return texto.split("\n").filter { it.isNotBlank() }.map { it.split("|") }
    }

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

        for (item in items) {
            val nombre = item[0]
            val precio = item[1].toInt()
            val cantidad = item[2].toInt()
            val talla = item[3]
            total += precio * cantidad

            val fila = TextView(this)
            fila.text = "$nombre\nTalla $talla  x$cantidad  -  $" +
                    String.format("%,d", precio * cantidad) + " COP"
            fila.setPadding(0, 28, 0, 0)
            fila.setTextColor(getColor(R.color.text_brown))
            contenedor.addView(fila)
        }

        findViewById<TextView>(R.id.tvTotalCarrito).text =
            "TOTAL: $" + String.format("%,d", total) + " COP"
    }
}