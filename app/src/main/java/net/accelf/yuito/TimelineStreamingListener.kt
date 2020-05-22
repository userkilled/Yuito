package net.accelf.yuito

import android.text.Spanned
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.PreferenceChangedEvent
import com.keylesspalace.tusky.appstore.StatusDeletedEvent
import com.keylesspalace.tusky.appstore.StreamUpdateEvent
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.entity.StreamEvent
import com.keylesspalace.tusky.fragment.TimelineFragment
import com.keylesspalace.tusky.json.SpannedTypeAdapter
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class TimelineStreamingListener(private val eventHub: EventHub,
                                private val kind: TimelineFragment.Kind) : WebSocketListener() {

    private val gson = buildGson()
    private var isFirstStatus = true

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Stream connected to: " + kind.name)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = gson.fromJson(text, StreamEvent::class.java)
        val payload = event.payload
        when (event.event) {
            StreamEvent.EventType.UPDATE -> {
                val status = gson.fromJson(payload, Status::class.java)
                eventHub.dispatch(StreamUpdateEvent(status, kind, isFirstStatus))
                if (isFirstStatus) {
                    isFirstStatus = false
                }
            }
            StreamEvent.EventType.DELETE -> eventHub.dispatch(StatusDeletedEvent(payload))
            StreamEvent.EventType.FILTERS_CHANGED -> eventHub.dispatch(PreferenceChangedEvent(Filter.HOME)) // It may be not a home but it doesn't matter
            else -> Log.d(TAG, "Unsupported event type.")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Stream closed for: " + kind.name)
    }

    companion object {
        private const val TAG = "StreamingListener"

        private fun buildGson(): Gson {
            return GsonBuilder()
                    .registerTypeAdapter(Spanned::class.java, SpannedTypeAdapter())
                    .create()
        }
    }

}
