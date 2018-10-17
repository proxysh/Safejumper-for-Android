package com.proxysh.shieldtra.service.network.apimodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AuthUserResponse {
    @SerializedName("type")
    @Expose
    private int type;

    @SerializedName("status")
    @Expose
    private int status;

    @SerializedName("expiration_date")
    @Expose
    private String expirationDate;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
}
