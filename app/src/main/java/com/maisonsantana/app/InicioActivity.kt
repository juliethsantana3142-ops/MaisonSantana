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

        findViewById<Button>(R.id.btnNuevaColeccion).setOnClickListener {
            mostrar("Nueva colección próximamente")
        }

        abrirDetalle(R.id.card1, "Body Satinado de Tirantes con Perlas Maison Pearl", 48000, R.drawable.body_perlas)
        abrirDetalle(R.id.card2, "Vestido Corto Strapless Vino", 180000, R.drawable.vestido_vino)
        abrirDetalle(R.id.card3, "Vestido Negro Asimétrico", 220000, R.drawable.vestido_negro)

        findViewById<TextView>(R.id.navHome).setOnClickListener {
            mostrar("Favoritos próximamente")
        }
        findViewById<TextView>(R.id.navSearch).setOnClickListener {
            mostrar("Búsqueda próximamente")
        }
        findViewById<TextView>(R.id.navCart).setOnClickListener {
            mostrar("Carrito próximamente")
        }
        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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

    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}