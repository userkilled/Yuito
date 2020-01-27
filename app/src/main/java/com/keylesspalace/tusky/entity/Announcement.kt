package com.keylesspalace.tusky.entity

import android.text.Spanned
import com.google.gson.annotations.SerializedName
import java.util.*

data class Announcement(
        val id: String,
        val content: Spanned,
        @SerializedName("starts_at") val startsAt: Date,
        @SerializedName("ends_at") val endsAt: Date,
        @SerializedName("all_day") val allDay: Boolean,
        val emojis: List<Emoji>,
        val mentions: Array<Status.Mention>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val announcement = other as Announcement?
        return id == announcement?.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
