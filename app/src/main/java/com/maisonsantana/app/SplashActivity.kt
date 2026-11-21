package com.maisonsantana.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla de bienvenida (Splash).
 * Es la primera pantalla de la app (LAUNCHER): muestra el logo de
 * Maison Santana durante 2 segundos y luego abre el inicio de sesión.
 */
class SplashActivity : AppCompatActivity() {

    /** Se ejecuta al abrir la app: muestra el splash y programa el paso al login. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Espera 2 segundos y abre MainActivity (login)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}