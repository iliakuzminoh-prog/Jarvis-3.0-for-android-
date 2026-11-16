package com.urchi.jarvis

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class BrowserActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(11, 17, 24))
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        address = EditText(this).apply {
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Поиск или адрес сайта"
            textSize = 14f
            setPadding(12.dp, 0, 12.dp, 0)
        }
        bar.addView(address, LinearLayout.LayoutParams(0, 48.dp, 1f))
        bar.addView(Button(this).apply {
            text = "Открыть"
            setOnClickListener { navigate(address.text.toString()) }
        }, LinearLayout.LayoutParams(90.dp, 48.dp))
        root.addView(bar)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true
            }
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        navigate(intent.getStringExtra(EXTRA_QUERY).orEmpty())
    }

    private fun navigate(value: String) {
        val q = value.trim()
        if (q.isBlank()) {
            address.setText("https://www.google.com/")
            web.loadUrl("https://www.google.com/")
            return
        }
        val url = if (
            Regex("(?i)^(https?://|www\\.)").containsMatchIn(q) ||
            Regex("(?i)^([a-z0-9-]+\\.)+[a-z]{2,}(/.*)?$").matches(q)
        ) {
            if (q.startsWith("http", true)) q else "https://$q"
        } else {
            "https://www.google.com/search?q=" + android.net.Uri.encode(q)
        }
        address.setText(url)
        web.loadUrl(url)
    }

    override fun onDestroy() {
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_QUERY = "com.urchi.jarvis.BROWSER_QUERY"
    }
}
