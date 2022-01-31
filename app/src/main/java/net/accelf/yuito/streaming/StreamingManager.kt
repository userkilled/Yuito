package net.accelf.yuito.streaming

import android.util.Log
import com.google.gson.Gson
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.network.MastodonApi
import net.accelf.yuito.streaming.SubscribeRequest.RequestType
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingManager @Inject constructor(
    private val eventHub: EventHub,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {

    private val url by lazy {
        HttpUrl.Builder()
            .scheme("https")
            .host(MastodonApi.PLACEHOLDER_DOMAIN)
            .addPathSegments("api/v1/streaming")
            .build()
            .toString()
            .replace("https://", "wss://")
    }

    private var webSocket: WebSocket? = null

    private val subscribing = mutableSetOf<Subscription>()

    val active
        get() = subscribing.isNotEmpty()

    private fun openSocket() {
        val request = Request.Builder()
            .url(url).build()
        val listener = TimelineStreamingListener(eventHub, gson)
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private fun closeSocket() {
        webSocket!!.close(1000, null)
    }

    private fun sendSubscribe(subscription: Subscription) {
        Log.d("StreamingManager", "Subscribed $subscription")
        webSocket!!.send(gson.toJson(SubscribeRequest.fromSubscription(RequestType.SUBSCRIBE, subscription)))
    }

    private fun sendUnsubscribe(subscription: Subscription) {
        Log.d("StreamingManager", "Unsubscribed $subscription")
        webSocket!!.send(gson.toJson(SubscribeRequest.fromSubscription(RequestType.UNSUBSCRIBE, subscription)))
    }

    fun subscribe(subscription: Subscription) {
        if (!subscribing.add(subscription)) {
            return
        }

        if (webSocket == null) {
            openSocket()
        }

        sendSubscribe(subscription)
    }

    fun unsubscribe(subscription: Subscription) {
        if (!subscribing.remove(subscription)) {
            return
        }

        if (webSocket == null) {
            return
        }

        if (subscribing.isEmpty()) {
            closeSocket()
            return
        }

        sendUnsubscribe(subscription)
    }

    fun pause() {
        if (webSocket == null) {
            return
        }

        closeSocket()
    }

    fun resume() {
        if (subscribing.isEmpty()) {
            return
        }

        openSocket()

        subscribing.forEach {
            sendSubscribe(it)
        }
    }
}
