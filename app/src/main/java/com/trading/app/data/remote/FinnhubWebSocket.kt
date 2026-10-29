package com.trading.app.data.remote

import android.util.Log
import com.google.gson.Gson
import com.trading.app.data.remote.model.FinnhubTrade
import com.trading.app.data.remote.model.FinnhubTradeMessage
import com.trading.app.domain.model.FeedStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.random.Random

/**
 * Single, long-lived Finnhub WebSocket (the free tier allows one connection per key).
 *
 * Stability features:
 *  - automatic reconnect with exponential backoff + jitter (1s → 30s cap)
 *  - re-subscribes every symbol after each reconnect
 *  - protocol pings every 20s and a silence watchdog (no frames for 45s ⇒ force reconnect)
 *  - [reconnectNow] lets the app skip the backoff when the network comes back
 *
 * Wire protocol: wss://ws.finnhub.io?token=KEY, send {"type":"subscribe","symbol":"AAPL"}.
 */
class FinnhubWebSocket(
    okHttpClient: OkHttpClient,
    private val apiKey: String
) {
    private val tag = "FinnhubWebSocket"
    private val gson = Gson()

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .apply { interceptors().clear() } // token is already in the URL
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _trades = MutableSharedFlow<FinnhubTrade>(
        extraBufferCapacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val trades: SharedFlow<FinnhubTrade> = _trades.asSharedFlow()

    private val _status = MutableStateFlow(FeedStatus.CONNECTING)
    val status: StateFlow<FeedStatus> = _status.asStateFlow()

    private val wake = Channel<Unit>(Channel.CONFLATED)
    @Volatile private var lastFrameAt = 0L
    @Volatile private var socket: WebSocket? = null
    private var loop: Job? = null

    /** Opens the stream (idempotent) and keeps it alive until [stop]. */
    fun start(scope: CoroutineScope, symbols: List<String>) {
        if (loop?.isActive == true) return
        if (apiKey.isBlank()) {
            _status.value = FeedStatus.OFFLINE
            return
        }
        loop = scope.launch {
            var attempt = 0
            while (isActive) {
                _status.value = when {
                    attempt == 0 -> FeedStatus.CONNECTING
                    attempt < 4 -> FeedStatus.RECONNECTING
                    else -> FeedStatus.OFFLINE
                }
                val closed = CompletableDeferred<Unit>()
                val request = Request.Builder().url("wss://ws.finnhub.io?token=$apiKey").build()
                val opened = CompletableDeferred<Unit>()
                val ws = client.newWebSocket(request, Listener(symbols, opened, closed))
                socket = ws

                val watchdog = launch {
                    while (isActive) {
                        delay(10_000)
                        if (opened.isCompleted && System.currentTimeMillis() - lastFrameAt > 45_000) {
                            Log.w(tag, "No frames for 45s, forcing reconnect")
                            ws.cancel()
                        }
                    }
                }
                closed.await()
                watchdog.cancel()
                socket = null
                if (!isActive) break

                attempt = if (opened.isCompleted) 1 else attempt + 1
                val backoff = backoffMillis(attempt)
                Log.d(tag, "Reconnecting in ${backoff}ms (attempt $attempt)")
                select<Unit> {
                    wake.onReceive { attempt = 1 }
                    onTimeout(backoff) {}
                }
            }
        }
    }

    /** Skip the remaining backoff (e.g. connectivity regained). */
    fun reconnectNow() {
        wake.trySend(Unit)
    }

    fun stop() {
        loop?.cancel()
        loop = null
        socket?.close(1000, "stopped")
        socket = null
    }

    private inner class Listener(
        private val symbols: List<String>,
        private val opened: CompletableDeferred<Unit>,
        private val closed: CompletableDeferred<Unit>
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(tag, "Connected, subscribing to ${symbols.size} symbols")
            lastFrameAt = System.currentTimeMillis()
            opened.complete(Unit)
            _status.value = FeedStatus.LIVE
            symbols.forEach { webSocket.send("""{"type":"subscribe","symbol":"$it"}""") }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            lastFrameAt = System.currentTimeMillis()
            parseTrades(text).forEach { _trades.tryEmit(it) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(tag, "Closed: $code $reason")
            closed.complete(Unit)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(tag, "Failure (${response?.code}): ${t.message}")
            closed.complete(Unit)
        }
    }

    /** Parses a Finnhub frame. Returns trades for `type=trade`, nothing for ping/other. */
    fun parseTrades(text: String): List<FinnhubTrade> = try {
        val msg = gson.fromJson(text, FinnhubTradeMessage::class.java)
        if (msg?.type == "trade") msg.data.orEmpty().filter { it.price > 0 } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    companion object {
        fun backoffMillis(attempt: Int): Long {
            val base = 1000L shl min(attempt - 1, 5).coerceAtLeast(0) // 1,2,4,8,16,32s
            return min(base, 30_000L) + Random.nextLong(0, 500)
        }
    }
}
