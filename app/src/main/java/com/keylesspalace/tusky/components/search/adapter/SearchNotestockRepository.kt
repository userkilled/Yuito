package com.keylesspalace.tusky.components.search.adapter

import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.toLiveData
import com.keylesspalace.tusky.entity.SearchResult
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.NotestockApi
import com.keylesspalace.tusky.util.Listing
import com.keylesspalace.tusky.viewdata.StatusViewData
import io.reactivex.disposables.CompositeDisposable
import java.util.concurrent.Executors

class SearchNotestockRepository(private val notestockApi: NotestockApi) {

    private val executor = Executors.newSingleThreadExecutor()

    fun getSearchData(searchRequest: String?, disposables: CompositeDisposable, pageSize: Int = 20,
                      initialItems: List<Pair<Status, StatusViewData.Concrete>>? = null,
                      parser: (SearchResult?) -> List<Pair<Status, StatusViewData.Concrete>>): Listing<Pair<Status, StatusViewData.Concrete>> {
        val sourceFactory = SearchNotestockDataSourceFactory(notestockApi, searchRequest, disposables, executor, initialItems, parser)
        val livePagedList = sourceFactory.toLiveData(
                config = Config(pageSize = pageSize, enablePlaceholders = false, initialLoadSizeHint = pageSize * 2),
                fetchExecutor = executor
        )
        return Listing(
                pagedList = livePagedList,
                networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                    it.networkState
                },
                retry = {
                    sourceFactory.sourceLiveData.value?.retry()
                },
                refresh = {
                    sourceFactory.sourceLiveData.value?.invalidate()
                },
                refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                    it.initialLoad
                }

        )
    }

}