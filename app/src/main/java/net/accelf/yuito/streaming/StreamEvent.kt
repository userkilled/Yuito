package net.accelf.yuito.streaming

import com.google.gson.annotations.SerializedName

data class StreamEvent(
    val stream: List<String>,
    var event: EventType,
    var payload: String,
) {

    enum class EventType {
        UNKNOWN,

        @SerializedName("update")
        UPDATE,

        @SerializedName("notification")
        NOTIFICATION,

        @SerializedName("delete")
        DELETE,

        @SerializedName("filters_changed")
        FILTERS_CHANGED;
    }
}
