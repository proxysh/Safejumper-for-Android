package com.proxysh.safejumper.service.network;

import com.proxy.sh.safejumper.BuildConfig;

import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiService {

    private static ApiService instance = null;
    private static ApiServiceInterface apiService = null;
    private static final String BASE_URL = "https://shieldtra.com/";


    private ApiService() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(new OkHttpClient().newBuilder()
                        .addInterceptor(chain -> {
                            Request originalRequest = chain.request();
                            final Request.Builder requestBuilder = originalRequest.newBuilder();
//                            requestBuilder.addHeader("Content-Type", "application/json");
                            return chain.proceed(requestBuilder.build());
                        })
                        .addNetworkInterceptor(interceptor)
                .readTimeout(20, TimeUnit.SECONDS)
                .build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build()
                .create(ApiServiceInterface.class);
    }

    public static ApiService getInstance(){
        if (instance == null){
            instance = new ApiService();
        }
        return instance;
    }

    public ApiServiceInterface getApiService() {
        return apiService;
    }
}
