package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)

        findViewById<Button>(R.id.btnIngresar).setOnClickListener {
            val correo = etCorreo.text.toString().trim().lowercase()
            val contrasena = etContrasena.text.toString()

            if (correo.isBlank() || contrasena.isBlank()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("usuarios", Context.MODE_PRIVATE)
            val guardada = prefs.getString("pass_$correo", null)

            if (guardada == null) {
                Toast.makeText(this, "Usuario no registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (guardada != contrasena) {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val destino = when (prefs.getString("rol_$correo", "Comprador")) {
                "Administrador" -> AdminActivity::class.java
                "Vendedor" -> VendedorActivity::class.java
                else -> InicioActivity::class.java
            }
            startActivity(Intent(this, destino))
            finish()
        }

        findViewById<Button>(R.id.btnRegistro).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }
}