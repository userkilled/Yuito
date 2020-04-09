package com.keylesspalace.tusky.components.search.adapter

import androidx.lifecycle.MutableLiveData
import androidx.paging.PositionalDataSource
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.viewdata.StatusViewData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.concurrent.Executor

class SearchNotestockDataSource(
        private val notestockApi: NotestockApi,
        private val searchRequest: String,
        private val disposables: CompositeDisposable,
        private val retryExecutor: Executor,
        private val initialItems: List<Pair<Status, StatusViewData.Concrete>>? = null,
        private val parser: (SearchResult?) -> List<Pair<Status, StatusViewData.Concrete>>,
        private val source: SearchNotestockDataSourceFactory) : PositionalDataSource<Pair<Status, StatusViewData.Concrete>>() {

    val networkState = MutableLiveData<NetworkState>()

    private var retry: (() -> Any)? = null

    val initialLoad = MutableLiveData<NetworkState>()

    fun retry() {
        retry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Pair<Status, StatusViewData.Concrete>>) {
        if (!initialItems.isNullOrEmpty()) {
            callback.onResult(initialItems.toList(), 0)
        } else {
            networkState.postValue(NetworkState.LOADED)
            retry = null
            initialLoad.postValue(NetworkState.LOADING)
            notestockApi.searchObservable(
                    q = searchRequest)
                    .subscribe(
                            { data ->
                                val res = parser(data)
                                callback.onResult(res, params.requestedStartPosition)
                                initialLoad.postValue(NetworkState.LOADED)
                                try {
                                    source.lastDt = data.statuses.last().createdAt
                                } catch (e: NoSuchElementException) {
                                }
                            },
                            { error ->
                                retry = {
                                    loadInitial(params, callback)
                                }
                                initialLoad.postValue(NetworkState.error(error.message))
                            }
                    ).addTo(disposables)
        }

    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Pair<Status, StatusViewData.Concrete>>) {
        networkState.postValue(NetworkState.LOADING)
        retry = null
        if (source.exhausted || source.lastDt === null) {
            return callback.onResult(emptyList())
        }
        notestockApi.searchObservable(
                q = searchRequest,
                maxDt = source.iso8601.format(source.lastDt!!))
                .subscribe(
                        { data ->
                            val res = parser(data)
                            if (res.isEmpty()) {
                                source.exhausted = true
                            }
                            callback.onResult(res)
                            networkState.postValue(NetworkState.LOADED)
                            try {
                                source.lastDt = data.statuses.last().createdAt
                            } catch (e: NoSuchElementException) {
                            }
                        },
                        { error ->
                            retry = {
                                loadRange(params, callback)
                            }
                            networkState.postValue(NetworkState.error(error.message))
                        }
                ).addTo(disposables)
    }
}
