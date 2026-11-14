package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
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

        findViewById<android.view.View>(R.id.card1).setOnClickListener {
            mostrar("Detalle del producto próximamente")
        }
        findViewById<android.view.View>(R.id.card2).setOnClickListener {
            mostrar("Detalle del producto próximamente")
        }
        findViewById<android.view.View>(R.id.card3).setOnClickListener {
            mostrar("Detalle del producto próximamente")
        }

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

    private fun mostrar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}