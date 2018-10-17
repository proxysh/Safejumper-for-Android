package com.proxysh.shieldtra.service.network.apimodel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ServerResponse {
    @SerializedName("ip")
    @Expose
    private String ip;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("hostname")
    @Expose
    private String hostName;

    @SerializedName("iso_code")
    @Expose
    private String isoCode;

    @SerializedName("ports")
    @Expose
    private String ports;

    @SerializedName("ports_xor")
    @Expose
    private String portsXor;


    @SerializedName("server_load")
    @Expose
    private String serverLoad;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getPortsXor() {
        return portsXor;
    }

    public void setPortsXor(String portsXor) {
        this.portsXor = portsXor;
    }

    public String getServerLoad() {
        return serverLoad;
    }

    public void setServerLoad(String serverLoad) {
        this.serverLoad = serverLoad;
    }
}
