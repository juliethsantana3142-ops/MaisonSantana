package com.maisonsantana.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pasarela de pagos ePayco (Davipay), en modo de pruebas.
 * Carga el checkout oficial de ePayco (checkout.js) dentro de un WebView.
 * Al terminar el pago, ePayco redirige a una URL de respuesta con la
 * referencia "ref_payco"; la app consulta a ePayco el estado real de la
 * transacción (confirmación) y devuelve a PagoActivity el resultado.
 */
class PagoEpaycoActivity : AppCompatActivity() {

    companion object {
        // Llave PÚBLICA de ePayco (Configuración > Personalizaciones > Llaves secretas)
        const val LLAVE_PUBLICA = "df938a57aafd01d8670a3ef47d2668a4"

        // true = pagos de prueba, no mueve dinero real
        const val MODO_PRUEBAS = "true"

        // URL a la que ePayco regresa al terminar el pago
        const val URL_RESPUESTA = "https://maisonsantana.app/respuesta"

        // Consulta pública del estado de una transacción
        const val API_VALIDACION = "https://secure.epayco.co/validation/v1/reference/"
    }

    private lateinit var webView: WebView

    // Evita verificar la misma transacción más de una vez
    private var verificando = false

    /** Arma la página con el checkout de ePayco y el valor a pagar, y la carga en el WebView. */
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LLAVE_PUBLICA.startsWith("PEGA")) {
            Toast.makeText(this, "Falta configurar la llave pública de ePayco", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val total = intent.getIntExtra("total", 0)
        val factura = "MS-" + System.currentTimeMillis()

        // Página HTML mínima que abre el checkout de ePayco
        val html = """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="margin:0;background:#ffffff;">
            <script src="https://checkout.epayco.co/checkout.js"></script>
            <script>
              window.onload = function () {
                var handler = ePayco.checkout.configure({ key: '$LLAVE_PUBLICA', test: $MODO_PRUEBAS });
                handler.open({
                  name: 'Maison Santana',
                  description: 'Compra en Maison Santana',
                  invoice: '$factura',
                  currency: 'cop',
                  amount: '$total',
                  tax_base: '$total',
                  tax: '0',
                  country: 'co',
                  lang: 'es',
                  external: 'false',
                  response: '$URL_RESPUESTA',
                  confirmation: '$URL_RESPUESTA'
                });
              };
            </script>
            </body></html>
        """.trimIndent()

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        webView.webViewClient = object : WebViewClient() {
            // Detecta cuando ePayco redirige a la URL de respuesta
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return revisarRespuesta(request.url)
            }
        }

        setContentView(webView)
        webView.loadDataWithBaseURL("https://secure.payco.co/", html, "text/html", "UTF-8", null)
    }

    /**
     * Si la dirección es la URL de respuesta, toma la referencia ref_payco
     * y verifica la transacción. Devuelve true si fue interceptada.
     */
    private fun revisarRespuesta(destino: Uri): Boolean {
        if (!destino.toString().startsWith(URL_RESPUESTA)) return false
        val referencia = destino.getQueryParameter("ref_payco")
        if (referencia != null && !verificando) {
            verificando = true
            verificarTransaccion(referencia)
        }
        return true
    }

    /**
     * CONFIRMACIÓN: consulta a ePayco el estado real de la transacción.
     * Código de respuesta: 1 = aceptada, 2 = rechazada, 3 = pendiente, 4 = fallida.
     * Devuelve a PagoActivity el número de transacción y el estado.
     */
    private fun verificarTransaccion(referencia: String) {
        Toast.makeText(this, "Confirmando transacción...", Toast.LENGTH_SHORT).show()
        Thread {
            var estado = "ERROR"
            var transaccion = referencia
            try {
                val conexion = URL(API_VALIDACION + referencia).openConnection() as HttpURLConnection
                conexion.requestMethod = "GET"
                conexion.connectTimeout = 15000
                conexion.readTimeout = 15000
                val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }
                val datos = JSONObject(respuesta).getJSONObject("data")
                estado = when (datos.optString("x_cod_response")) {
                    "1" -> "APPROVED"
                    "2" -> "DECLINED"
                    "3" -> "PENDING"
                    else -> "ERROR"
                }
                val idTransaccion = datos.optString("x_transaction_id")
                if (idTransaccion.isNotBlank()) transaccion = idTransaccion
            } catch (e: Exception) {
                estado = "ERROR"
            }
            runOnUiThread {
                val resultado = Intent()
                resultado.putExtra("transaccionId", transaccion)
                resultado.putExtra("estado", estado)
                setResult(RESULT_OK, resultado)
                finish()
            }
        }.start()
    }

    /** Si el usuario retrocede en el WebView navega atrás; si no, cancela el pago. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
