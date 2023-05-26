package net.accelf.yuito.streaming

import com.google.gson.Gson
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.di.ActivityScope
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import javax.inject.Inject

@ActivityScope
class StreamingManager @Inject constructor(
    private val eventHub: EventHub,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {

    private lateinit var stream: MastodonStream

    fun setup(parent: Job, onStatusChange: (Boolean) -> Unit) {
        stream = MastodonStream(parent, okHttpClient, gson, eventHub, onStatusChange)
    }

    fun subscribe(subscription: Subscription) {
        stream.subscribe(subscription)
    }

    fun unsubscribe(subscription: Subscription) {
        stream.unsubscribe(subscription)
    }
}
