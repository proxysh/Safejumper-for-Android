package com.proxysh.shieldtra.service.network;

import com.proxysh.shieldtra.service.network.apimodel.AuthBody;
import com.proxysh.shieldtra.service.network.apimodel.AuthResponse;
import com.proxysh.shieldtra.service.network.apimodel.ServerResponse;

import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface ApiServiceInterface {

    @POST("auth")
    Single<AuthResponse> login(@Body AuthBody body);

    @GET("servers")
    Single<List<ServerResponse>> getServerList();

    @GET("config")
    Single<ResponseBody> getServerConfigs(@QueryMap Map<String, String> serverParams);

}
