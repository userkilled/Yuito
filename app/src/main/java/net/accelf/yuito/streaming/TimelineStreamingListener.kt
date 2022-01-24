package net.accelf.yuito.streaming

import android.util.Log
import com.google.gson.Gson
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.StatusDeletedEvent
import com.keylesspalace.tusky.appstore.StreamUpdateEvent
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Status
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class TimelineStreamingListener(
    private val eventHub: EventHub,
    private val gson: Gson,
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Stream connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = gson.fromJson(text, StreamEvent::class.java)
        val payload = event.payload
        when (event.event) {
            StreamEvent.EventType.UPDATE -> {
                val status = gson.fromJson(payload, Status::class.java)
                eventHub.dispatch(StreamUpdateEvent(status, Subscription.fromStreamList(event.stream)))
            }
            StreamEvent.EventType.DELETE -> eventHub.dispatch(StatusDeletedEvent(payload))
            StreamEvent.EventType.FILTERS_CHANGED -> eventHub.dispatch(PreferenceChangedEvent(Filter.HOME)) // It may be not a home but it doesn't matter
            else -> Log.d(TAG, "Unsupported event type.")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Stream closed")
    }

    companion object {
        private const val TAG = "StreamingListener"
    }
}
