package com.ict.spentopia.feature.market

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ict.spentopia.BuildConfig
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import org.json.JSONObject

private const val MARKET_WEBVIEW_TAG = "MarketWebView"

@Composable
fun MarketScreen(
    isWalletConnected: Boolean = false,
    walletAddress: String = "",
    walletProvider: String = "",
    onWalletConnectClick: (SolanaWalletType) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    val accessToken = remember {
        prefs.getString("access_token", "") ?: ""
    }
    val baseUrl = BuildConfig.NFT_MARKET_WEBVIEW_URL.trim()
    val marketUrl = remember(baseUrl, accessToken, walletAddress, walletProvider, isWalletConnected) {
        buildMarketWebViewUrl(
            baseUrl = baseUrl,
            accessToken = accessToken,
            walletAddress = walletAddress,
            walletProvider = walletProvider,
            isWalletConnected = isWalletConnected
        )
    }
    val requestHeaders = remember(accessToken, walletAddress, walletProvider, isWalletConnected) {
        buildMarketHeaders(
            accessToken = accessToken,
            walletAddress = walletAddress,
            walletProvider = walletProvider,
            isWalletConnected = isWalletConnected
        )
    }

    LaunchedEffect(baseUrl, marketUrl, accessToken, walletAddress, walletProvider, isWalletConnected) {
        Log.d(MARKET_WEBVIEW_TAG, "BuildConfig.NFT_MARKET_WEBVIEW_URL=$baseUrl")
        Log.d(MARKET_WEBVIEW_TAG, "Resolved marketUrl=$marketUrl")
        Log.d(MARKET_WEBVIEW_TAG, "accessTokenPresent=${accessToken.isNotBlank()}")
        Log.d(
            MARKET_WEBVIEW_TAG,
            "walletConnected=$isWalletConnected walletAddressPresent=${walletAddress.isNotBlank()} walletProvider=$walletProvider"
        )
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (baseUrl.isBlank()) {
            MarketWebViewError(
                message = "NFT 마켓 주소가 설정되지 않았습니다.",
                onRetry = {}
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        configureMarketWebView()
                        Log.d(
                            MARKET_WEBVIEW_TAG,
                            "settings js=${settings.javaScriptEnabled} dom=${settings.domStorageEnabled} viewport=${settings.useWideViewPort} overview=${settings.loadWithOverviewMode}"
                        )
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                Log.d(MARKET_WEBVIEW_TAG, "progress=$newProgress url=${view?.url}")
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                Log.d(MARKET_WEBVIEW_TAG, "onPageStarted url=$url")
                                isLoading = true
                                errorMessage = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                Log.d(MARKET_WEBVIEW_TAG, "onPageFinished url=$url")
                                isLoading = false
                                view?.injectMarketSession(
                                    accessToken = accessToken,
                                    walletAddress = walletAddress,
                                    walletProvider = walletProvider,
                                    isWalletConnected = isWalletConnected
                                )
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                Log.e(
                                    MARKET_WEBVIEW_TAG,
                                    "onReceivedError url=${request.url} code=${error?.errorCode} description=${error?.description}"
                                )
                                isLoading = false
                                errorMessage = error?.description?.toString()
                                        ?: "NFT 마켓을 불러오지 못했습니다."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                        ) {
                            if (request?.isForMainFrame == true && errorResponse != null) {
                                Log.e(
                                    MARKET_WEBVIEW_TAG,
                                    "onReceivedHttpError url=${request.url} status=${errorResponse.statusCode} reason=${errorResponse.reasonPhrase}"
                                )
                                isLoading = false
                                errorMessage = "NFT 마켓 응답 오류 (${errorResponse.statusCode})"
                                }
                            }
                        }
                        webView = this
                        Log.d(MARKET_WEBVIEW_TAG, "loadUrl url=$marketUrl headers=${requestHeaders.keys}")
                        loadUrl(marketUrl, requestHeaders)
                    }
                },
                update = { view ->
                    if (view.url != marketUrl && marketUrl.isNotBlank()) {
                        Log.d(MARKET_WEBVIEW_TAG, "reloadUrl current=${view.url} next=$marketUrl headers=${requestHeaders.keys}")
                        isLoading = true
                        errorMessage = null
                        view.loadUrl(marketUrl, requestHeaders)
                    } else {
                        view.injectMarketSession(
                            accessToken = accessToken,
                            walletAddress = walletAddress,
                            walletProvider = walletProvider,
                            isWalletConnected = isWalletConnected
                        )
                    }
                }
            )
        }

        if (baseUrl.isNotBlank() && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let { message ->
            MarketWebViewError(
                message = message,
                onRetry = {
                    errorMessage = null
                    isLoading = true
                    webView?.loadUrl(marketUrl, requestHeaders)
                }
            )
        }
    }
}

@Composable
private fun MarketWebViewError(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NFT 마켓을 불러오지 못했습니다.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(text = "다시 시도")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureMarketWebView() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.textZoom = 100
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
}

private fun buildMarketWebViewUrl(
    baseUrl: String,
    accessToken: String,
    walletAddress: String,
    walletProvider: String,
    isWalletConnected: Boolean
): String {
    if (baseUrl.isBlank()) return ""

    val builder = Uri.parse(baseUrl).buildUpon()
        .appendQueryParameter("webview", "android")

    if (accessToken.isNotBlank()) {
        builder.appendQueryParameter("app_access_token", accessToken)
    }
    if (isWalletConnected && walletAddress.isNotBlank()) {
        builder.appendQueryParameter("wallet_address", walletAddress)
        builder.appendQueryParameter("wallet_provider", walletProvider)
    }

    return builder.build().toString()
}

private fun buildMarketHeaders(
    accessToken: String,
    walletAddress: String,
    walletProvider: String,
    isWalletConnected: Boolean
): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    if (accessToken.isNotBlank()) {
        headers["Authorization"] = "Bearer $accessToken"
    }
    if (isWalletConnected && walletAddress.isNotBlank()) {
        headers["X-Wallet-Address"] = walletAddress
        headers["X-Wallet-Provider"] = walletProvider
    }
    return headers
}

private fun WebView.injectMarketSession(
    accessToken: String,
    walletAddress: String,
    walletProvider: String,
    isWalletConnected: Boolean
) {
    val script = """
        (function() {
          window.localStorage.setItem('spentopia_webview', 'android');
          window.localStorage.setItem('spentopia_app_access_token', ${JSONObject.quote(accessToken)});
          window.localStorage.setItem('spentopia_wallet_connected', ${JSONObject.quote(isWalletConnected.toString())});
          window.localStorage.setItem('spentopia_wallet_address', ${JSONObject.quote(walletAddress)});
          window.localStorage.setItem('spentopia_wallet_provider', ${JSONObject.quote(walletProvider)});
          window.dispatchEvent(new CustomEvent('spentopiaAndroidSession', {
            detail: {
              accessToken: ${JSONObject.quote(accessToken)},
              walletAddress: ${JSONObject.quote(walletAddress)},
              walletProvider: ${JSONObject.quote(walletProvider)},
              walletConnected: $isWalletConnected
            }
          }));
        })();
    """.trimIndent()
    Log.d(MARKET_WEBVIEW_TAG, "injectMarketSession tokenPresent=${accessToken.isNotBlank()}")
    evaluateJavascript(script, null)
}
