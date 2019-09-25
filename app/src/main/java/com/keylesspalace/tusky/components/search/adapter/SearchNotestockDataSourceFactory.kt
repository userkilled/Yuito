package com.keylesspalace.tusky.components.search.adapter

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.viewdata.StatusViewData
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.Executor

class SearchNotestockDataSourceFactory(
        private val notestockApi: NotestockApi,
        private val searchRequest: String?,
        private val disposables: CompositeDisposable,
        private val retryExecutor: Executor,
        private val cacheData: List<Pair<Status, StatusViewData.Concrete>>? = null,
        private val parser: (SearchResult?) -> List<Pair<Status, StatusViewData.Concrete>>) : DataSource.Factory<Int, Pair<Status, StatusViewData.Concrete>>() {
    val sourceLiveData = MutableLiveData<SearchNotestockDataSource>()
    override fun create(): DataSource<Int, Pair<Status, StatusViewData.Concrete>> {
        val source = SearchNotestockDataSource(notestockApi, searchRequest, disposables, retryExecutor, cacheData, parser)
        sourceLiveData.postValue(source)
        return source
    }
}