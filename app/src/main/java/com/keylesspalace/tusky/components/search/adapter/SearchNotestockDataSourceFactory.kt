package com.keylesspalace.tusky.components.search.adapter

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.viewdata.StatusViewData
import io.reactivex.disposables.CompositeDisposable
import okhttp3.internal.UTC
import java.util.*
import java.util.concurrent.Executor

class SearchNotestockDataSourceFactory(
        private val notestockApi: NotestockApi,
        private val searchRequest: String,
        private val disposables: CompositeDisposable,
        private val retryExecutor: Executor,
        private val cacheData: List<Pair<Status, StatusViewData.Concrete>>? = null,
        private val parser: (SearchResult?) -> List<Pair<Status, StatusViewData.Concrete>>) : DataSource.Factory<Int, Pair<Status, StatusViewData.Concrete>>() {

    val sourceLiveData = MutableLiveData<SearchNotestockDataSource>()

    var lastDt: Date? = null
    var exhausted = false

    val iso8601 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    override fun create(): DataSource<Int, Pair<Status, StatusViewData.Concrete>> {
        iso8601.timeZone = UTC
        val source = SearchNotestockDataSource(notestockApi, searchRequest, disposables, retryExecutor, cacheData, parser, this)
        sourceLiveData.postValue(source)
        return source
    }
}
