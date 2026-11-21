package com.maisonsantana.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla del Vendedor: CRUD completo de productos con Firestore.
 *  - Crear: botón "+ Nuevo" con foto de cámara o galería.
 *  - Leer: lista los productos del vendedor de la colección "productos".
 *  - Actualizar: botón "Editar" de cada producto.
 *  - Eliminar: botón "Eliminar" con confirmación.
 * Cada producto guarda nombre, precio, tallas, colores, descripción, estado
 * y la foto, que se comprime y se guarda como texto Base64.
 */
class VendedorActivity : AppCompatActivity() {

    // Base de datos y autenticación de Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estados posibles de un producto
    private val estados = listOf("Disponible", "Agotado")

    // Contenedor donde se agregan las tarjetas de producto
    private lateinit var contenedor: LinearLayout

    // Foto del formulario abierto: vista previa y texto Base64 a guardar
    private var vistaFoto: ImageView? = null
    private var fotoBase64: String? = null

    // Abre la cámara y devuelve una imagen en miniatura
    private val tomarFoto =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) establecerFoto(bitmap)
        }

    // Abre la galería para elegir una imagen existente
    private val elegirFoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val bitmap = cargarBitmap(uri)
                if (bitmap != null) establecerFoto(bitmap) else mostrar("No se pudo abrir la imagen")
            }
        }

    /**
     * Se ejecuta al abrir la pantalla: enlaza las vistas, configura el botón
     * "+ Nuevo" y la barra inferior, y carga los productos.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendedor)

        contenedor = findViewById(R.id.contenedorProductos)

        findViewById<Button>(R.id.btnNuevo).setOnClickListener { mostrarFormulario(null) }

        // Barra inferior
        findViewById<View>(R.id.navPerfil).setOnClickListener { cerrarSesion() }
        findViewById<View>(R.id.navInventario).setOnClickListener {
            cargarProductos()
            mostrar("Inventario actualizado")
        }
        mensaje(R.id.navReportes, "Reportes de ventas próximamente")
        mensaje(R.id.navPedidos, "Pedidos próximamente")
        mensaje(R.id.navBuscar, "Búsqueda próximamente")

        cargarProductos()
    }

    /** Asigna un mensaje corto a un botón de la barra inferior. */
    private fun mensaje(id: Int, texto: String) {
        findViewById<View>(id).setOnClickListener { mostrar(texto) }
    }

    /**
     * LEER: consulta los productos del vendedor en Firestore y dibuja
     * una tarjeta por cada uno, ordenados por nombre.
     */
    private fun cargarProductos() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("productos").whereEqualTo("vendedorUid", uid).get()
            .addOnSuccessListener { resultado ->
                contenedor.removeAllViews()

                if (resultado.isEmpty) {
                    val vacio = TextView(this)
                    vacio.text = "Aún no has creado productos"
                    vacio.setPadding(0, 40, 0, 0)
                    contenedor.addView(vacio)
                    return@addOnSuccessListener
                }

                val lista = resultado.documents.sortedBy {
                    (it.getString("nombre") ?: "").lowercase()
                }
                for (documento in lista) agregarTarjeta(documento)
            }
            .addOnFailureListener { mostrar("No se pudieron cargar los productos: ${it.message}") }
    }

    /**
     * Dibuja la tarjeta de un producto con su foto, nombre, precio, tallas,
     * colores y estado, y los botones "Editar" y "Eliminar".
     */
    private fun agregarTarjeta(documento: DocumentSnapshot) {
        val fila = LayoutInflater.from(this).inflate(R.layout.item_producto, contenedor, false)

        val nombre = documento.getString("nombre") ?: ""
        val precio = documento.getLong("precio") ?: 0L
        val tallas = documento.getString("tallas") ?: ""
        val colores = documento.getString("colores") ?: ""
        val estado = documento.getString("estado") ?: "Disponible"
        val foto = documento.getString("foto") ?: ""

        fila.findViewById<TextView>(R.id.tvProductoNombre).text = nombre
        fila.findViewById<TextView>(R.id.tvProductoPrecio).text =
            "$" + NumberFormat.getInstance(Locale("es", "CO")).format(precio) + " COP"
        fila.findViewById<TextView>(R.id.tvProductoDetalle).text =
            "Tallas: $tallas\nColores: $colores\nEstado: $estado"

        if (foto.isNotEmpty()) {
            base64ABitmap(foto)?.let {
                fila.findViewById<ImageView>(R.id.imgProducto).setImageBitmap(it)
            }
        }

        fila.findViewById<Button>(R.id.btnEditarProducto).setOnClickListener {
            mostrarFormulario(documento)
        }
        fila.findViewById<Button>(R.id.btnEliminarProducto).setOnClickListener {
            confirmarEliminar(documento.id, nombre)
        }

        contenedor.addView(fila)
    }

    /**
     * CREAR / ACTUALIZAR: muestra el formulario del producto. Si recibe un
     * documento, lo rellena para editarlo; si es null, crea uno nuevo.
     */
    private fun mostrarFormulario(producto: DocumentSnapshot?) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_producto, null)

        vistaFoto = vista.findViewById(R.id.imgFotoProducto)
        val etNombre = vista.findViewById<EditText>(R.id.etProdNombre)
        val etPrecio = vista.findViewById<EditText>(R.id.etProdPrecio)
        val etTallas = vista.findViewById<EditText>(R.id.etProdTallas)
        val etColores = vista.findViewById<EditText>(R.id.etProdColores)
        val etDescripcion = vista.findViewById<EditText>(R.id.etProdDescripcion)
        val spEstado = vista.findViewById<Spinner>(R.id.spProdEstado)

        spEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

        // Si se edita, se cargan los datos actuales del producto
        fotoBase64 = producto?.getString("foto")
        if (producto != null) {
            etNombre.setText(producto.getString("nombre"))
            etPrecio.setText((producto.getLong("precio") ?: 0L).toString())
            etTallas.setText(producto.getString("tallas"))
            etColores.setText(producto.getString("colores"))
            etDescripcion.setText(producto.getString("descripcion"))
            spEstado.setSelection(estados.indexOf(producto.getString("estado")).coerceAtLeast(0))
            fotoBase64?.takeIf { it.isNotEmpty() }?.let { texto ->
                base64ABitmap(texto)?.let { vistaFoto?.setImageBitmap(it) }
            }
        }

        vista.findViewById<Button>(R.id.btnCamara).setOnClickListener { tomarFoto.launch(null) }
        vista.findViewById<Button>(R.id.btnGaleria).setOnClickListener { elegirFoto.launch("image/*") }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (producto == null) "Nuevo producto" else "Editar producto")
            .setView(vista)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialogo.show()

        // Se controla el botón Guardar para que el formulario no se cierre si hay errores
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val precio = etPrecio.text.toString().trim().toLongOrNull()
            val tallas = etTallas.text.toString().trim()
            val colores = etColores.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (nombre.isEmpty() || tallas.isEmpty() || colores.isEmpty()) {
                mostrar("Escribe el nombre, las tallas y los colores")
                return@setOnClickListener
            }
            if (precio == null || precio <= 0) {
                mostrar("Escribe un precio válido")
                return@setOnClickListener
            }

            guardarProducto(
                producto?.id, nombre, precio, tallas, colores, descripcion,
                spEstado.selectedItem.toString()
            )
            dialogo.dismiss()
        }
    }

    /**
     * Guarda el producto en Firestore: lo crea si no tiene id, o lo
     * actualiza si ya existe.
     */
    private fun guardarProducto(
        id: String?,
        nombre: String,
        precio: Long,
        tallas: String,
        colores: String,
        descripcion: String,
        estado: String
    ) {
        val datos = hashMapOf<String, Any>(
            "nombre" to nombre,
            "precio" to precio,
            "tallas" to tallas,
            "colores" to colores,
            "descripcion" to descripcion,
            "estado" to estado,
            "foto" to (fotoBase64 ?: ""),
            "vendedorUid" to (auth.currentUser?.uid ?: "")
        )

        val tarea = if (id == null) {
            db.collection("productos").add(datos)
        } else {
            db.collection("productos").document(id).update(datos)
        }

        tarea
            .addOnSuccessListener {
                mostrar(if (id == null) "Producto creado" else "Producto actualizado")
                cargarProductos()
            }
            .addOnFailureListener { mostrar("No se pudo guardar: ${it.message}") }
    }

    /**
     * ELIMINAR: pide confirmación y borra el producto de Firestore.
     */
    private fun confirmarEliminar(id: String, nombre: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Seguro que deseas eliminar \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("productos").document(id).delete()
                    .addOnSuccessListener {
                        mostrar("Producto eliminado")
                        cargarProductos()
                    }
                    .addOnFailureListener { mostrar("No se pudo eliminar: ${it.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Lee una imagen de la galería reduciendo su tamaño al abrirla,
     * para no gastar demasiada memoria con fotos grandes.
     */
    private fun cargarBitmap(uri: Uri): Bitmap? {
        return try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, limites)
            }
            var muestra = 1
            while (limites.outWidth / muestra > 1200 || limites.outHeight / muestra > 1200) {
                muestra *= 2
            }
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opciones)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reduce la foto a un máximo de 600 píxeles, la comprime en JPEG y la
     * convierte a texto Base64 para guardarla en Firestore. También la
     * muestra en la vista previa del formulario.
     */
    private fun establecerFoto(original: Bitmap) {
        val maximo = 600
        val escala = minOf(1f, maximo.toFloat() / maxOf(original.width, original.height))
        val bitmap = if (escala < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * escala).toInt(),
                (original.height * escala).toInt(),
                true
            )
        } else {
            original
        }

        val salida = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, salida)
        fotoBase64 = Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
        vistaFoto?.setImageBitmap(bitmap)
    }

    /** Convierte un texto Base64 guardado en Firestore de nuevo en imagen. */
    private fun base64ABitmap(texto: String): Bitmap? {
        return try {
            val bytes = Base64.decode(texto, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
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







