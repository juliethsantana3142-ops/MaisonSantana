package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Panel del Administrador: CRUD completo de usuarios con Firestore.
 *  - Crear: botón "+ Nuevo usuario" (cuenta en Authentication + perfil en Firestore).
 *  - Leer: lista todos los usuarios de la colección "usuarios".
 *  - Actualizar: cambia el rol y el estado (Activo/Inactivo) de un usuario.
 *  - Eliminar: borra el perfil del usuario, que ya no podrá ingresar a la app.
 */
class AdminActivity : AppCompatActivity() {

    // Base de datos Firestore y servicio de autenticación
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Opciones disponibles para rol y estado
    private val roles = listOf("Administrador", "Vendedor", "Comprador")
    private val estados = listOf("Activo", "Inactivo")

    // Contenedor donde se agregan las tarjetas de usuario
    private lateinit var contenedor: LinearLayout

    /**
     * Se ejecuta al abrir la pantalla: enlaza las vistas, configura los
     * botones principales y carga la lista de usuarios.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        contenedor = findViewById(R.id.contenedorUsuarios)

        findViewById<Button>(R.id.btnNuevoUsuario).setOnClickListener {
            mostrarDialogoNuevoUsuario()
        }
        findViewById<Button>(R.id.btnSalirAdmin).setOnClickListener {
            cerrarSesion()
        }

        cargarUsuarios()
    }

    /**
     * LEER: consulta todos los usuarios en Firestore y dibuja una tarjeta
     * por cada uno, ordenados por nombre.
     */
    private fun cargarUsuarios() {
        db.collection("usuarios").get()
            .addOnSuccessListener { resultado ->
                contenedor.removeAllViews()

                if (resultado.isEmpty) {
                    val vacio = TextView(this)
                    vacio.text = "Aún no hay usuarios registrados"
                    vacio.setPadding(0, 40, 0, 0)
                    contenedor.addView(vacio)
                    return@addOnSuccessListener
                }

                val lista = resultado.documents.sortedBy {
                    (it.getString("nombre") ?: "").lowercase()
                }
                for (documento in lista) {
                    agregarTarjeta(
                        uid = documento.id,
                        nombre = documento.getString("nombre") ?: "",
                        correo = documento.getString("correo") ?: "",
                        rol = documento.getString("rol") ?: "Comprador",
                        estado = documento.getString("estado") ?: "Activo"
                    )
                }
            }
            .addOnFailureListener {
                mostrar("No se pudieron cargar los usuarios: ${it.message}")
            }
    }

    /**
     * Dibuja la tarjeta de un usuario con sus datos, los selectores de
     * rol y estado, y los botones "Modificar permisos" y "Eliminar".
     */
    private fun agregarTarjeta(
        uid: String,
        nombre: String,
        correo: String,
        rol: String,
        estado: String
    ) {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_usuario, contenedor, false)

        fila.findViewById<TextView>(R.id.tvUsuarioNombre).text = nombre
        fila.findViewById<TextView>(R.id.tvUsuarioCorreo).text = correo

