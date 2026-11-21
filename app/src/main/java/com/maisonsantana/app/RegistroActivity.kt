package com.maisonsantana.app

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Pantalla de registro de usuarios.
 * Crea la cuenta en Firebase Authentication (correo y contraseña) y guarda
 * los datos del perfil (nombre, correo, rol y estado) en la colección "usuarios"
 * de Firestore, usando el uid de la cuenta como identificador del documento.
 */
class RegistroActivity : AppCompatActivity() {

    // Servicio de autenticación de Firebase
    private val auth = FirebaseAuth.getInstance()

    // Base de datos Firestore
    private val db = FirebaseFirestore.getInstance()

    // Roles disponibles para elegir al registrarse
    private val roles = listOf("Comprador", "Vendedor", "Administrador")

    /**
     * Se ejecuta al abrir la pantalla: enlaza las vistas del XML
     * y configura los botones de "Crear cuenta" y "Volver".
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etRegCorreo)
        val spRol = findViewById<Spinner>(R.id.spRolRegistro)
        val etClave = findViewById<EditText>(R.id.etRegContrasena)
        val etConfirmar = findViewById<EditText>(R.id.etRegConfirmar)
        val btnCrear = findViewById<Button>(R.id.btnCrearCuenta)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        // Si el spinner no trae opciones definidas en el XML, se cargan desde aquí
        if (spRol.adapter == null) {
            spRol.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
            )
        }

        // Botón para volver a la pantalla de login
        btnVolver.setOnClickListener { finish() }

        // Botón para crear la cuenta
        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString()
            val confirmar = etConfirmar.text.toString()
            val rol = spRol.selectedItem.toString()

            // Se validan los datos antes de enviar nada a Firebase
            if (!datosValidos(nombre, correo, clave, confirmar)) return@setOnClickListener

            // Se bloquea el botón para evitar registros duplicados por doble toque
            btnCrear.isEnabled = false
            crearCuenta(nombre, correo, clave, rol, btnCrear)
        }
    }

    /**
     * Valida los campos del formulario.
     * Devuelve true si todo es correcto; si no, muestra un mensaje y devuelve false.
     */
    private fun datosValidos(
        nombre: String,
        correo: String,
        clave: String,
        confirmar: String
    ): Boolean {
        if (nombre.isEmpty() || correo.isEmpty() || clave.isEmpty() || confirmar.isEmpty()) {
            mostrar("Completa todos los campos")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrar("El correo no tiene un formato válido")
            return false
        }
        if (clave.length < 6) {
            mostrar("La contraseña debe tener mínimo 6 caracteres")
            return false
        }
        if (clave != confirmar) {
            mostrar("Las contraseñas no coinciden")
            return false
        }
        return true
    }

    /**
     * Crea el usuario en Firebase Authentication y, si sale bien,
     * guarda su perfil en Firestore con estado "Activo".
     */
    private fun crearCuenta(
        nombre: String,
        correo: String,
        clave: String,
        rol: String,
        boton: Button
    ) {
        auth.createUserWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrar("No se pudo obtener el usuario creado")
                    boton.isEnabled = true
                    return@addOnSuccessListener
                }

                // Datos del perfil que se guardan en Firestore
                val perfil = hashMapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "rol" to rol,
                    "estado" to "Activo"
                )

                db.collection("usuarios").document(uid).set(perfil)
                    .addOnSuccessListener {
                        mostrar("Cuenta creada correctamente")
                        // Se cierra la sesión para que la persona ingrese desde el login
                        auth.signOut()
                        finish()
                    }
                    .addOnFailureListener {
                        mostrar("No se pudo guardar el perfil: ${it.message}")
                        boton.isEnabled = true
                    }
            }
            .addOnFailureListener { error ->
                // Mensaje claro según el tipo de error
                if (error is FirebaseAuthUserCollisionException) {
                    mostrar("Ese correo ya está registrado")
                } else {
                    mostrar("No se pudo crear la cuenta: ${error.message}")
                }
                boton.isEnabled = true
            }
    }

    /** Muestra un mensaje corto en pantalla. */
    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }
}