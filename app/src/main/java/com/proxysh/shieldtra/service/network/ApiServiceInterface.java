package com.proxysh.shieldtra.service.network;

import com.proxysh.shieldtra.service.network.apimodel.AuthBody;
import com.proxysh.shieldtra.service.network.apimodel.AuthResponse;
import com.proxysh.shieldtra.service.network.apimodel.ServerResponse;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiServiceInterface {

    @POST("auth")
    Single<AuthResponse> login(@Body AuthBody body);

    @GET("servers")
    Single<List<ServerResponse>> getServerList();

}
