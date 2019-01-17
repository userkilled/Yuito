package com.keylesspalace.tusky.components.search.adapter

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.paging.PositionalDataSource
import com.keylesspalace.tusky.entity.SearchResults
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.viewdata.StatusViewData
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.Executor

class SearchNotestockDataSource(
        private val notestockApi: NotestockApi,
        private val searchRequest: String?,
        private val disposables: CompositeDisposable,
        private val retryExecutor: Executor,
        private val initialItems: List<Pair<Status, StatusViewData.Concrete>>? = null,
        private val parser: (SearchResults?) -> List<Pair<Status, StatusViewData.Concrete>>) : PositionalDataSource<Pair<Status, StatusViewData.Concrete>>() {

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

    @SuppressLint("CheckResult")
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Pair<Status, StatusViewData.Concrete>>) {
        if (!initialItems.isNullOrEmpty()) {
            callback.onResult(initialItems, 0)
        } else {
            networkState.postValue(NetworkState.LOADED)
            retry = null
            initialLoad.postValue(NetworkState.LOADING)
            notestockApi.search(searchRequest)
                    .doOnSubscribe {
                        disposables.add(it)
                    }
                    .subscribe(
                            { data ->
                                val res = parser(data)
                                callback.onResult(res, params.requestedStartPosition)
                                initialLoad.postValue(NetworkState.LOADED)

                            },
                            { error ->
                                retry = {
                                    loadInitial(params, callback)
                                }
                                initialLoad.postValue(NetworkState.error(error.message))
                            }
                    )
        }

    }

    @SuppressLint("CheckResult")
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Pair<Status, StatusViewData.Concrete>>) {
        // Forbidden
    }
}