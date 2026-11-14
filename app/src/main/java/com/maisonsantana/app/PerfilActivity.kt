package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val correo = getSharedPreferences("sesion", Context.MODE_PRIVATE)
            .getString("correo", "") ?: ""
        val usuarios = getSharedPreferences("usuarios", Context.MODE_PRIVATE)

        findViewById<TextView>(R.id.tvPerfilNombre).text =
            usuarios.getString("nombre_$correo", "-")
        findViewById<TextView>(R.id.tvPerfilCorreo).text = correo
        findViewById<TextView>(R.id.tvPerfilRol).text =
            usuarios.getString("rol_$correo", "Comprador")

        findViewById<TextView>(R.id.btnVolverPerfil).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            getSharedPreferences("sesion", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}