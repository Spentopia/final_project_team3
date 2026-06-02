package com.ict.spentopia.feature.market // 이 파일이 속한 패키지 위치를 적음

// NFT 마켓 화면임
// 더미 UI 대신 WebView 래퍼

import android.annotation.SuppressLint // SuppressLint 기능을 가져옴
import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.graphics.Bitmap // 이미지 데이터 타입을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import android.view.ViewGroup // ViewGroup 기능을 가져옴
import android.webkit.CookieManager // CookieManager 기능을 가져옴
import android.webkit.ConsoleMessage // ConsoleMessage 기능을 가져옴
import android.webkit.WebChromeClient // WebChromeClient 기능을 가져옴
import android.webkit.WebResourceError // WebResourceError 기능을 가져옴
import android.webkit.WebResourceRequest // WebResourceRequest 기능을 가져옴
import android.webkit.WebResourceResponse // WebResourceResponse 기능을 가져옴
import android.webkit.WebSettings // WebSettings 기능을 가져옴
import android.webkit.WebView // WebView 기능을 가져옴
import android.webkit.WebViewClient // WebViewClient 기능을 가져옴
import android.webkit.RenderProcessGoneDetail // RenderProcessGoneDetail 기능을 가져옴
import androidx.activity.compose.BackHandler // BackHandler 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator // CircularProgressIndicator 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.DisposableEffect // DisposableEffect 기능을 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.runtime.key // key 기능을 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.viewinterop.AndroidView // AndroidView 기능을 가져옴
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ict.spentopia.BuildConfig // BuildConfig 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground
import com.ict.spentopia.ui.theme.spentopiaAppButtonColor
import com.ict.spentopia.ui.theme.spentopiaAppButtonContentColor
import kotlinx.coroutines.delay // delay 기능을 가져옴
import org.json.JSONObject // JSONObject 기능을 가져옴

