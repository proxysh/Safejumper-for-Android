package com.proxysh.safejumper.service.network;

import com.proxysh.safejumper.service.network.apimodel.AuthBody;
import com.proxysh.safejumper.service.network.apimodel.AuthResponse;
import com.proxysh.safejumper.service.network.apimodel.ServerResponse;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiServiceInterface {

    @POST("auth")
    Single<AuthResponse> login(@Body AuthBody body);

    @GET("servers")
    Single<List<ServerResponse>> getServerList();

}
