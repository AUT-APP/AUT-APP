package com.example.autapp.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.view.ViewGroup
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun MazeMapView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Enable hardware acceleration
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                
                // Additional settings for better rendering
                mediaPlaybackRequiresUserGesture = false
                allowContentAccess = true
                allowFileAccess = true
                databaseEnabled = true
                setGeolocationEnabled(true)
                
                // Enable SVG support
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d("MazeMapView", "Page started loading: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("MazeMapView", "Page finished loading: $url")
                    // Inject CSS to fix SVG rendering
                    view?.evaluateJavascript("""
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = 'svg { width: 100% !important; height: 100% !important; } .maze-header { width: 400px !important; max-width: 400px !important; } .maze-header .search-button .maze-header-search-inset { font-size: 12px !important; line-height: 28px !important; border-width: 4px !important; padding: 4px 8px !important; } .maze-header .search-button .maze-header-search-inset svg { width: 12px !important; height: 12px !important; }';
                        document.head.appendChild(style);
                    """.trimIndent(), null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e("MazeMapView", "Error loading page: ${error?.description}")
                }
            }
            
            webChromeClient = WebChromeClient()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                Lifecycle.Event.ON_DESTROY -> webView.destroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { webView },
        update = { view ->
            view.loadUrl("https://use.mazemap.com/#v=1&campusid=103&zlevel=1&center=174.765877,-36.853388&zoom=16")
        }
    )
} 