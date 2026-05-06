package com.ict.spentopia.feature.market

// NFT 마켓 화면임
// 더미 UI 대신 WebView 래퍼

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ict.spentopia.BuildConfig
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.WebviewIssueRequest
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import kotlinx.coroutines.delay
import retrofit2.HttpException
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
    val baseUrl = BuildConfig.NFT_MARKET_WEBVIEW_URL
    var marketUrl by remember { mutableStateOf("") }
    val requestHeaders = remember { emptyMap<String, String>() }

    LaunchedEffect(baseUrl, accessToken, walletAddress, walletProvider, isWalletConnected) {
        Log.d(MARKET_WEBVIEW_TAG, "BuildConfig.NFT_MARKET_WEBVIEW_URL=$baseUrl")
        Log.d(MARKET_WEBVIEW_TAG, "BuildConfig.API_BASE_URL=${BuildConfig.API_BASE_URL}")
        Log.d(MARKET_WEBVIEW_TAG, "accessTokenPresent=${accessToken.isNotBlank()}")
        Log.d(
            MARKET_WEBVIEW_TAG,
            "walletConnected=$isWalletConnected walletAddressPresent=${walletAddress.isNotBlank()} walletProvider=$walletProvider"
        )
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var webViewKey by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var loadedMarketUrl by remember { mutableStateOf("") }

    LaunchedEffect(accessToken, webViewKey) {
        marketUrl = ""
        pageReady = false
        loadedMarketUrl = ""
        errorMessage = null

        if (accessToken.isBlank()) {
            isLoading = false
            errorMessage = "로그인 정보가 없습니다. 다시 로그인해주세요."
            return@LaunchedEffect
        }

        isLoading = true

        try {
            Log.d(
                MARKET_WEBVIEW_TAG,
                "request webview token url=${buildWebviewIssueUrl()} authorizationPresent=${accessToken.isNotBlank()} xClientType=app contentType=application/json body={\"redirect_path\":\"/marketplace\"}"
            )
            val issueResponse = RetrofitClient.authApi.issueWebviewToken(
                request = WebviewIssueRequest(redirect_path = "/marketplace")
            )
            marketUrl = buildWebviewCallbackUrl(issueResponse.webview_token)
            Log.d(MARKET_WEBVIEW_TAG, "issued webview token expiresIn=${issueResponse.expires_in} callbackUrl=$marketUrl")
        } catch (e: HttpException) {
            isLoading = false
            val errorBody = e.response()?.errorBody()?.string().orEmpty()
            Log.e(
                MARKET_WEBVIEW_TAG,
                "issue webview token http error url=${buildWebviewIssueUrl()} code=${e.code()} message=${e.message()} errorBody=$errorBody",
                e
            )
            errorMessage = when (e.code()) {
                401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                else -> "NFT 마켓 로그인 연결에 실패했습니다. (${e.code()})"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "NFT 마켓 로그인 연결에 실패했습니다."
            Log.e(MARKET_WEBVIEW_TAG, "issue webview token failed", e)
        }
    }

    LaunchedEffect(webViewKey, marketUrl) {
        pageReady = false
        isLoading = true
        loadedMarketUrl = ""
        delay(4_000)
        if (isLoading && errorMessage == null) {
            Log.d(MARKET_WEBVIEW_TAG, "hide native loader by timeout url=$marketUrl")
            isLoading = false
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (baseUrl.isBlank()) {
            MarketWebViewError(
                message = "NFT 마켓 주소가 설정되지 않았습니다.",
                onRetry = {}
            )
        } else if (marketUrl.isBlank()) {
            // webview token 발급을 기다리는 동안 네이티브 로더만 보여줍니다.
        } else {
            key(webViewKey) {
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
                                    if (newProgress >= 60) {
                                        pageReady = true
                                        isLoading = false
                                        view?.injectMarketSessionIfReady(
                                            accessToken = accessToken,
                                            walletAddress = walletAddress,
                                            walletProvider = walletProvider,
                                            isWalletConnected = isWalletConnected
                                        )
                                    }
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    Log.d(
                                        MARKET_WEBVIEW_TAG,
                                        "console ${consoleMessage?.messageLevel()} ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()} ${consoleMessage?.message()}"
                                    )
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestedUrl = request?.url ?: return false
                                    val rewrittenUrl = rewriteLocalhostFrontendUrlIfNeeded(requestedUrl)
                                    if (rewrittenUrl != requestedUrl.toString()) {
                                        Log.d(MARKET_WEBVIEW_TAG, "rewrite redirect url $requestedUrl -> $rewrittenUrl")
                                        view?.loadUrl(rewrittenUrl)
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    Log.d(MARKET_WEBVIEW_TAG, "onPageStarted url=$url")
                                    val currentUri = url?.let { Uri.parse(it) }
                                    if (currentUri != null) {
                                        val rewrittenUrl = rewriteLocalhostFrontendUrlIfNeeded(currentUri)
                                        if (rewrittenUrl != url) {
                                            Log.d(MARKET_WEBVIEW_TAG, "rewrite started url $url -> $rewrittenUrl")
                                            view?.stopLoading()
                                            view?.loadUrl(rewrittenUrl)
                                            return
                                        }
                                    }
                                    isLoading = true
                                    pageReady = false
                                    errorMessage = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    Log.d(MARKET_WEBVIEW_TAG, "onPageFinished url=$url")
                                    pageReady = true
                                    isLoading = false
                                    view?.injectMarketSessionIfReady(
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

                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    Log.e(
                                        MARKET_WEBVIEW_TAG,
                                        "rendererGone didCrash=${detail?.didCrash()} priorityAtExit=${detail?.rendererPriorityAtExit()}"
                                    )
                                    isLoading = false
                                    errorMessage = "NFT 마켓 화면이 비정상 종료되었습니다. 다시 시도해 주세요."
                                    if (view == webView) {
                                        webView = null
                                    }
                                    view?.destroy()
                                    return true
                                }
                            }
                            webView = this
                            loadedMarketUrl = marketUrl
                            Log.d(MARKET_WEBVIEW_TAG, "loadUrl url=$marketUrl headers=${requestHeaders.keys}")
                            loadUrl(marketUrl, requestHeaders)
                        }
                    },
                    update = { view ->
                        if (loadedMarketUrl != marketUrl && marketUrl.isNotBlank()) {
                            Log.d(MARKET_WEBVIEW_TAG, "reloadUrl loaded=$loadedMarketUrl current=${view.url} next=$marketUrl headers=${requestHeaders.keys}")
                            isLoading = true
                            pageReady = false
                            errorMessage = null
                            loadedMarketUrl = marketUrl
                            view.loadUrl(marketUrl, requestHeaders)
                        } else if (pageReady) {
                            view.injectMarketSessionIfReady(
                                accessToken = accessToken,
                                walletAddress = walletAddress,
                                walletProvider = walletProvider,
                                isWalletConnected = isWalletConnected
                            )
                        }
                    }
                )
            }
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
                    pageReady = false
                    marketUrl = ""
                    loadedMarketUrl = ""
                    webViewKey += 1
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
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    settings.allowFileAccess = true
    settings.allowContentAccess = true
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
}

private fun buildWebviewIssueUrl(): String {
    val backendBaseUrl = BuildConfig.API_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }

    return Uri.parse(backendBaseUrl)
        .buildUpon()
        .appendPath("auth")
        .appendPath("webview")
        .appendPath("issue")
        .build()
        .toString()
}

