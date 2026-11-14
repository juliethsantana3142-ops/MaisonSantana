package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

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
            if (prefs.getString("estado_$correo", "Activo") == "Inactivo") {
                Toast.makeText(this, "Tu cuenta está inactiva", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            entrar(correo)
        }

        findViewById<Button>(R.id.btnHuella).setOnClickListener { entrarConHuella() }

        findViewById<Button>(R.id.btnRegistro).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        findViewById<Button>(R.id.tvRecuperar).setOnClickListener {
            startActivity(Intent(this, RecuperarActivity::class.java))
        }
    }

    private fun entrar(correo: String) {
        val prefs = getSharedPreferences("usuarios", Context.MODE_PRIVATE)
        getSharedPreferences("sesion", Context.MODE_PRIVATE)
            .edit().putString("correo", correo).putString("ultimo", correo).apply()

        val destino = when (prefs.getString("rol_$correo", "Comprador")) {
            "Administrador" -> AdminActivity::class.java
            "Vendedor" -> VendedorActivity::class.java
            else -> InicioActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }

    private fun entrarConHuella() {
        val ultimo = getSharedPreferences("sesion", Context.MODE_PRIVATE)
            .getString("ultimo", null)
        if (ultimo == null) {
            Toast.makeText(this, "Primero inicia sesión una vez con tu correo", Toast.LENGTH_LONG).show()
            return
        }


        val autenticadores = BiometricManager.Authenticators.BIOMETRIC_WEAK
        val disponible = BiometricManager.from(this).canAuthenticate(autenticadores)
        if (disponible != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Registra una huella en los ajustes del celular", Toast.LENGTH_LONG).show()
            return
        }

        val ejecutor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, ejecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    entrar(ultimo)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@MainActivity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
                }
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Maison Santana")
            .setSubtitle("Entra con tu huella")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(autenticadores)
            .build()

        prompt.authenticate(info)
    }
}