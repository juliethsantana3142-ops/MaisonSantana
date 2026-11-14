package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PagoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago)

        val prefsCarrito = getSharedPreferences("carrito", Context.MODE_PRIVATE)
        val texto = prefsCarrito.getString("items", "") ?: ""
        val items = texto.split("\n").filter { it.isNotBlank() }.map { it.split("|") }

        var subtotal = 0
        val resumen = StringBuilder()
        for (item in items) {
            val precio = item[1].toInt()
            val cantidad = item[2].toInt()
            subtotal += precio * cantidad
            resumen.append(item[0]).append(" (Talla ").append(item[3])
                .append(", x").append(cantidad).append("): $")
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

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnProcesar).setOnClickListener {
            val direccion = findViewById<EditText>(R.id.etDireccion).text.toString().trim()
            val metodo = findViewById<RadioGroup>(R.id.rgPago).checkedRadioButtonId

            if (items.isEmpty()) {
                Toast.makeText(this, "Tu carrito está vacío", Toast.LENGTH_SHORT).show()
            } else if (direccion.isBlank()) {
                Toast.makeText(this, "Escribe la dirección de envío", Toast.LENGTH_SHORT).show()
            } else if (metodo == -1) {
                Toast.makeText(this, "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
            } else {
                prefsCarrito.edit().clear().apply()
                Toast.makeText(this, "¡Pago procesado con éxito!", Toast.LENGTH_LONG).show()
                val i = Intent(this, InicioActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                finish()
            }
        }

        findViewById<TextView>(R.id.navHome).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navSearch).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            Toast.makeText(this, "Ya estás en el pago", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}