package com.maisonsantana.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)

        findViewById<Button>(R.id.btnIngresar).setOnClickListener {
            if (etCorreo.text.isBlank() || etContrasena.text.isBlank()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tvRecuperar).setOnClickListener {

        }
    }
}