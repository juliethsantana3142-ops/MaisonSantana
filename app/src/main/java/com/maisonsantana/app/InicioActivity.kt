package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class InicioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        abrirDetalle(R.id.imgPromo1, "Conjunto Blanco Primavera - Top Halter y Falda Volantes", 120000, R.drawable.promo_1)
        abrirDetalle(R.id.imgPromo2, "Conjunto Blanco Primavera - Vista Espalda", 120000, R.drawable.promo2)

        findViewById<Button>(R.id.btnNuevaColeccion).setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java)
            intent.putExtra("nombre", "Conjunto Blanco Primavera - Top Halter y Falda Volantes")
            intent.putExtra("precio", 120000)
            intent.putExtra("foto", R.drawable.promo_1)
            startActivity(intent)
        }

        abrirDetalle(R.id.card1, "Body Satinado de Tirantes con Perlas Maison Pearl", 48000, R.drawable.body_perlas)
        abrirDetalle(R.id.card2, "Vestido Corto Strapless Vino", 180000, R.drawable.vestido_vino)
        abrirDetalle(R.id.card3, "Vestido Negro Asimétrico", 220000, R.drawable.vestido_negro)

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
    }

    private fun abrirDetalle(id: Int, nombre: String, precio: Int, foto: Int) {
        findViewById<View>(id).setOnClickListener {
            val intent = Intent(this, DetalleActivity::class.java)
            intent.putExtra("nombre", nombre)
            intent.putExtra("precio", precio)
            intent.putExtra("foto", foto)
            startActivity(intent)
        }
    }
}