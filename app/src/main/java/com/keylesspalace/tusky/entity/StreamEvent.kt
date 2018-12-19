package com.keylesspalace.tusky.entity

import com.google.gson.annotations.SerializedName

data class StreamEvent(
        var event: EventType,
        var payload: String
) {

    enum class EventType(val num: Int) {
        UNKNOWN(0),
        @SerializedName("update")
        UPDATE(1),
        @SerializedName("notification")
        NOTIFICATION(2),
        @SerializedName("delete")
        DELETE(3);
    }

}