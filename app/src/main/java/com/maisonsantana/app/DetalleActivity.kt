package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Pantalla de detalle de un producto.
 * Muestra la foto, el nombre y el precio, permite elegir talla, color y
 * cantidad, y agrega el producto al carrito. Puede abrirse con un producto
 * fijo (nombre, precio y foto) o con el identificador de un producto de Firestore.
 */
class DetalleActivity : AppCompatActivity() {

    // Cantidad elegida, y datos del producto que se va a agregar al carrito
    private var cantidad = 1
    private var nombre = "Producto"
    private var precio = 0
    private var color = "Blanco"

    // Base de datos Firestore
    private val db = FirebaseFirestore.getInstance()

    /**
     * Se ejecuta al abrir la pantalla: recibe los datos del producto,
     * configura la cantidad, la talla y el botón de agregar al carrito.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)

        nombre = intent.getStringExtra("nombre") ?: "Producto"
        precio = intent.getIntExtra("precio", 0)
        val foto = intent.getIntExtra("foto", 0)
        val productoId = intent.getStringExtra("productoId")

        if (foto != 0) {
            findViewById<ImageView>(R.id.imgDetalle).setImageResource(foto)
        }
        mostrarDatos()

        // Si llega el identificador de un producto, se leen sus datos desde Firestore
        if (productoId != null) cargarProducto(productoId)

        val tvCantidad = findViewById<TextView>(R.id.tvCantidad)

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnMas).setOnClickListener {
            cantidad++
            tvCantidad.text = cantidad.toString()
        }
        findViewById<TextView>(R.id.btnMenos).setOnClickListener {
            if (cantidad > 1) cantidad--
            tvCantidad.text = cantidad.toString()
        }

        // Agregar al carrito: exige elegir una talla
        findViewById<Button>(R.id.btnAgregar).setOnClickListener {
            val grupo = findViewById<RadioGroup>(R.id.rgTalla)
            if (grupo.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Selecciona una talla", Toast.LENGTH_SHORT).show()
            } else {
                val talla = findViewById<RadioButton>(grupo.checkedRadioButtonId).text.toString()
                val prefs = getSharedPreferences("carrito", Context.MODE_PRIVATE)
                val actual = prefs.getString("items", "") ?: ""
                // Cada producto se guarda en una línea: nombre|precio|cantidad|talla|color
                val nuevo = actual + nombre + "|" + precio + "|" + cantidad + "|" + talla + "|" + color + "\n"
                prefs.edit().putString("items", nuevo).apply()
                Toast.makeText(this, "Agregado al carrito", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CarritoActivity::class.java))
            }
        }

        // Barra de navegación inferior
        findViewById<TextView>(R.id.navHome).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navSearch).setOnClickListener { finish() }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    /** Muestra el nombre y el precio actuales del producto en pantalla. */
    private fun mostrarDatos() {
        findViewById<TextView>(R.id.tvNombre).text = nombre
        findViewById<TextView>(R.id.tvPrecio).text = "$" + String.format("%,d", precio) + " COP"
    }

    /**
     * LEER: obtiene el producto desde Firestore y actualiza la pantalla con
     * su nombre, precio, foto, tallas y colores disponibles.
     */
    private fun cargarProducto(id: String) {
        db.collection("productos").document(id).get()
            .addOnSuccessListener { documento ->
                if (!documento.exists()) {
                    Toast.makeText(this, "Este producto ya no está disponible", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                nombre = documento.getString("nombre") ?: "Producto"
                precio = (documento.getLong("precio") ?: 0L).toInt()
                mostrarDatos()

                // Foto guardada como texto Base64
                val foto = documento.getString("foto") ?: ""
                if (foto.isNotEmpty()) {
                    try {
                        val bytes = Base64.decode(foto, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        findViewById<ImageView>(R.id.imgDetalle).setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Si la foto no se puede leer, se deja la imagen por defecto
                    }
                }

                filtrarTallas(documento.getString("tallas") ?: "")
                mostrarColores(documento.getString("colores") ?: "")
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cargar el producto", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Muestra solo las tallas que el vendedor indicó para el producto.
     * Si ninguna coincide con las opciones de la pantalla, se muestran todas.
     */
    private fun filtrarTallas(tallas: String) {
        val disponibles = tallas.split(",", " ", "/")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }

        val grupo = findViewById<RadioGroup>(R.id.rgTalla)
        val botones = (0 until grupo.childCount).mapNotNull { grupo.getChildAt(it) as? RadioButton }

        if (botones.any { it.text.toString().trim().uppercase() in disponibles }) {
            for (boton in botones) {
                if (boton.text.toString().trim().uppercase() !in disponibles) {
                    boton.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Crea las opciones de color según lo que escribió el vendedor.
     * Reemplaza el texto fijo "Seleccionar Color" por una lista de opciones
     * y deja el primer color seleccionado por defecto.
     */
    private fun mostrarColores(colores: String) {
        val lista = colores.split(",", "/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lista.isEmpty()) return

        // El texto "Seleccionar Color" está justo debajo del grupo de tallas
        val grupoTalla = findViewById<RadioGroup>(R.id.rgTalla)
        val padre = grupoTalla.parent as LinearLayout
        val posicion = padre.indexOfChild(grupoTalla) + 1
        (padre.getChildAt(posicion) as? TextView)?.text = "Seleccionar Color:"

        val grupoColor = RadioGroup(this)
        grupoColor.orientation = RadioGroup.VERTICAL

        for (nombreColor in lista) {
            val boton = RadioButton(this)
            boton.id = View.generateViewId()
            boton.text = nombreColor
            boton.typeface = ResourcesCompat.getFont(this, R.font.inria_serif_regular)
            boton.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                (48 * resources.displayMetrics.density).toInt()
            )
            grupoColor.addView(boton)
        }

        // Se selecciona el primer color por defecto
        (grupoColor.getChildAt(0) as RadioButton).isChecked = true
        color = lista[0]

        // Cada vez que se elige otro color se actualiza la variable
        grupoColor.setOnCheckedChangeListener { grupo, idElegido ->
            color = grupo.findViewById<RadioButton>(idElegido).text.toString()
        }

        padre.addView(grupoColor, posicion + 1)
    }
}