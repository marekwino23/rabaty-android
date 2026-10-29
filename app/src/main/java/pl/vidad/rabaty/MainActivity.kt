
package pl.vidad.rabaty

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

private const val STORE_URL = "https://vidad.webd.pl/rabaty/"
private const val STORE_HOST = "vidad.webd.pl"

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this)

        webView = WebView(this)
        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        )

        progressBar.max = 100

        container.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        container.addView(
            progressBar,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(4)
            )
        )

        setContentView(container)

        CookieManager.getInstance().setAcceptCookie(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
        }

        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()

                if (scheme == "https" || scheme == "http") {
                    // Stronę sklepu i jej podstrony otwieramy w aplikacji.
                    return false
                }

                // Inne protokoły, np. mailto: i tel:, przekazujemy
                // do aplikacji obsługującej dany typ linku.
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, uri)
                    )
                } catch (_: Exception) {
                    // Brak aplikacji obsługującej ten link.
                }

                return true
            }

            override fun onPageStarted(
                view: WebView,
                url: String?,
                favicon: Bitmap?
            ) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }

            override fun onPageFinished(
                view: WebView,
                url: String?
            ) {
                progressBar.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {
                progressBar.progress = newProgress
                progressBar.visibility =
                    if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            }
        )

        if (savedInstanceState == null) {
            webView.loadUrl(STORE_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
