package net.accelf.yuito.streaming

import android.util.Log
import com.google.gson.Gson
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.StatusDeletedEvent
import com.keylesspalace.tusky.appstore.StreamUpdateEvent
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.accelf.yuito.streaming.SubscribeRequest.RequestType.SUBSCRIBE
import net.accelf.yuito.streaming.SubscribeRequest.RequestType.UNSUBSCRIBE
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.coroutines.CoroutineContext

class MastodonStream(
    parent: Job,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val eventHub: EventHub,
    private val onStatusChange: (Boolean) -> Unit,
) : WebSocketListener(), CoroutineScope {

    private var webSocket: WebSocket? = null
    private val subscribing = mutableSetOf<Subscription>()

    private val job = SupervisorJob(parent).apply {
        invokeOnCompletion {
            webSocket?.let {
                closeSocket()
            }
        }
    }
    override val coroutineContext: CoroutineContext
        get() = job

    fun subscribe(subscription: Subscription) {
        if (!subscribing.add(subscription)) {
            // already subscribed
            return
        }

        if (webSocket == null) {
            openSocket()
        }

        send(SubscribeRequest.fromSubscription(SUBSCRIBE, subscription))
        Log.d(TAG, "Subscribed $subscription")
    }

    fun unsubscribe(subscription: Subscription) {
        if (!subscribing.remove(subscription)) {
            // already unsubscribed
            return
        }

        if (subscribing.isEmpty()) {
            closeSocket()
            return
        }

        send(SubscribeRequest.fromSubscription(UNSUBSCRIBE, subscription))
        Log.d(TAG, "Unsubscribed $subscription")
    }

    private fun openSocket() {
        val request = Request.Builder().url(STREAMING_URL).build()
        webSocket = okHttpClient.newWebSocket(request, this)
        onStatusChange(true)
    }

    private fun closeSocket() {
        webSocket!!.close(1000, null)
        webSocket = null
        onStatusChange(false)
    }

    private fun send(subscribeRequest: SubscribeRequest) {
        webSocket!!.send(gson.toJson(subscribeRequest))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Stream connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = gson.fromJson(text, StreamEvent::class.java)
        val payload = event.payload
        when (event.event) {
            StreamEvent.EventType.UPDATE -> {
                val status = gson.fromJson(payload, Status::class.java)
                launch {
                    eventHub.dispatch(StreamUpdateEvent(status, Subscription.fromStreamList(event.stream)))
                }
            }
            StreamEvent.EventType.DELETE -> launch {
                eventHub.dispatch(StatusDeletedEvent(payload))
            }
            StreamEvent.EventType.FILTERS_CHANGED -> launch {
                eventHub.dispatch(PreferenceChangedEvent(Filter.Kind.HOME.kind)) // It may be not a home but it doesn't matter
            }
            else -> Log.d(TAG, "Unsupported event type.")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Stream closed")
    }

    companion object {
        private const val TAG = "MastodonStream"

        private val STREAMING_URL by lazy {
            HttpUrl.Builder()
                .scheme("https")
                .host(MastodonApi.PLACEHOLDER_DOMAIN)
                .addPathSegments("api/v1/streaming")
                .build()
                .toString()
                .replace("https://", "wss://")
        }
    }
}
