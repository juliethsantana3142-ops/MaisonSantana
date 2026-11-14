package com.maisonsantana.app

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RecuperarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        val etCorreo = findViewById<EditText>(R.id.etRecCorreo)
        val etNueva = findViewById<EditText>(R.id.etRecNueva)

        findViewById<Button>(R.id.btnRestablecer).setOnClickListener {
            val correo = etCorreo.text.toString().trim().lowercase()
            val nueva = etNueva.text.toString()

            if (correo.isBlank() || nueva.isBlank()) {
                mostrar("Completa los campos")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrar("Correo no válido")
                return@setOnClickListener
            }
            if (nueva.length < 6) {
                mostrar("La contraseña debe tener mínimo 6 caracteres")
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("usuarios", Context.MODE_PRIVATE)
            if (!prefs.contains("pass_$correo")) {
                mostrar("Ese correo no está registrado")
                return@setOnClickListener
            }

            prefs.edit().putString("pass_$correo", nueva).apply()
            mostrar("Contraseña actualizada. Inicia sesión")
            finish()
        }

        findViewById<Button>(R.id.btnVolverRec).setOnClickListener { finish() }
    }

    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}