private fun buildWebviewCallbackUrl(webviewToken: String): String {
    val backendBaseUrl = BuildConfig.API_BASE_URL.let { url ->
        if (url.endsWith("/")) url else "$url/"
    }

    return Uri.parse(backendBaseUrl)
        .buildUpon()
        .appendPath("auth")
        .appendPath("webview")
        .appendPath("callback")
        .appendQueryParameter("token", webviewToken)
        .build()
        .toString()
}

private fun rewriteLocalhostFrontendUrlIfNeeded(uri: Uri): String {
    val frontendUri = Uri.parse(BuildConfig.NFT_MARKET_WEBVIEW_URL)
    val shouldRewrite =
        uri.scheme == "http" &&
            uri.host == "localhost" &&
            uri.port == 5173 &&
            frontendUri.host == "10.0.2.2"

    if (!shouldRewrite) {
        return uri.toString()
    }

    return uri.buildUpon()
        .encodedAuthority("10.0.2.2:${uri.port}")
        .build()
        .toString()
}

private fun WebView.injectMarketSessionIfReady(
    accessToken: String,
    walletAddress: String,
    walletProvider: String,
    isWalletConnected: Boolean
) {
    val currentUrl = url.orEmpty()
    if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) {
        Log.d(MARKET_WEBVIEW_TAG, "skip injectMarketSession url=$currentUrl")
        return
    }

    val script = """
    (function() {
      try {
        window.localStorage.setItem('spentopia_webview', 'android');
        window.localStorage.setItem('spentopia_app_access_token', ${JSONObject.quote(accessToken)});
        window.localStorage.setItem('spentopia_wallet_connected', ${JSONObject.quote(isWalletConnected.toString())});
        window.localStorage.setItem('spentopia_wallet_address', ${JSONObject.quote(walletAddress)});
        window.localStorage.setItem('spentopia_wallet_provider', ${JSONObject.quote(walletProvider)});
      } catch (e) {
        console.error('localStorage set failed', e);
      }

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