private const val MARKET_WEBVIEW_TAG = "MarketWebView" // 마켓 관련 값을 저장함

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun MarketScreen( // MarketScreen 함수를 선언함
    isWalletConnected: Boolean = false, // 지갑 관련 값을 받음
    walletAddress: String = "", // 지갑 주소를 받음
    walletProvider: String = "", // 지갑 이름을 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 지갑 관련 값을 받음
    onNavigateBack: () -> Unit = {}, // onNavigateBack 때 실행할 함수를 받음
    webPath: String = "/nft-market", // webPath 값을 받음
    screenTitle: String = "NFT 마켓", // screenTitle 값을 받음
    isDarkTheme: Boolean = false // 앱 다크모드 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val context = LocalContext.current // 현재 화면 정보를 저장함
    val prefs = remember(context) { // 화면이 다시 그려져도 간단 저장소를 기억함
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    val accessToken = remember { // 화면이 다시 그려져도 접근 토큰을 기억함
        prefs.getString("access_token", "") ?: ""
    }
    val baseUrl = BuildConfig.NFT_MARKET_WEBVIEW_URL // 서버 주소를 저장함
    var marketUrl by remember { mutableStateOf("") } // 화면에서 바뀔 마켓 관련 값을 저장함
    val requestHeaders = remember { emptyMap<String, String>() } // 화면이 다시 그려져도 requestHeaders 값을 기억함

    LaunchedEffect(baseUrl, accessToken, walletAddress, walletProvider, isWalletConnected, webPath) { // 화면이 열리거나 값이 바뀔 때 실행함
        Log.d(MARKET_WEBVIEW_TAG, "BuildConfig.NFT_MARKET_WEBVIEW_URL=$baseUrl") // 개발자가 확인할 로그를 찍음
        Log.d(MARKET_WEBVIEW_TAG, "BuildConfig.API_BASE_URL=${BuildConfig.API_BASE_URL}") // 개발자가 확인할 로그를 찍음
        Log.d(MARKET_WEBVIEW_TAG, "accessTokenPresent=${accessToken.isNotBlank()}") // 개발자가 확인할 로그를 찍음
        Log.d( // 개발자가 확인할 로그를 찍음
            MARKET_WEBVIEW_TAG,
            "walletConnected=$isWalletConnected walletAddressPresent=${walletAddress.isNotBlank()} walletProvider=$walletProvider" // 지갑 관련 값을 정해줌
        )
    }

    var webView by remember { mutableStateOf<WebView?>(null) } // 화면에서 바뀔 webView 값을 저장함
    var webViewKey by remember { mutableStateOf(0) } // 화면에서 바뀔 webViewKey 값을 저장함
    var isLoading by remember { mutableStateOf(true) } // 화면에서 바뀔 로딩 여부를 저장함
    var errorMessage by remember { mutableStateOf<String?>(null) } // 화면에서 바뀔 오류 내용을 저장함
    var pageReady by remember { mutableStateOf(false) } // 화면에서 바뀔 pageReady 값을 저장함
    var loadedMarketUrl by remember { mutableStateOf("") } // 화면에서 바뀔 마켓 관련 값을 저장함
    val loadingBackground = MaterialTheme.colorScheme.background

    LaunchedEffect(accessToken, webViewKey, webPath, isDarkTheme) { // 화면이 열리거나 값이 바뀔 때 실행함
        marketUrl = "" // 마켓 관련 값을 정해줌
        pageReady = false // false 값을 pageReady 값에 넣음
        loadedMarketUrl = "" // 마켓 관련 값을 정해줌
        errorMessage = null // null 값을 오류 내용에 넣음

        if (accessToken.isBlank()) { // 조건이 맞는지 확인함
            isLoading = false // false 값을 로딩 여부에 넣음
            errorMessage = "로그인 정보가 없습니다. 다시 로그인해주세요." // 오류 내용을 정해줌
            return@LaunchedEffect // 화면이 열리거나 값이 바뀔 때 실행함
        }

        isLoading = true // true 값을 로딩 여부에 넣음

        marketUrl = buildFrontendWebViewUrl(webPath, isDarkTheme, accessToken) // 앱 토큰을 fragment로 전달해 웹뷰 인증 API 호출을 줄임
        Log.d(MARKET_WEBVIEW_TAG, "load frontend webview url=${marketUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
    }

    LaunchedEffect(webViewKey, marketUrl) { // 화면이 열리거나 값이 바뀔 때 실행함
        pageReady = false // false 값을 pageReady 값에 넣음
        isLoading = true // true 값을 로딩 여부에 넣음
        delay(4_000) // delay 함수를 실행함
        if (isLoading && errorMessage == null) { // 조건이 맞는지 확인함
            Log.d(MARKET_WEBVIEW_TAG, "hide native loader by timeout url=${marketUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
            isLoading = false // false 값을 로딩 여부에 넣음
        }
    }

    BackHandler { // 이 블록 안의 내용이 시작됨
        val currentWebView = webView // currentWebView 값을 저장함
        if (currentWebView?.canGoBack() == true) { // 조건이 맞는지 확인함
            currentWebView.goBack()
        } else { // 이 블록 안의 내용이 시작됨
            onNavigateBack() // 화면 이동 관련 함수를 실행함
        }
    }

    DisposableEffect(Unit) { // 이 블록 안의 내용이 시작됨
        onDispose { // 이 블록 안의 내용이 시작됨
            webView?.destroy()
            webView = null // null 값을 webView 값에 넣음
        }
    }

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(loadingBackground)
    ) { // 이 블록 안의 내용이 시작됨
        if (baseUrl.isBlank()) { // 조건이 맞는지 확인함
            MarketWebViewError( // Market Web View Error 함수를 실행함
                title = screenTitle, // screenTitle 값을 제목에 넣음
                message = "$screenTitle 주소가 설정되지 않았습니다.", // 메시지를 정해줌
                onRetry = {} // onRetry 때 실행할 함수를 정해줌
            )
        } else if (marketUrl.isBlank()) { // 이 블록 안의 내용이 시작됨
            // 마켓 진입 URL을 준비하는 동안 네이티브 로더만 보여줍니다.
        } else { // 이 블록 안의 내용이 시작됨
            key(webViewKey) { // 이 블록 안의 내용이 시작됨
                AndroidView( // Android View 함수를 실행함
                    modifier = Modifier.fillMaxSize(), // UI 크기나 여백 같은 모양을 정함
                    factory = { viewContext -> // factory 값을 정해줌
                        WebView(viewContext).apply { // 이 블록 안의 내용이 시작됨
                            layoutParams = ViewGroup.LayoutParams( // layoutParams 값을 정해줌
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            configureMarketWebView(loadingBackground) // configure Market Web View 함수를 실행함
                            setBackgroundColor(loadingBackground.toArgb())
                            isClickable = true // true 값을 isClickable인지 여부에 넣음
                            isFocusable = true // true 값을 isFocusable인지 여부에 넣음
                            isFocusableInTouchMode = true // true 값을 isFocusableInTouchMode인지 여부에 넣음
                            requestFocus() // request Focus 함수를 실행함
                            Log.d( // 개발자가 확인할 로그를 찍음
                                MARKET_WEBVIEW_TAG,
                                "settings js=${settings.javaScriptEnabled} dom=${settings.domStorageEnabled} viewport=${settings.useWideViewPort} overview=${settings.loadWithOverviewMode}" // js 값을 정해줌
                            )
                            webChromeClient = object : WebChromeClient() { // webChromeClient 값을 정해줌
                                override fun onProgressChanged(view: WebView?, newProgress: Int) { // onProgressChanged 함수를 선언함
                                    Log.d(MARKET_WEBVIEW_TAG, "progress=$newProgress url=${view?.url.orEmpty().toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
                                    if (newProgress >= 60) { // 조건이 맞는지 확인함
                                        pageReady = true // true 값을 pageReady 값에 넣음
                                        isLoading = false // false 값을 로딩 여부에 넣음
                                        view?.injectMarketSessionIfReady(
                                            accessToken = accessToken, // 접근 토큰을 접근 토큰에 넣음
                                            walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                                            walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                                            isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                                            isDarkTheme = isDarkTheme // 앱 테마 값을 웹뷰에 전달함
                                        )
                                    }
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean { // onConsoleMessage 함수를 선언함
                                    Log.d( // 개발자가 확인할 로그를 찍음
                                        MARKET_WEBVIEW_TAG,
                                        "console ${consoleMessage?.messageLevel()} ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()} ${consoleMessage?.message()}"
                                    )
                                    return true // 이 값을 함수 결과로 돌려줌
                                }
                            }
                            webViewClient = object : WebViewClient() { // webViewClient 값을 정해줌
                                override fun shouldOverrideUrlLoading( // 데이터를 불러오는 함수 시작
                                    view: WebView?, // view 값을 받음
                                    request: WebResourceRequest? // 서버 요청값을 받음
                                ): Boolean { // 이 블록 안의 내용이 시작됨
                                    val requestedUrl = request?.url ?: return false // requestedUrl 값을 저장함
                                    val rewrittenUrl = rewriteLocalhostFrontendUrlIfNeeded(requestedUrl) // rewrittenUrl 값을 저장함
                                    if (rewrittenUrl != requestedUrl.toString()) { // 조건이 맞는지 확인함
                                        Log.d(MARKET_WEBVIEW_TAG, "rewrite redirect url ${requestedUrl.toString().toSafeMarketLogUrl()} -> ${rewrittenUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
                                        view?.loadUrl(rewrittenUrl)
                                        return true // 이 값을 함수 결과로 돌려줌
                                    }
                                    return false // 이 값을 함수 결과로 돌려줌
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { // onPageStarted 함수를 선언함
                                    Log.d(MARKET_WEBVIEW_TAG, "onPageStarted url=${url.orEmpty().toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
                                    val currentUri = url?.let { Uri.parse(it) } // currentUri 값을 저장함
                                    if (currentUri != null) { // 조건이 맞는지 확인함
                                        val rewrittenUrl = rewriteLocalhostFrontendUrlIfNeeded(currentUri) // rewrittenUrl 값을 저장함
                                        if (rewrittenUrl != url) { // 조건이 맞는지 확인함
                                            Log.d(MARKET_WEBVIEW_TAG, "rewrite started url ${url.toSafeMarketLogUrl()} -> ${rewrittenUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
                                            view?.stopLoading()
                                            view?.loadUrl(rewrittenUrl)
                                            return
                                        }
                                    }
                                    isLoading = true // true 값을 로딩 여부에 넣음
                                    pageReady = false // false 값을 pageReady 값에 넣음
                                    errorMessage = null // null 값을 오류 내용에 넣음
                                }

                                override fun onPageFinished(view: WebView?, url: String?) { // onPageFinished 함수를 선언함
                                    Log.d(MARKET_WEBVIEW_TAG, "onPageFinished url=${url.orEmpty().toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
                                    pageReady = true // true 값을 pageReady 값에 넣음
                                    isLoading = false // false 값을 로딩 여부에 넣음
                                    view?.injectMarketSessionIfReady(
                                        accessToken = accessToken, // 접근 토큰을 접근 토큰에 넣음
                                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                                        walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                                        isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                                        isDarkTheme = isDarkTheme // 앱 테마 값을 웹뷰에 전달함
                                    )
                                }

                                override fun onReceivedError( // onReceivedError 함수를 선언함
                                    view: WebView?, // view 값을 받음
                                    request: WebResourceRequest?, // 서버 요청값을 받음
                                    error: WebResourceError? // 오류 내용을 받음
                                ) { // 이 블록 안의 내용이 시작됨
                                    if (request?.isForMainFrame == true) { // 조건이 맞는지 확인함
                                        Log.e( // 개발자가 확인할 로그를 찍음
                                            MARKET_WEBVIEW_TAG,
                                            "onReceivedError url=${request.url.toString().toSafeMarketLogUrl()} code=${error?.errorCode} description=${error?.description}" // url 값을 정해줌
                                        )
                                        isLoading = false // false 값을 로딩 여부에 넣음
                                        errorMessage = error?.description?.toString() // 오류 내용을 정해줌
                                            ?: "NFT 마켓을 불러오지 못했습니다."
                                    }
                                }

                                override fun onReceivedHttpError( // onReceivedHttpError 함수를 선언함
                                    view: WebView?, // view 값을 받음
                                    request: WebResourceRequest?, // 서버 요청값을 받음
                                    errorResponse: WebResourceResponse? // 오류 내용을 받음
                                ) { // 이 블록 안의 내용이 시작됨
                                    if (request?.isForMainFrame == true && errorResponse != null) { // 조건이 맞는지 확인함
                                        Log.e( // 개발자가 확인할 로그를 찍음
                                            MARKET_WEBVIEW_TAG,
                                            "onReceivedHttpError url=${request.url.toString().toSafeMarketLogUrl()} status=${errorResponse.statusCode} reason=${errorResponse.reasonPhrase}" // url 값을 정해줌
                                        )
                                        isLoading = false // false 값을 로딩 여부에 넣음
                                        errorMessage = "NFT 마켓 응답 오류 (${errorResponse.statusCode})" // 오류 내용을 정해줌
                                    }
                                }

                                override fun onRenderProcessGone( // onRenderProcessGone 함수를 선언함
                                    view: WebView?, // view 값을 받음
                                    detail: RenderProcessGoneDetail? // detail 값을 받음
                                ): Boolean { // 이 블록 안의 내용이 시작됨
                                    Log.e( // 개발자가 확인할 로그를 찍음
                                        MARKET_WEBVIEW_TAG,
                                        "rendererGone didCrash=${detail?.didCrash()} priorityAtExit=${detail?.rendererPriorityAtExit()}" // didCrash 값을 정해줌
                                    )
                                    isLoading = false // false 값을 로딩 여부에 넣음
                                    errorMessage = "NFT 마켓 화면이 비정상 종료되었습니다. 다시 시도해 주세요." // 오류 내용을 정해줌
                                    if (view == webView) { // 조건이 맞는지 확인함
                                        webView = null // null 값을 webView 값에 넣음
                                    }
                                    view?.destroy()
                                    return true // 이 값을 함수 결과로 돌려줌
                                }
                            }
                            webView = this // this 값을 webView 값에 넣음
                            loadedMarketUrl = marketUrl // 마켓 관련 값을 마켓 관련 값에 넣음
                            Log.d(MARKET_WEBVIEW_TAG, "loadUrl url=${marketUrl.toSafeMarketLogUrl()} headers=${requestHeaders.keys}") // 개발자가 확인할 로그를 찍음
                            loadUrl(marketUrl, requestHeaders) // 데이터를 불러오는 함수를 실행함
                        }
                    },
                    update = { view -> // update 값을 정해줌
                        view.setBackgroundColor(loadingBackground.toArgb())
                        val currentUrl = view.url.orEmpty() // currentUrl 값을 저장함
                        if ( // 조건이 맞는지 확인함
                            loadedMarketUrl != marketUrl && // ! 값을 정해줌
                            marketUrl.isNotBlank() &&
                            currentUrl != marketUrl // ! 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Log.d(MARKET_WEBVIEW_TAG, "reloadUrl loaded=${loadedMarketUrl.toSafeMarketLogUrl()} current=${view.url.orEmpty().toSafeMarketLogUrl()} next=${marketUrl.toSafeMarketLogUrl()} headers=${requestHeaders.keys}") // 개발자가 확인할 로그를 찍음
                            isLoading = true // true 값을 로딩 여부에 넣음
                            pageReady = false // false 값을 pageReady 값에 넣음
                            errorMessage = null // null 값을 오류 내용에 넣음
                            loadedMarketUrl = marketUrl // 마켓 관련 값을 마켓 관련 값에 넣음
                            view.loadUrl(marketUrl, requestHeaders)
                        } else if (pageReady) { // 이 블록 안의 내용이 시작됨
                            view.injectMarketSessionIfReady(
                                accessToken = accessToken, // 접근 토큰을 접근 토큰에 넣음
                                walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                                walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                                isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                                isDarkTheme = isDarkTheme // 앱 테마 값을 웹뷰에 전달함
                            )
                        }
                    }
                )
            }
        }

        if (baseUrl.isNotBlank() && isLoading) { // 조건이 맞는지 확인함
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxSize()
                    .background(loadingBackground),
                contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "$screenTitle 불러오는 중",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        errorMessage?.let { message ->
            MarketWebViewError( // Market Web View Error 함수를 실행함
                title = screenTitle, // screenTitle 값을 제목에 넣음
                message = message, // 메시지를 메시지에 넣음
                onRetry = { // onRetry 때 실행할 함수를 정해줌
                    errorMessage = null // null 값을 오류 내용에 넣음
                    isLoading = true // true 값을 로딩 여부에 넣음
                    pageReady = false // false 값을 pageReady 값에 넣음
                    marketUrl = "" // 마켓 관련 값을 정해줌
                    loadedMarketUrl = "" // 마켓 관련 값을 정해줌
                    webViewKey += 1 // + 값을 정해줌
                }
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MarketWebViewError( // MarketWebViewError 함수를 선언함
    title: String, // 제목을 받음
    message: String, // 메시지를 받음
    onRetry: () -> Unit // onRetry 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        val isDark = MaterialTheme.colorScheme.background == SpentopiaDarkBackground
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "$title 화면을 불러오지 못했습니다.", // text 값을 정해줌
                style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onBackground // color 값을 정해줌
            )
            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = message, // 메시지를 text 값에 넣음
                style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = spentopiaAppButtonColor(isDark),
                    contentColor = spentopiaAppButtonContentColor(isDark)
                )
            ) { // 누를 수 있는 버튼을 만듦
                Text(text = "다시 시도") // 화면에 글자를 보여줌
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled") // 이 코드에 특별한 역할을 붙이는 표시
private fun WebView.configureMarketWebView(loadingBackground: Color) { // WebView 함수를 선언함
    settings.javaScriptEnabled = true // settings.javaScriptEnabled 값을 정해줌
    settings.domStorageEnabled = true // settings.domStorageEnabled 값을 정해줌
    settings.useWideViewPort = true // settings.useWideViewPort 값을 정해줌
    settings.loadWithOverviewMode = true // settings.loadWithOverviewMode 값을 정해줌
    settings.textZoom = 100 // settings.textZoom 값을 정해줌
    settings.cacheMode = WebSettings.LOAD_NO_CACHE // settings.cacheMode 값을 정해줌
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // settings.mixedContentMode 값을 정해줌
    settings.allowFileAccess = true // settings.allowFileAccess 값을 정해줌
    settings.allowContentAccess = true // settings.allowContentAccess 값을 정해줌
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    setBackgroundColor(loadingBackground.toArgb())
    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true) // set Renderer Priority Policy 함수를 실행함
}

private fun buildFrontendWebViewUrl(
    webPath: String,
    isDarkTheme: Boolean,
    accessToken: String
): String {
    val normalizedPath = if (webPath.startsWith("/")) webPath else "/$webPath"
    val frontendUri = Uri.parse(BuildConfig.NFT_MARKET_WEBVIEW_URL)

    val frontendUrl = frontendUri.buildUpon()
        .encodedPath(normalizedPath)
        .clearQuery()
        .appendQueryParameter("webview", "true")
        .appendQueryParameter("theme", if (isDarkTheme) "dark" else "light")
        .build()
        .toString()

    return "$frontendUrl#access_token=${Uri.encode(accessToken)}"
}

private fun String.toSafeMarketLogUrl(): String {
    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return this
    if (uri.fragment.isNullOrBlank()) return this
    return uri.buildUpon()
        .fragment("access_token=***")
        .build()
        .toString()
}

private fun rewriteLocalhostFrontendUrlIfNeeded(uri: Uri): String { // rewriteLocalhostFrontendUrlIfNeeded 함수를 선언함
    val frontendUri = Uri.parse(BuildConfig.NFT_MARKET_WEBVIEW_URL) // frontendUri 값을 저장함
    val shouldRewrite = // shouldRewrite 값을 저장함
        uri.scheme == "http" && // uri.scheme 값을 정해줌
            uri.host == "localhost" && // uri.host 값을 정해줌
            uri.port == 5173 && // uri.port 값을 정해줌
            frontendUri.host == "10.0.2.2" // frontendUri.host 값을 정해줌

    if (!shouldRewrite) { // 조건이 맞는지 확인함
        return uri.toString() // 이 값을 함수 결과로 돌려줌
    }

    return uri.buildUpon() // 이 값을 함수 결과로 돌려줌
        .encodedAuthority("10.0.2.2:${uri.port}")
        .build()
        .toString()
}

private fun isFrontendMarketUrl(url: String): Boolean { // isFrontendMarketUrl 함수를 선언함
    val currentUri = runCatching { Uri.parse(url) }.getOrNull() ?: return false // currentUri 값을 저장함
    val frontendUri = runCatching { Uri.parse(BuildConfig.NFT_MARKET_WEBVIEW_URL) }.getOrNull() // frontendUri 값을 저장함
        ?: return false

    return currentUri.scheme == frontendUri.scheme && // 이 값을 함수 결과로 돌려줌
        currentUri.host == frontendUri.host && // currentUri.host 값을 정해줌
        currentUri.port == frontendUri.port // currentUri.port 값을 정해줌
}

private fun WebView.injectMarketSessionIfReady( // WebView 함수를 선언함
    accessToken: String, // 접근 토큰을 받음
    walletAddress: String, // 지갑 주소를 받음
    walletProvider: String, // 지갑 이름을 받음
    isWalletConnected: Boolean, // 지갑 관련 값을 받음
    isDarkTheme: Boolean // 앱 다크모드 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val currentUrl = url.orEmpty() // currentUrl 값을 저장함
    if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) { // 조건이 맞는지 확인함
        Log.d(MARKET_WEBVIEW_TAG, "skip injectMarketSession url=${currentUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
        return
    }

    if (!isFrontendMarketUrl(currentUrl)) { // 조건이 맞는지 확인함
        Log.d(MARKET_WEBVIEW_TAG, "skip injectMarketSession nonFrontendUrl=${currentUrl.toSafeMarketLogUrl()}") // 개발자가 확인할 로그를 찍음
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
        window.localStorage.setItem('theme', ${JSONObject.quote(if (isDarkTheme) "dark" else "light")});
      } catch (e) {
        console.error('localStorage set failed', e);
      }

      try {
        var root = document.documentElement;
        root.classList.toggle('dark', $isDarkTheme);
        root.style.colorScheme = ${JSONObject.quote(if (isDarkTheme) "dark" else "light")};
      } catch (e) {
        console.error('theme apply failed', e);
      }

      window.dispatchEvent(new CustomEvent('spentopiaAndroidSession', {
        detail: {
          accessToken: ${JSONObject.quote(accessToken)},
          walletAddress: ${JSONObject.quote(walletAddress)},
          walletProvider: ${JSONObject.quote(walletProvider)},
          walletConnected: $isWalletConnected,
          theme: ${JSONObject.quote(if (isDarkTheme) "dark" else "light")}
        }
      }));

      window.dispatchEvent(new CustomEvent('spentopiaAndroidThemeChanged', {
        detail: {
          theme: ${JSONObject.quote(if (isDarkTheme) "dark" else "light")},
          dark: $isDarkTheme
        }
      }));
    })();
""".trimIndent()

    Log.d(MARKET_WEBVIEW_TAG, "injectMarketSession tokenPresent=${accessToken.isNotBlank()} dark=$isDarkTheme") // 개발자가 확인할 로그를 찍음
    evaluateJavascript(script, null) // evaluate Javascript 함수를 실행함
}
