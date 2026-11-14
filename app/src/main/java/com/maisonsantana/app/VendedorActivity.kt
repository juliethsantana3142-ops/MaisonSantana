package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VendedorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendedor)

        mensaje(R.id.btnNuevo, "Crear producto próximamente")
        mensaje(R.id.btnEditar1, "Editar Vestido Seda Gala")
        mensaje(R.id.btnEditar2, "Editar Chaqueta Cuero Sintético")
        mensaje(R.id.btnFoto1, "Abrir cámara próximamente")
        mensaje(R.id.btnFoto2, "Abrir cámara próximamente")
        mensaje(R.id.navInventario, "Inventario")
        mensaje(R.id.navReportes, "Reportes de ventas próximamente")
        mensaje(R.id.navPedidos, "Pedidos próximamente")
        mensaje(R.id.navBuscar, "Búsqueda próximamente")

        findViewById<View>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun mensaje(id: Int, texto: String) {
        findViewById<View>(id).setOnClickListener {
            Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
        }
    }
}








