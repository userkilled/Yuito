package com.keylesspalace.tusky.components.search.adapter

import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.viewdata.StatusViewData

class SearchNotestockPagingSourceFactory(
        private val notestockApi: NotestockApi,
        private val initialItems: List<StatusViewData.Concrete>? = null,
        private val parser: (SearchResult) -> List<StatusViewData.Concrete>
) : () -> SearchNotestockPagingSource {

    private var searchRequest: String = ""

    private var currentSource: SearchNotestockPagingSource? = null

    override fun invoke(): SearchNotestockPagingSource {
        return SearchNotestockPagingSource(
            notestockApi = notestockApi,
            searchRequest = searchRequest,
            initialItems = initialItems,
            parser = parser,
        ).also { source ->
            currentSource = source
        }
    }

    fun newSearch(newSearchRequest: String) {
        this.searchRequest = newSearchRequest
        currentSource?.invalidate()
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}
