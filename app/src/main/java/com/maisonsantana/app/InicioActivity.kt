package com.maisonsantana.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de inicio del Comprador.
 * Muestra las promociones y los productos destacados de la tienda, y debajo
 * el catálogo real: los productos que los vendedores crearon en Firestore.
 * Desde aquí se abre el detalle de un producto, el carrito y el perfil.
 */
class InicioActivity : AppCompatActivity() {

    // Base de datos Firestore
    private val db = FirebaseFirestore.getInstance()

    // Contenedor donde se dibuja el catálogo cargado desde Firestore
    private lateinit var catalogo: LinearLayout

    /**
     * Se ejecuta al abrir la pantalla: configura las promociones, los
     * productos destacados, la barra inferior y crea la sección del catálogo.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Promociones principales
        abrirDetalle(R.id.imgPromo1, "Conjunto Blanco Primavera - Top Halter y Falda Volantes", 120000, R.drawable.promo_1)
        abrirDetalle(R.id.imgPromo2, "Conjunto Blanco Primavera - Vista Espalda", 120000, R.drawable.promo2)

        findViewById<Button>(R.id.btnNuevaColeccion).setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java)
            intent.putExtra("nombre", "Conjunto Blanco Primavera - Top Halter y Falda Volantes")
            intent.putExtra("precio", 120000)
            intent.putExtra("foto", R.drawable.promo_1)
            startActivity(intent)
        }

        // Productos destacados
        abrirDetalle(R.id.card1, "Body Satinado de Tirantes con Perlas Maison Pearl", 48000, R.drawable.body_perlas)
        abrirDetalle(R.id.card2, "Vestido Corto Strapless Vino", 180000, R.drawable.vestido_vino)
        abrirDetalle(R.id.card3, "Vestido Negro Asimétrico", 220000, R.drawable.vestido_negro)

        // Barra de navegación inferior
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            Toast.makeText(this, "Favoritos próximamente", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navSearch).setOnClickListener {
            Toast.makeText(this, "Búsqueda próximamente", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CarritoActivity::class.java))
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        crearSeccionCatalogo()
    }

    /** Cada vez que se vuelve a esta pantalla se refresca el catálogo. */
    override fun onResume() {
        super.onResume()
        cargarCatalogo()
    }

    /**
     * Agrega al final de la pantalla el título "Catálogo de la tienda" y un
     * contenedor vacío donde luego se dibujan los productos de Firestore.
     */
    private fun crearSeccionCatalogo() {
        // Contenedor vertical principal que está dentro del ScrollView
        val principal = findViewById<View>(R.id.card1).parent.parent as LinearLayout

        val titulo = TextView(this)
        titulo.text = "Catálogo de la tienda"
        titulo.setTextColor(getColor(R.color.gold))
        titulo.textSize = 18f
        titulo.typeface = ResourcesCompat.getFont(this, R.font.inria_serif_bold)
        titulo.setPadding(dp(20), dp(12), 0, dp(4))

        catalogo = LinearLayout(this)
        catalogo.orientation = LinearLayout.VERTICAL
        catalogo.setPadding(dp(20), 0, dp(20), dp(24))

        principal.addView(titulo)
        principal.addView(catalogo)
    }

    /**
     * LEER: consulta los productos de Firestore y dibuja una fila por cada
     * producto disponible, ordenados por nombre.
     */
    private fun cargarCatalogo() {
        db.collection("productos").get()
            .addOnSuccessListener { resultado ->
                catalogo.removeAllViews()

                val lista = resultado.documents
                    .filter { (it.getString("estado") ?: "Disponible") == "Disponible" }
                    .sortedBy { (it.getString("nombre") ?: "").lowercase() }

                if (lista.isEmpty()) {
                    val vacio = TextView(this)
                    vacio.text = "Aún no hay productos publicados"
                    vacio.setTextColor(getColor(R.color.text_muted))
                    catalogo.addView(vacio)
                    return@addOnSuccessListener
                }

                for (documento in lista) {
                    val fila = LayoutInflater.from(this)
                        .inflate(R.layout.item_catalogo, catalogo, false)

                    val precio = documento.getLong("precio") ?: 0L
                    fila.findViewById<TextView>(R.id.tvCatNombre).text =
                        documento.getString("nombre") ?: ""
                    fila.findViewById<TextView>(R.id.tvCatPrecio).text =
                        "$" + NumberFormat.getInstance(Locale("es", "CO")).format(precio) + " COP"
                    fila.findViewById<TextView>(R.id.tvCatTallas).text =
                        "Tallas: " + (documento.getString("tallas") ?: "")

                    // Foto guardada como texto Base64 en Firestore
                    val foto = documento.getString("foto") ?: ""
                    if (foto.isNotEmpty()) {
                        try {
                            val bytes = Base64.decode(foto, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            fila.findViewById<ImageView>(R.id.imgCatalogo).setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Si la foto no se puede leer, se deja el fondo por defecto
                        }
                    }

                    // Al tocar el producto se abre su detalle usando su identificador
                    fila.setOnClickListener {
                        val intent = Intent(this, DetalleActivity::class.java)
                        intent.putExtra("productoId", documento.id)
                        startActivity(intent)
                    }

                    catalogo.addView(fila)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cargar el catálogo", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Hace que una vista abra el detalle de un producto fijo (promociones y
     * destacados), enviando su nombre, precio y foto.
     */
    private fun abrirDetalle(id: Int, nombre: String, precio: Int, foto: Int) {
        findViewById<View>(id).setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java)
            intent.putExtra("nombre", nombre)
            intent.putExtra("precio", precio)
            intent.putExtra("foto", foto)
            startActivity(intent)
        }
    }

    /** Convierte una medida en dp a píxeles según la pantalla del dispositivo. */
    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()
}