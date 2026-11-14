package com.maisonsantana.app

import android.os.Bundle
import android.widget.Button
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

        val tvCantidad = findViewById<TextView>(R.id.tvCantidad)
        findViewById<TextView>(R.id.tvNombre).text = nombre
        findViewById<TextView>(R.id.tvPrecio).text =
            "$" + String.format("%,d", precio) + " COP"

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
            Toast.makeText(this, "Agregado al carrito", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.navHome).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navSearch).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            Toast.makeText(this, "Carrito próximamente", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Perfil próximamente", Toast.LENGTH_SHORT).show()
        }
    }
}