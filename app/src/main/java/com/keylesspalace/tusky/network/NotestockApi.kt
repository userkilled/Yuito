package com.keylesspalace.tusky.network

import com.keylesspalace.tusky.entity.SearchResult
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface NotestockApi {

    @GET("api/v1/search.json")
    fun searchObservable(
            @Query("q") q: String,
            @Query("max_dt") maxDt: String? = null
    ): Single<SearchResult>

}
