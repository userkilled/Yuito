package com.keylesspalace.tusky.components.search.adapter

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.viewdata.StatusViewData
import kotlinx.coroutines.rx3.await
import okhttp3.internal.UTC
import java.util.*

class SearchNotestockPagingSource(
    private val notestockApi: NotestockApi,
    private val searchRequest: String,
    private val initialItems: List<Pair<Status, StatusViewData.Concrete>>? = null,
    private val parser: (SearchResult) -> List<Pair<Status, StatusViewData.Concrete>>
) : PagingSource<Date, Pair<Status, StatusViewData.Concrete>>() {

    @Suppress("SpellCheckingInspection")
    private val iso8601 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        .apply { timeZone = UTC }

    override fun getRefreshKey(state: PagingState<Date, Pair<Status, StatusViewData.Concrete>>): Date? {
        return null
    }

    override suspend fun load(params: LoadParams<Date>): LoadResult<Date, Pair<Status, StatusViewData.Concrete>> {
        if (searchRequest.isEmpty()) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
        }

        if (params.key == null && !initialItems.isNullOrEmpty()) {
            return LoadResult.Page(
                data = initialItems.toList(),
                prevKey = null,
                nextKey = initialItems.last().first.createdAt
            )
        }

        try {
            val data = notestockApi.searchObservable(
                q = searchRequest,
                maxDt = params.key?.let { iso8601.format(it) },
            ).await()

            val res = parser(data)

            return LoadResult.Page(
                data = res,
                prevKey = null,
                nextKey = res.last().first.createdAt,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}
