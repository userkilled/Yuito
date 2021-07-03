package net.accelf.yuito

import android.util.Log
import com.google.gson.Gson
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.StatusDeletedEvent
import com.keylesspalace.tusky.appstore.StreamUpdateEvent
import com.keylesspalace.tusky.components.timeline.TimelineViewModel
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.StreamEvent
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class TimelineStreamingListener(
    private val eventHub: EventHub,
    private val gson: Gson,
    private val kind: TimelineViewModel.Kind,
    private val identifier: String? = null,
) : WebSocketListener() {

    private val target = if (identifier == null) { kind.name } else { kind.name + ":" + identifier }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Stream connected to: $target")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = gson.fromJson(text, StreamEvent::class.java)
        val payload = event.payload
        when (event.event) {
            StreamEvent.EventType.UPDATE -> {
                val status = gson.fromJson(payload, Status::class.java)
                eventHub.dispatch(StreamUpdateEvent(status, kind, identifier))
            }
            StreamEvent.EventType.DELETE -> eventHub.dispatch(StatusDeletedEvent(payload))
            StreamEvent.EventType.FILTERS_CHANGED -> eventHub.dispatch(PreferenceChangedEvent(Filter.HOME)) // It may be not a home but it doesn't matter
            else -> Log.d(TAG, "Unsupported event type.")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Stream closed for: $target")
    }

    companion object {
        private const val TAG = "StreamingListener"
    }
}
