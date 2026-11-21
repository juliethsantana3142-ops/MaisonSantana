package com.maisonsantana.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

/**
 * Pantalla de perfil del usuario.
 * Muestra el nombre, el correo y el rol de la sesión activa, da acceso al
 * historial de pedidos y permite cerrar la sesión.
 */
class PerfilActivity : AppCompatActivity() {

    /** Se ejecuta al abrir la pantalla: muestra los datos y configura los botones. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Datos de la sesión guardados al iniciar sesión
        val sesion = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        findViewById<TextView>(R.id.tvPerfilNombre).text = sesion.getString("nombre", "-")
        findViewById<TextView>(R.id.tvPerfilCorreo).text = sesion.getString("correo", "-")
        findViewById<TextView>(R.id.tvPerfilRol).text = sesion.getString("rol", "Comprador")

        findViewById<TextView>(R.id.btnVolverPerfil).setOnClickListener { finish() }

        val btnCerrar = findViewById<Button>(R.id.btnCerrarSesion)
        agregarBotonPedidos(btnCerrar)

        // Cerrar sesión: borra la sesión y el carrito, y vuelve al login
        btnCerrar.setOnClickListener {
            sesion.edit().clear().apply()
            getSharedPreferences("carrito", Context.MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /** Agrega el botón "Mis pedidos" justo encima del botón de cerrar sesión. */
    private fun agregarBotonPedidos(btnCerrar: Button) {
        val padre = btnCerrar.parent as LinearLayout

        val boton = Button(this)
        boton.text = "Mis pedidos"
        boton.isAllCaps = false
        boton.stateListAnimator = null
        boton.background = ContextCompat.getDrawable(this, R.drawable.bg_pill_gold)
        boton.backgroundTintList = null
        boton.typeface = ResourcesCompat.getFont(this, R.font.bree_serif)
        boton.setTextColor(getColor(R.color.white))

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (48 * resources.displayMetrics.density).toInt()
        )
        parametros.topMargin = (24 * resources.displayMetrics.density).toInt()
        boton.layoutParams = parametros

        boton.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
        padre.addView(boton, padre.indexOfChild(btnCerrar))
    }
}