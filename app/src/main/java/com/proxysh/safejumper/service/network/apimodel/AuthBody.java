package com.proxysh.safejumper.service.network.apimodel;

import com.google.gson.annotations.SerializedName;

public class AuthBody {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    public AuthBody(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