        val spRol = fila.findViewById<Spinner>(R.id.spRolUsuario)
        val spEstado = fila.findViewById<Spinner>(R.id.spEstadoUsuario)
        spRol.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)
        spRol.setSelection(roles.indexOf(rol).coerceAtLeast(0))
        spEstado.setSelection(estados.indexOf(estado).coerceAtLeast(0))

        // ACTUALIZAR: guarda el nuevo rol y estado en Firestore
        fila.findViewById<Button>(R.id.btnModificar).setOnClickListener {
            val cambios = mapOf(
                "rol" to spRol.selectedItem.toString(),
                "estado" to spEstado.selectedItem.toString()
            )
            db.collection("usuarios").document(uid).update(cambios)
                .addOnSuccessListener { mostrar("Permisos actualizados") }
                .addOnFailureListener { mostrar("No se pudo actualizar: ${it.message}") }
        }

        // ELIMINAR: pide confirmación antes de borrar
        fila.findViewById<Button>(R.id.btnEliminar).setOnClickListener {
            confirmarEliminar(uid, nombre)
        }

        contenedor.addView(fila)
    }

    /**
     * ELIMINAR: pregunta si se desea borrar al usuario. No permite que el
     * administrador elimine su propia cuenta.
     */
    private fun confirmarEliminar(uid: String, nombre: String) {
        if (uid == auth.currentUser?.uid) {
            mostrar("No puedes eliminar tu propia cuenta")
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Eliminar usuario")
            .setMessage("¿Seguro que deseas eliminar a $nombre? Ya no podrá ingresar a la app.")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("usuarios").document(uid).delete()
                    .addOnSuccessListener {
                        mostrar("Usuario eliminado")
                        cargarUsuarios()
                    }
                    .addOnFailureListener { mostrar("No se pudo eliminar: ${it.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * CREAR: muestra un formulario emergente para registrar un usuario nuevo
     * con nombre, correo, contraseña y rol.
     */
    private fun mostrarDialogoNuevoUsuario() {
        val caja = LinearLayout(this)
        caja.orientation = LinearLayout.VERTICAL
        caja.setPadding(48, 24, 48, 0)

        val etNombre = EditText(this)
        etNombre.hint = "Nombre completo"

        val etCorreo = EditText(this)
        etCorreo.hint = "Correo electrónico"
        etCorreo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val etClave = EditText(this)
        etClave.hint = "Contraseña (mínimo 6 caracteres)"
        etClave.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        val spRol = Spinner(this)
        spRol.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles.reversed()
        )

        caja.addView(etNombre)
        caja.addView(etCorreo)
        caja.addView(etClave)
        caja.addView(spRol)

        AlertDialog.Builder(this)
            .setTitle("Nuevo usuario")
            .setView(caja)
            .setPositiveButton("Crear") { _, _ ->
                crearUsuario(
                    etNombre.text.toString().trim(),
                    etCorreo.text.toString().trim(),
                    etClave.text.toString(),
                    spRol.selectedItem.toString()
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * CREAR: valida los datos y crea la cuenta en Authentication usando una
     * segunda instancia de Firebase, para que la sesión del administrador no
     * se cierre. Luego guarda el perfil en Firestore.
     */
    private fun crearUsuario(nombre: String, correo: String, clave: String, rol: String) {
        if (nombre.isEmpty() || correo.isEmpty() || clave.isEmpty()) {
            mostrar("Completa todos los campos")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrar("El correo no tiene un formato válido")
            return
        }
        if (clave.length < 6) {
            mostrar("La contraseña debe tener mínimo 6 caracteres")
            return
        }

        // Instancia secundaria de Firebase solo para crear la cuenta
        val opciones = FirebaseApp.getInstance().options
        val appSecundaria = try {
            FirebaseApp.getInstance("secundaria")
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(this, opciones, "secundaria")
        }
        val authSecundario = FirebaseAuth.getInstance(appSecundaria)

        authSecundario.createUserWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { resultado ->
                val uid = resultado.user?.uid
                if (uid == null) {
                    mostrar("No se pudo obtener el usuario creado")
                    return@addOnSuccessListener
                }
                val perfil = hashMapOf(
                    "nombre" to nombre,
                    "correo" to correo,
                    "rol" to rol,
                    "estado" to "Activo"
                )
                db.collection("usuarios").document(uid).set(perfil)
                    .addOnSuccessListener {
                        authSecundario.signOut()
                        mostrar("Usuario creado correctamente")
                        cargarUsuarios()
                    }
                    .addOnFailureListener { mostrar("No se pudo guardar el perfil: ${it.message}") }
            }
            .addOnFailureListener { mostrar("No se pudo crear la cuenta: ${it.message}") }
    }

    /**
     * Cierra la sesión de la app: borra los datos de sesión guardados y
     * vuelve al login, limpiando las pantallas anteriores.
     */
    private fun cerrarSesion() {
        getSharedPreferences("sesion", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /** Muestra un mensaje corto en pantalla. */
    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}
