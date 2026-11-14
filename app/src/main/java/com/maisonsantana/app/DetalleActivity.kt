package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetalleActivity : AppCompatActivity() {
    private var cantidad = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        val nombre = intent.getStringExtra("nombre") ?: "Producto"
        val precio = intent.getIntExtra("precio", 0)
        val foto = intent.getIntExtra("foto", 0)

        if (foto != 0) {
            findViewById<ImageView>(R.id.imgDetalle).setImageResource(foto)
        }

        val tvCantidad = findViewById<TextView>(R.id.tvCantidad)
        findViewById<TextView>(R.id.tvNombre).text = nombre
        findViewById<TextView>(R.id.tvPrecio).text = "$" + String.format("%,d", precio) + " COP"

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnMas).setOnClickListener {
            cantidad++
            tvCantidad.text = cantidad.toString()
        }
        findViewById<TextView>(R.id.btnMenos).setOnClickListener {
            if (cantidad > 1) cantidad--
            tvCantidad.text = cantidad.toString()
        }

        findViewById<Button>(R.id.btnAgregar).setOnClickListener {
            val grupo = findViewById<RadioGroup>(R.id.rgTalla)
            if (grupo.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Selecciona una talla", Toast.LENGTH_SHORT).show()
            } else {
                val talla = findViewById<RadioButton>(grupo.checkedRadioButtonId).text.toString()
                val prefs = getSharedPreferences("carrito", Context.MODE_PRIVATE)
                val actual = prefs.getString("items", "") ?: ""
                val nuevo = actual + nombre + "|" + precio + "|" + cantidad + "|" + talla + "\n"
                prefs.edit().putString("items", nuevo).apply()
                Toast.makeText(this, "Agregado al carrito", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CarritoActivity::class.java))
            }
        }

        findViewById<TextView>(R.id.navHome).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navSearch).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}