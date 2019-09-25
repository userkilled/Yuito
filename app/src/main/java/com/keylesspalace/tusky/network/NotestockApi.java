package com.keylesspalace.tusky.network;

import com.keylesspalace.tusky.entity.SearchResult;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NotestockApi {

    @GET("api/v1/search.json")
    Single<SearchResult> search(@Query("q") String q);

}
