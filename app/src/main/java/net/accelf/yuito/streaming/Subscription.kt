package net.accelf.yuito.streaming

data class Subscription(
    val stream: StreamType,
    val id: Int? = null,
) {

    companion object {
        fun fromStreamList(stream: List<String>): Subscription {
            if (stream.isEmpty()) {
                throw IllegalArgumentException("empty stream list")
            }

            val type = StreamType.fromType(stream.first())

            if (type == StreamType.LIST) {
                if (stream.size < 2) {
                    throw IllegalArgumentException("missing list id")
                }

                return Subscription(
                    stream = type,
                    id = stream[1].toInt(),
                )
            }

            return Subscription(
                stream = type,
            )
        }
    }
}
