package com.maisonsantana.app

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla de recuperación de contraseña.
 * Envía al correo del usuario un enlace de Firebase Authentication para
 * que pueda crear una nueva contraseña de forma segura.
 */
class RecuperarActivity : AppCompatActivity() {

    // Servicio de autenticación de Firebase
    private val auth = FirebaseAuth.getInstance()

    /**
     * Se ejecuta al abrir la pantalla: enlaza las vistas del XML
     * y configura los botones "Enviar enlace" y "Volver".
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        val etCorreo = findViewById<EditText>(R.id.etRecCorreo)
        val btnEnviar = findViewById<Button>(R.id.btnRestablecer)
        val btnVolver = findViewById<Button>(R.id.btnVolverRec)

        // Botón para volver al login
        btnVolver.setOnClickListener { finish() }

        // Botón para enviar el correo de recuperación
        btnEnviar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()

            // Se valida el correo antes de llamar a Firebase
            if (correo.isEmpty()) {
                mostrar("Escribe tu correo")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrar("El correo no tiene un formato válido")
                return@setOnClickListener
            }

            // Se bloquea el botón mientras se envía para evitar toques repetidos
            btnEnviar.isEnabled = false
            enviarCorreo(correo, btnEnviar)
        }
    }

    /**
     * Pide a Firebase que envíe el correo de restablecimiento de contraseña.
     * Si sale bien, informa a la persona y cierra la pantalla.
     */
    private fun enviarCorreo(correo: String, boton: Button) {
        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener {
                mostrar("Te enviamos un enlace. Revisa tu correo (y la carpeta de spam)")
                finish()
            }
            .addOnFailureListener {
                mostrar("No se pudo enviar el correo: ${it.message}")
                boton.isEnabled = true
            }
    }

    /** Muestra un mensaje corto en pantalla. */
    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }
}
