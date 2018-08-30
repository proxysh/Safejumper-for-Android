package com.proxysh.safejumper.service.network.apimodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AuthResponse {
    @SerializedName("user")
    @Expose
    private AuthUserResponse user;

    @SerializedName("servers")
    @Expose
    private List<ServerResponse> servers;

    public AuthUserResponse getUser() {
        return user;
    }

    public void setUser(AuthUserResponse user) {
        this.user = user;
    }

    public List<ServerResponse> getServers() {
        return servers;
    }

    public void setServers(List<ServerResponse> servers) {
        this.servers = servers;
    }
}
