/* Copyright 2021 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.timeline.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.keylesspalace.tusky.appstore.*
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Filter
import com.keylesspalace.tusky.entity.Poll
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.FilterModel
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.network.TimelineCases
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.viewdata.StatusViewData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.await
import net.accelf.yuito.streaming.StreamType
import net.accelf.yuito.streaming.StreamingManager
import net.accelf.yuito.streaming.Subscription
import retrofit2.HttpException
import java.io.IOException

abstract class TimelineViewModel(
    private val timelineCases: TimelineCases,
    private val api: MastodonApi,
    private val eventHub: EventHub,
    protected val accountManager: AccountManager,
    private val sharedPreferences: SharedPreferences,
    private val filterModel: FilterModel,
    private val streamingManager: StreamingManager,
) : ViewModel() {

    abstract val statuses: Flow<PagingData<StatusViewData>>

    var kind: Kind = Kind.HOME
        private set
    var id: String? = null
        private set
    var tags: List<String> = emptyList()
        private set

    protected var alwaysShowSensitiveMedia = false
    protected var alwaysOpenSpoilers = false
    private var filterRemoveReplies = false
    private var filterRemoveReblogs = false

    val shouldReplyInQuick by lazy {
        when (kind) {
            Kind.HOME,
            Kind.PUBLIC_LOCAL,
            Kind.PUBLIC_FEDERATED,
            Kind.TAG,
            Kind.FAVOURITES,
            Kind.LIST,
            -> true
            Kind.BOOKMARKS,
            Kind.USER,
            Kind.USER_PINNED,
            Kind.USER_WITH_REPLIES,
            -> false
        }
    }

    var isFirstOfStreaming = false
    private val subscription by lazy {
        when (kind) {
            Kind.HOME -> Subscription(StreamType.USER)
            Kind.PUBLIC_LOCAL -> Subscription(StreamType.LOCAL)
            Kind.PUBLIC_FEDERATED -> Subscription(StreamType.PUBLIC)
            Kind.LIST -> Subscription(StreamType.LIST, id?.toInt())
            Kind.TAG,
            Kind.USER,
            Kind.USER_PINNED,
            Kind.USER_WITH_REPLIES,
            Kind.FAVOURITES,
            Kind.BOOKMARKS,
            -> {
                throw NotImplementedError("streaming not implemented for this type")
            }
        }
    }
    var isStreamingEnabled = false
        set(value) {
            if (field != value) {
                field = value
                when (value) {
                    true -> {
                        streamingManager.subscribe(subscription)
                        isFirstOfStreaming = true
                    }
                    false -> {
                        streamingManager.unsubscribe(subscription)
                    }
                }
            }
        }

    fun init(
        kind: Kind,
        id: String?,
        tags: List<String>,
        isStreamingEnabled: Boolean,
    ) {
        this.kind = kind
        this.id = id
        this.tags = tags
        this.isStreamingEnabled = isStreamingEnabled

        if (kind == Kind.HOME) {
            filterRemoveReplies =
                !sharedPreferences.getBoolean(PrefKeys.TAB_FILTER_HOME_REPLIES, true)
            filterRemoveReblogs =
                !sharedPreferences.getBoolean(PrefKeys.TAB_FILTER_HOME_BOOSTS, true)
        }
        this.alwaysShowSensitiveMedia = accountManager.activeAccount!!.alwaysShowSensitiveMedia
        this.alwaysOpenSpoilers = accountManager.activeAccount!!.alwaysOpenSpoiler

        viewModelScope.launch {
            eventHub.events
                .asFlow()
                .collect { event -> handleEvent(event) }
        }

        reloadFilters()
    }

    fun reblog(reblog: Boolean, status: StatusViewData.Concrete): Job = viewModelScope.launch {
        try {
            timelineCases.reblog(status.actionableId, reblog).await()
        } catch (t: Exception) {
            ifExpected(t) {
                Log.d(TAG, "Failed to reblog status " + status.actionableId, t)
            }
        }
    }

    fun favorite(favorite: Boolean, status: StatusViewData.Concrete): Job = viewModelScope.launch {
        try {
            timelineCases.favourite(status.actionableId, favorite).await()
        } catch (t: Exception) {
            ifExpected(t) {
                Log.d(TAG, "Failed to favourite status " + status.actionableId, t)
            }
        }
    }

    fun bookmark(bookmark: Boolean, status: StatusViewData.Concrete): Job = viewModelScope.launch {
        try {
            timelineCases.bookmark(status.actionableId, bookmark).await()
        } catch (t: Exception) {
            ifExpected(t) {
                Log.d(TAG, "Failed to favourite status " + status.actionableId, t)
            }
        }
    }

    fun voteInPoll(choices: List<Int>, status: StatusViewData.Concrete): Job = viewModelScope.launch {
        val poll = status.status.actionableStatus.poll ?: run {
            Log.w(TAG, "No poll on status ${status.id}")
            return@launch
        }

        val votedPoll = poll.votedCopy(choices)
        updatePoll(votedPoll, status)

        try {
            timelineCases.voteInPoll(status.actionableId, poll.id, choices).await()
        } catch (t: Exception) {
            ifExpected(t) {
                Log.d(TAG, "Failed to vote in poll: " + status.actionableId, t)
            }
        }
    }

    abstract fun updatePoll(newPoll: Poll, status: StatusViewData.Concrete)

    abstract fun changeExpanded(expanded: Boolean, status: StatusViewData.Concrete)

    abstract fun changeContentShowing(isShowing: Boolean, status: StatusViewData.Concrete)

    abstract fun changeContentCollapsed(isCollapsed: Boolean, status: StatusViewData.Concrete)

    abstract fun removeAllByAccountId(accountId: String)

    abstract fun removeAllByInstance(instance: String)

    abstract fun removeStatusWithId(id: String)

    abstract fun loadMore(placeholderId: String)

    abstract fun handleReblogEvent(reblogEvent: ReblogEvent)

    abstract fun handleFavEvent(favEvent: FavoriteEvent)

    abstract fun handleBookmarkEvent(bookmarkEvent: BookmarkEvent)

    abstract fun handlePinEvent(pinEvent: PinEvent)

    abstract fun handleStreamUpdateEvent(status: Status)

    abstract fun fullReload()

    protected fun shouldFilterStatus(statusViewData: StatusViewData): Boolean {
        val status = statusViewData.asStatusOrNull()?.status ?: return false
        return status.inReplyToId != null && filterRemoveReplies ||
                status.reblog != null && filterRemoveReblogs ||
                filterModel.shouldFilterStatus(status.actionableStatus)
    }

    private fun onPreferenceChanged(key: String) {
        when (key) {
            PrefKeys.TAB_FILTER_HOME_REPLIES -> {
                val filter = sharedPreferences.getBoolean(PrefKeys.TAB_FILTER_HOME_REPLIES, true)
                val oldRemoveReplies = filterRemoveReplies
                filterRemoveReplies = kind == Kind.HOME && !filter
                if (oldRemoveReplies != filterRemoveReplies) {
                    fullReload()
                }
            }
            PrefKeys.TAB_FILTER_HOME_BOOSTS -> {
                val filter = sharedPreferences.getBoolean(PrefKeys.TAB_FILTER_HOME_BOOSTS, true)
                val oldRemoveReblogs = filterRemoveReblogs
                filterRemoveReblogs = kind == Kind.HOME && !filter
                if (oldRemoveReblogs != filterRemoveReblogs) {
                    fullReload()
                }
            }
            Filter.HOME, Filter.NOTIFICATIONS, Filter.THREAD, Filter.PUBLIC, Filter.ACCOUNT -> {
                if (filterContextMatchesKind(kind, listOf(key))) {
                    reloadFilters()
                }
            }
            PrefKeys.ALWAYS_SHOW_SENSITIVE_MEDIA -> {
                // it is ok if only newly loaded statuses are affected, no need to fully refresh
                alwaysShowSensitiveMedia =
                    accountManager.activeAccount!!.alwaysShowSensitiveMedia
            }
        }
    }

    private fun filterContextMatchesKind(
        kind: Kind,
        filterContext: List<String>,
    ): Boolean {
        // home, notifications, public, thread
        return when (kind) {
            Kind.HOME, Kind.LIST -> filterContext.contains(
                Filter.HOME
            )
            Kind.PUBLIC_FEDERATED, Kind.PUBLIC_LOCAL, Kind.TAG -> filterContext.contains(
                Filter.PUBLIC
            )
            Kind.FAVOURITES -> filterContext.contains(Filter.PUBLIC) || filterContext.contains(
                Filter.NOTIFICATIONS
            )
            Kind.USER, Kind.USER_WITH_REPLIES, Kind.USER_PINNED -> filterContext.contains(
                Filter.ACCOUNT
            )
            else -> false
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            is FavoriteEvent -> handleFavEvent(event)
            is ReblogEvent -> handleReblogEvent(event)
            is BookmarkEvent -> handleBookmarkEvent(event)
            is PinEvent -> handlePinEvent(event)
            is StreamUpdateEvent -> {
                if (isStreamingEnabled && event.subscription == subscription) {
                    handleStreamUpdateEvent(event.status)
                }
            }
            is MuteConversationEvent -> fullReload()
            is UnfollowEvent -> {
                if (kind == Kind.HOME) {
                    val id = event.accountId
                    removeAllByAccountId(id)
                }
            }
            is BlockEvent -> {
                if (kind != Kind.USER && kind != Kind.USER_WITH_REPLIES && kind != Kind.USER_PINNED) {
                    val id = event.accountId
                    removeAllByAccountId(id)
                }
            }
            is MuteEvent -> {
                if (kind != Kind.USER && kind != Kind.USER_WITH_REPLIES && kind != Kind.USER_PINNED) {
                    val id = event.accountId
                    removeAllByAccountId(id)
                }
            }
            is DomainMuteEvent -> {
                if (kind != Kind.USER && kind != Kind.USER_WITH_REPLIES && kind != Kind.USER_PINNED) {
                    val instance = event.instance
                    removeAllByInstance(instance)
                }
            }
            is StatusDeletedEvent -> {
                if (kind != Kind.USER && kind != Kind.USER_WITH_REPLIES && kind != Kind.USER_PINNED) {
                    removeStatusWithId(event.statusId)
                }
            }
            is PreferenceChangedEvent -> {
                onPreferenceChanged(event.preferenceKey)
            }
        }
    }

    private fun reloadFilters() {
        viewModelScope.launch {
            val filters = try {
                api.getFilters().await()
            } catch (t: Exception) {
                Log.e(TAG, "Failed to fetch filters", t)
                return@launch
            }
            filterModel.initWithFilters(
                filters.filter {
                    filterContextMatchesKind(kind, it.context)
                }
            )
        }
    }

    private fun isExpectedRequestException(t: Exception) = t is IOException || t is HttpException

    private inline fun ifExpected(
        t: Exception,
        cb: () -> Unit,
    ) {
        if (isExpectedRequestException(t)) {
            cb()
        } else {
            throw t
        }
    }

    companion object {
        private const val TAG = "TimelineVM"
        internal const val LOAD_AT_ONCE = 30
    }

    enum class Kind {
        HOME, PUBLIC_LOCAL, PUBLIC_FEDERATED, TAG, USER, USER_PINNED, USER_WITH_REPLIES, FAVOURITES, LIST, BOOKMARKS
    }
}