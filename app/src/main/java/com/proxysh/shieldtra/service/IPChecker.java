/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.shieldtra.service;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.proxysh.shieldtra.AppActivity;
import com.proxysh.shieldtra.SignInCallbackInterface;
import com.proxysh.shieldtra.openvpn.ConfigManager;
import com.proxysh.shieldtra.service.network.ApiService;
import com.proxysh.shieldtra.service.network.apimodel.AuthBody;
import com.proxysh.shieldtra.service.network.apimodel.AuthResponse;
import com.proxysh.shieldtra.service.network.apimodel.ServerResponse;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import okhttp3.ResponseBody;

public class IPChecker {

    //service URL
    private static final String SURL_ACCESS = "https://proxy.sh/access.php";
    private static final String SURL_LOAD = "https://proxy.sh/load.php";
    private static final String SURL_OVPNTEMPLATE = "https://proxy.sh/ovpn-android.tpl";
    public static final String OVPN_TEMPLATE_FILENAME = "ovpn.template";
    //Server Data Tag
    private static final String TAG_ROOT = "account_info";
    private static final String TAG_CREDENTIALS = "credentials";
    private static final String TAG_USERNAME = "username";
    private static final String TAG_PASSWORD = "password";
    private static final String TAG_CERTPATH = "certpath";
    private static final String TAG_SERVER_LIST = "server_list";
    private static final String TAG_SERVER_INFO = "server_info";
    private static final String TAG_ASERVER = "server";
    public static final String TAG_ADDRESS = "address";
    public static final String TAG_LOCATION = "location";
    public static final String TAG_SERVERLOAD = "server_load";
    private static final String TAG_SERVERADDRESS = "server_address";
    public static final String TAG_PING_TIME = "ping_ms";

    private AppActivity c;
    private List<ServerResponse> serverList = new ArrayList<>();
    private HashMap<String, String> credentials = new HashMap<>();
    public static boolean mustUseOvpnTemplate = false;

    private static IPChecker instance = null;

    private static void initialize(AppActivity c) {
        if (instance == null)
            instance = new IPChecker(c);
    }

    public static IPChecker getInstance(AppActivity c) {
        initialize(c);
        return instance;
    }

    private IPChecker(AppActivity c) {
        this.c = c;
    }

