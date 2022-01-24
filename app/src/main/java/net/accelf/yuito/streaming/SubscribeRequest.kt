package net.accelf.yuito.streaming

import com.google.gson.annotations.SerializedName

enum class StreamType(val type: String) {
    @SerializedName("user") USER("user"),
    @SerializedName("public") PUBLIC("public"),
    @SerializedName("public:local") LOCAL("public:local"),
    @SerializedName("list") LIST("list"),
    ;

    companion object {
        fun fromType(type: String) = values().find { type == it.type }!!
    }
}

data class SubscribeRequest(
    val type: RequestType,
    val stream: StreamType,
    val list: String?,
) {

    enum class RequestType {
        @SerializedName("subscribe") SUBSCRIBE,
        @SerializedName("unsubscribe") UNSUBSCRIBE,
    }

    companion object {
        fun fromSubscription(type: RequestType, subscription: Subscription) = SubscribeRequest(
            type = type,
            stream = subscription.stream,
            list = subscription.id?.toString(),
        )
    }
}
