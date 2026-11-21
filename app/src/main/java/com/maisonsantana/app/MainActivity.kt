package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Pantalla de inicio de sesión.
 * Permite ingresar con correo y contraseña (Firebase Authentication) o con la
 * huella dactilar del dispositivo. Después de autenticar, lee el perfil del
 * usuario en Firestore, valida que su cuenta esté activa y lo envía a la
 * pantalla que le corresponde según su rol.
 */
class MainActivity : AppCompatActivity() {

    // Servicio de autenticación de Firebase
    private val auth = FirebaseAuth.getInstance()

    // Base de datos Firestore
    private val db = FirebaseFirestore.getInstance()

    // Botón de ingreso (se bloquea mientras se valida para evitar toques repetidos)
    private lateinit var btnIngresar: View

    /**
     * Se ejecuta al abrir la pantalla: enlaza las vistas del XML
     * y configura las acciones de cada botón.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etContrasena)
        btnIngresar = findViewById(R.id.btnIngresar)
        val btnHuella = findViewById<View>(R.id.btnHuella)
        val btnRegistro = findViewById<View>(R.id.btnRegistro)
        val tvRecuperar = findViewById<View>(R.id.tvRecuperar)

        // Ingreso con correo y contraseña
        btnIngresar.setOnClickListener {
            iniciarSesion(etCorreo.text.toString().trim(), etClave.text.toString())
        }

        // Ingreso con huella dactilar
        btnHuella.setOnClickListener { iniciarConHuella() }

        // Ir a la pantalla de registro
        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // Ir a la pantalla de recuperar contraseña
        tvRecuperar.setOnClickListener {
            startActivity(Intent(this, RecuperarActivity::class.java))
        }
    }

    /**
     * Valida los campos y autentica al usuario con Firebase.
     * Si las credenciales son correctas, carga su perfil desde Firestore.
     */
    private fun iniciarSesion(correo: String, clave: String) {
        if (correo.isEmpty() || clave.isEmpty()) {
            mostrar("Escribe tu correo y tu contraseña")
            return
        }

        btnIngresar.isEnabled = false
        auth.signInWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrar("No se pudo obtener el usuario")
                    btnIngresar.isEnabled = true
                } else {
                    cargarPerfilYEntrar(uid)
                }
            }
            .addOnFailureListener {
                mostrar("Correo o contraseña incorrectos")
                btnIngresar.isEnabled = true
            }
    }

    /**
     * Lee el perfil del usuario en Firestore (colección "usuarios").
     * Si la cuenta está inactiva se cierra la sesión y se bloquea el acceso;
     * si está activa, se guarda la sesión y se abre la pantalla según el rol.
     */
    private fun cargarPerfilYEntrar(uid: String) {
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { documento ->
                if (!documento.exists()) {
                    auth.signOut()
                    mostrar("No se encontró el perfil de este usuario")
                    btnIngresar.isEnabled = true
                    return@addOnSuccessListener
                }

                val nombre = documento.getString("nombre") ?: ""
                val correo = documento.getString("correo") ?: ""
                val rol = documento.getString("rol") ?: "Comprador"
                val estado = documento.getString("estado") ?: "Activo"

                // Una cuenta inactiva no puede ingresar
                if (estado == "Inactivo") {
                    auth.signOut()
                    mostrar("Tu cuenta está inactiva. Contacta al administrador")
                    btnIngresar.isEnabled = true
                    return@addOnSuccessListener
                }

                guardarSesion(uid, nombre, correo, rol)
                irSegunRol(rol)
            }
            .addOnFailureListener {
                mostrar("No se pudo leer el perfil: ${it.message}")
                btnIngresar.isEnabled = true
            }
    }

    /**
     * Guarda en el dispositivo los datos básicos de la sesión activa
     * para mostrarlos en otras pantallas (por ejemplo, el perfil).
     */
    private fun guardarSesion(uid: String, nombre: String, correo: String, rol: String) {
        getSharedPreferences("sesion", MODE_PRIVATE).edit()
            .putString("uid", uid)
            .putString("nombre", nombre)
            .putString("correo", correo)
            .putString("rol", rol)
            .apply()
    }

    /**
     * Abre la pantalla principal correspondiente al rol del usuario:
     * Administrador, Vendedor o Comprador.
     */
    private fun irSegunRol(rol: String) {
        val destino = when (rol) {
            "Administrador" -> AdminActivity::class.java
            "Vendedor" -> VendedorActivity::class.java
            else -> InicioActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }

    /**
     * Ingreso con huella dactilar.
     * Requiere haber iniciado sesión antes con correo y contraseña; la huella
     * confirma la identidad de la persona y reabre su sesión de Firebase.
     */
    private fun iniciarConHuella() {
        val usuarioActual = auth.currentUser
        if (usuarioActual == null) {
            mostrar("Primero inicia sesión una vez con tu correo y contraseña")
            return
        }


        val disponible = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        if (disponible != BiometricManager.BIOMETRIC_SUCCESS) {
            mostrar("Tu dispositivo no tiene una huella configurada")
            return
        }

        val ejecutor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, ejecutor,
            object : BiometricPrompt.AuthenticationCallback() {


                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    cargarPerfilYEntrar(usuarioActual.uid)
                }


                override fun onAuthenticationError(code: Int, mensaje: CharSequence) {
                    super.onAuthenticationError(code, mensaje)
                    if (code != BiometricPrompt.ERROR_USER_CANCELED &&
                        code != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        mostrar("No se pudo leer la huella: $mensaje")
                    }
                }
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Maison Santana")
            .setSubtitle("Ingresa con tu huella dactilar")
            .setNegativeButtonText("Cancelar")
            .build()

        prompt.authenticate(info)
    }

    /** Muestra un mensaje corto en pantalla. */
    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }
}