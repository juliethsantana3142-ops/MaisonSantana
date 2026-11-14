package com.maisonsantana.app

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etRegCorreo)
        val etContrasena = findViewById<EditText>(R.id.etRegContrasena)
        val etConfirmar = findViewById<EditText>(R.id.etRegConfirmar)
        val spRol = findViewById<Spinner>(R.id.spRolRegistro)

        val roles = listOf("Comprador", "Vendedor", "Administrador")
        spRol.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        findViewById<Button>(R.id.btnCrearCuenta).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim().lowercase()
            val contrasena = etContrasena.text.toString()
            val confirmar = etConfirmar.text.toString()
            val rol = spRol.selectedItem.toString()

            if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank() || confirmar.isBlank()) {
                mostrar("Completa todos los campos")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrar("Correo no válido")
                return@setOnClickListener
            }
            if (contrasena.length < 6) {
                mostrar("La contraseña debe tener mínimo 6 caracteres")
                return@setOnClickListener
            }
            if (contrasena != confirmar) {
                mostrar("Las contraseñas no coinciden")
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("usuarios", Context.MODE_PRIVATE)
            if (prefs.contains("pass_$correo")) {
                mostrar("Ese correo ya está registrado")
                return@setOnClickListener
            }

            prefs.edit()
                .putString("nombre_$correo", nombre)
                .putString("pass_$correo", contrasena)
                .putString("rol_$correo", rol)
                .apply()

            mostrar("Cuenta creada como $rol. Ahora inicia sesión")
            finish()
        }

        findViewById<Button>(R.id.btnVolverLogin).setOnClickListener { finish() }
    }

    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}