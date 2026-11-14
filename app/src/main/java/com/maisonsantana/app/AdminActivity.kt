package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    private val roles = listOf("Administrador", "Vendedor", "Comprador")
    private val estados = listOf("Activo", "Inactivo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val prefs = getSharedPreferences("usuarios", Context.MODE_PRIVATE)
        val contenedor = findViewById<LinearLayout>(R.id.contenedorUsuarios)

        val correos = prefs.all.keys
            .filter { it.startsWith("pass_") }
            .map { it.removePrefix("pass_") }
            .sorted()

        if (correos.isEmpty()) {
            val vacio = TextView(this)
            vacio.text = "Aún no hay usuarios registrados"
            vacio.setPadding(0, 40, 0, 0)
            contenedor.addView(vacio)
        }

        for (correo in correos) {
            val fila = LayoutInflater.from(this)
                .inflate(R.layout.item_usuario, contenedor, false)

            fila.findViewById<TextView>(R.id.tvUsuarioNombre).text =
                prefs.getString("nombre_$correo", correo)
            fila.findViewById<TextView>(R.id.tvUsuarioCorreo).text = correo

            val spRol = fila.findViewById<Spinner>(R.id.spRolUsuario)
            val spEstado = fila.findViewById<Spinner>(R.id.spEstadoUsuario)
            spRol.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
            spEstado.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, estados)

            spRol.setSelection(roles.indexOf(prefs.getString("rol_$correo", "Comprador")).coerceAtLeast(0))
            spEstado.setSelection(estados.indexOf(prefs.getString("estado_$correo", "Activo")).coerceAtLeast(0))

            fila.findViewById<Button>(R.id.btnModificar).setOnClickListener {
                prefs.edit()
                    .putString("rol_$correo", spRol.selectedItem.toString())
                    .putString("estado_$correo", spEstado.selectedItem.toString())
                    .apply()
                Toast.makeText(this, "Permisos actualizados", Toast.LENGTH_SHORT).show()
            }

            contenedor.addView(fila)
        }

        findViewById<Button>(R.id.btnSalirAdmin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
