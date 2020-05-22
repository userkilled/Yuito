package com.keylesspalace.tusky.entity

import com.google.gson.annotations.SerializedName

data class StreamEvent(
        var event: EventType,
        var payload: String
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