    public void registerTo(String username, String passwd, boolean enableAllLocation, SignInCallbackInterface callback) {
        c.appLog("--------Fetching servers list----------");
        (new CompositeDisposable()).add(
                ApiService.getInstance().getApiService().getServerList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(sList -> ApiService.getInstance().getApiService().login(new AuthBody(username, passwd))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(new DisposableSingleObserver<AuthResponse>() {
                                    @Override
                                    public void onSuccess(AuthResponse authResponse) {
                                        credentials.clear();
                                        credentials.put(TAG_USERNAME, username);
                                        credentials.put(TAG_PASSWORD, passwd);
                                        if (authResponse != null && authResponse.getUser() != null) {
                                            serverList.clear();
                                            serverList.addAll(authResponse.getServers());
                                            if (serverList.size() > 0) {
                                                c.appLog("List of servers was obtained and validated. Server of list contains "
                                                        + serverList.size() + " elements.");
//                                                mustUseOvpnTemplate = downloadOvpnTemplate();
                                            } else {
                                                c.appLog("Failed to get the list of server. Most likely the problem of response parsing. Please check your version.");
                                            }
                                            callback.signInFinished(true);
                                        } else if (authResponse != null && authResponse.getErrorMsg() != null) {
                                            callback.signInFinished(false);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        c.appLog("Cannot connect to IPChecker server for authentication. Please check your internet connection.");
                                        callback.signInFinished(false);
                                    }
                                }))
                        .subscribeWith(new DisposableSingleObserver<List<ServerResponse>>() {
                            @Override
                            public void onSuccess(List<ServerResponse> serverResponses) {
                                if (serverResponses != null) {
                                    serverList.clear();
                                    serverList.addAll(serverResponses);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                c.appLog("Cannot connect to IPChecker server for fetching servers list. Please check your internet connection.");
                                callback.signInFinished(false);
                            }
                        })
        );
    }

    public void downloadOvpnTemplate(Map<String, String> serverParams) {
        c.appLog("--------Downloading openvpn config template file servers----------");
        (new CompositeDisposable()).add(
                ApiService.getInstance().getApiService().getServerConfigs(serverParams)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<ResponseBody>() {
                            @Override
                            public void onSuccess(ResponseBody responseBody) {
                                ConfigParser cp = new ConfigParser();
                                try {
                                    cp.parseConfig(responseBody.charStream());
                                    VpnProfile vp = cp.convertProfile();
                                    vp.mUsername = ConfigManager.activeUserName;
                                    vp.mPassword = ConfigManager.activePasswdOfUser;
                                    vp.mProfileCreator = "com.proxysh.shieldtra.service.IPChecker";
                                    // We don't want provisioned profiles to be editable
                                    vp.mUserEditable = false;

                                    vp.setUUID(UUID.randomUUID());
                                    vp.mServerName = serverParams.get("ip");

                                    if (c != null){
                                        ProfileManager pm = ProfileManager.getInstance(c);

                                        // The add method will replace any older profiles with the same UUID
                                        pm.addProfile(vp);
                                        pm.saveProfile(c, vp);
                                        pm.saveProfileList(c);
                                        c.startVpn(vp);
                                    }
                                } catch (ConfigParser.ConfigParseError | IOException | IllegalArgumentException e) {
                                    VpnStatus.logException("Error during import of managed profile", e);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                c.appLog("Cannot connect to server for downloading openvpn config template file. Please check your internet connection.");
                            }
                        })
        );

//        try {
//            URL u = new URL(SURL_OVPNTEMPLATE);
//            URLConnection conn = u.openConnection();
//            int contentLength = conn.getContentLength();
//
//            DataInputStream stream = new DataInputStream(u.openStream());
//
//            byte[] buffer = new byte[contentLength];
//            stream.readFully(buffer);
//            stream.close();
//
//            File outputFile = new File(c.getCacheDir(), OVPN_TEMPLATE_FILENAME);
//            DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
//            fos.write(buffer);
//            fos.flush();
//            fos.close();
//
//            return true;
//        } catch (Exception e) {
//            return false; // swallow a 404
//        }
    }

    public boolean loadServerInfo(HashMap<String, Object> serverInfo) {

        Document doc = null;

        try {
            HttpClient http_client = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(http_client.getParams(), 15000);
            HttpConnectionParams.setSoTimeout(http_client.getParams(), 15000);
            HttpUriRequest request = new HttpGet(SURL_LOAD + "?ip=" + serverInfo.get(TAG_ADDRESS));
            HttpResponse response = http_client.execute(request);
            InputStream in = response.getEntity().getContent();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(in);
        } catch (Exception e) {
            e.printStackTrace();
            c.appLog("Cannot connect to IPChecker server. Please check your internet connection.");
        }
        if (doc != null) {

            try {

                Element root = (Element) doc.getFirstChild();
                Element addr = (Element) root.getElementsByTagName(TAG_SERVERADDRESS).item(0);
                Element load = (Element) root.getElementsByTagName(TAG_SERVERLOAD).item(0);
                String address = addr.getTextContent();
                if (address.equals(serverInfo.get(TAG_ADDRESS))) {

                    double d = 0.0;
                    try {
                        d = Double.parseDouble(load.getTextContent());
                    } catch (Exception e) {
                    }
                    if (d < 0) d -= 0.5;
                    else d += 0.5;
                    serverInfo.put(TAG_SERVERLOAD, Integer.valueOf((int) d));

                    c.appLog("Server Details(" + address + "): Load=" + (int) d + "%, Ping=" + serverInfo.get(TAG_PING_TIME).toString() + "ms");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        c.appLog("Error at synchronizing the detail of server[" + serverInfo.get(TAG_ADDRESS) + "].");
        return false;
    }

    public List<ServerResponse> availableServerList() {
        return serverList;
    }

    public ServerResponse randomServerForVpn() {
//        int index = ThreadLocalRandom.current().nextInt(0, serverList.size());
        int index = (int) (Math.random() * serverList.size());
        return serverList.get(index);
    }

    public ServerResponse serverForVpnAtIndex(int index) {

        if (index < this.serverList.size())
            return this.serverList.get(index);

        return null;
    }

    public ServerResponse serverForVpnByIp(String ip) {

        for (int i = 0; i < serverList.size(); i++) {
            ServerResponse as = serverList.get(i);
            if (ip.equals(as.getIp()))
                return as;
        }
        return null;
    }

    public ServerResponse serverForVpnByLocation(String loc) {
        for (int i = 0; i < serverList.size(); i++) {
            ServerResponse as = this.serverList.get(i);
            if (loc.equals(as.getIsoCode()))
                return as;
        }
        return null;
    }
}


