/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.os.Handler;
import android.util.Log;

import com.proxysh.safejumper.AppActivity;
import com.proxysh.safejumper.service.network.ApiService;
import com.proxysh.safejumper.service.network.apimodel.AuthBody;
import com.proxysh.safejumper.service.network.apimodel.AuthResponse;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;

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
    private Vector<HashMap<String, Object>> serverList = null;
    private HashMap<String, String> credentials = null;
    private PingScanningTask pingTask = null;
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

    public boolean registerTo(String username, String passwd, boolean enableAllLocation) {

        c.appLog("--------Fetching servers list----------");

        Document doc = null;

        new CompositeDisposable().add(
                ApiService.getInstance().getApiService().login(new AuthBody(username, passwd))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<AuthResponse>() {
                            @Override
                            public void onSuccess(AuthResponse authResponse) {
                                Log.d("IPChecker", authResponse.getUser().getExpirationDate());
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("IPChecker", e.getMessage());
                            }
                        })
        );

        try {
            HttpClient http_client = new DefaultHttpClient();
//			HttpConnectionParams.setConnectionTimeout(http_client.getParams(), 15000);
//			HttpConnectionParams.setSoTimeout(http_client.getParams(), 15000);
//			HttpUriRequest request = new HttpGet(SURL_ACCESS + "?u=" + username + "&p="+passwd);

            HttpPost request = new HttpPost(SURL_ACCESS);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("u", username));
            nameValuePairs.add(new BasicNameValuePair("p", passwd));
            nameValuePairs.add(new BasicNameValuePair("hub", enableAllLocation ? "0" : "1"));
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = http_client.execute(request);
            InputStream in = response.getEntity().getContent();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.parse(in);
        } catch (Exception e) {
            e.printStackTrace();
            c.appLog("Cannot connect to IPChecker server for authentication. Please check your internet connection.");
        }
        if (doc != null) {

            try {

                this.serverList = null;
                this.serverList = new Vector<HashMap<String, Object>>();
                this.credentials = null;
                this.credentials = new HashMap<String, String>();
                Element root = (Element) doc.getFirstChild();
                NodeList nl = root.getElementsByTagName(TAG_CREDENTIALS);
                if (nl.getLength() > 0) {
                    Element c = (Element) nl.item(0);
                    Element u = (Element) c.getElementsByTagName(TAG_USERNAME).item(0);
                    Element p = (Element) c.getElementsByTagName(TAG_PASSWORD).item(0);
                    this.credentials.put(TAG_USERNAME, u.getTextContent());
                    this.credentials.put(TAG_PASSWORD, p.getTextContent());
                }
                nl = root.getElementsByTagName(TAG_SERVER_LIST);
                if (nl.getLength() > 0) {
                    nl = ((Element) nl.item(0)).getElementsByTagName(TAG_ASERVER);
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element s = (Element) nl.item(i);
                        Element addr = (Element) s.getElementsByTagName(TAG_ADDRESS).item(0);
                        Element loc = (Element) s.getElementsByTagName(TAG_LOCATION).item(0);
                        Element load = (Element) s.getElementsByTagName(TAG_SERVERLOAD).item(0);

                        HashMap<String, Object> serverInfo = new HashMap<String, Object>();
                        serverInfo.put(TAG_ADDRESS, addr.getTextContent());
                        serverInfo.put(TAG_LOCATION, loc.getTextContent());
                        double d = 0.0;
                        try {
                            d = Double.parseDouble(load.getTextContent());
                        } catch (Exception e) {
                        }
                        if (d < 0) d -= 0.5;
                        else d += 0.5;
                        serverInfo.put(TAG_SERVERLOAD, Integer.valueOf((int) d));
                        serverInfo.put(TAG_PING_TIME, Integer.valueOf(-1));
                        this.serverList.add(serverInfo);
                    }

                    c.appLog("List of servers was obtained and validated. Server of list contains " + this.serverList.size() + " elements.");
                    if (this.serverList.size() > 0) {
                        if (downloadOvpnTemplate())
                            mustUseOvpnTemplate = true;
                        else
                            mustUseOvpnTemplate = false;
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                c.appLog("Failed to get the list of server. Most likely the problem of response parsing. Please check your version.");
            }
        }
        return false;
    }

    public boolean downloadOvpnTemplate() {

        c.appLog("--------Downloading openvpn config template file servers----------");
        try {
            URL u = new URL(SURL_OVPNTEMPLATE);
            URLConnection conn = u.openConnection();
            int contentLength = conn.getContentLength();

            DataInputStream stream = new DataInputStream(u.openStream());

            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();

            File outputFile = new File(c.getCacheDir(), OVPN_TEMPLATE_FILENAME);
            DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
            fos.write(buffer);
            fos.flush();
            fos.close();

            return true;
        } catch (Exception e) {
            return false; // swallow a 404
        }
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

    public Vector<HashMap<String, Object>> availableServerList() {
        return serverList;
    }

    public HashMap<String, Object> bestServerForVpn() {

        int count = this.serverList.size();

        if (count == 0) return null;
        if (count == 1) {
            return serverList.get(0);
        }

        HashMap<String, Object> bserver = this.serverList.get(0);
        float bloads = (Integer) bserver.get(TAG_SERVERLOAD);
        float bpings = (Integer) bserver.get(TAG_PING_TIME);
        if (bpings == -1)
            bpings = 10000000.0f;

        float bevals = (bloads + 1) * bpings;
        int bindex = 0;
        for (int i = 1; i < count; i++) {
            HashMap<String, Object> aserver = this.serverList.get(i);
            float loads = (Integer) aserver.get(TAG_SERVERLOAD);
            float pings = (Integer) aserver.get(TAG_PING_TIME);
            if (pings == -1) pings = 10000000.0f;
            float evals = pings * (loads + 1);
            if (evals < bevals) {
                bevals = evals;
                bindex = i;
            }
        }
        HashMap<String, Object> selected = this.serverList.get(bindex);
        c.appLog("Best VPN Server: [" + selected.get(TAG_ADDRESS) + "] Server Load=" + selected.get(TAG_SERVERLOAD) + "% Pings=" + selected.get(TAG_PING_TIME) + "ms");
        return selected;
    }

    public HashMap<String, Object> randomServerForVpn() {

        HashMap<String, Object> selected = null;
        int count = this.serverList.size();
        boolean loop = true;
        int i = 0;
        Random r = new Random();
        while (loop && i < count) {
            int rands = r.nextInt(count) % count;
            selected = this.serverList.get(rands);
            if (!selected.get(TAG_PING_TIME).equals(Integer.valueOf(-1)))
                break;

            i++;
        }
        return selected;
    }

    public HashMap<String, Object> serverForVpnAtIndex(int index) {

        if (index < this.serverList.size())
            return this.serverList.get(index);

        return null;
    }

    public HashMap<String, Object> serverForVpnByIp(String ip) {

        for (int i = 0; i < this.serverList.size(); i++) {
            HashMap<String, Object> as = this.serverList.get(i);
            if (ip.equals(as.get(TAG_ADDRESS).toString()))
                return as;
        }
        return null;
    }

    public HashMap<String, Object> serverForVpnByLocation(String loc) {
        for (int i = 0; i < this.serverList.size(); i++) {
            HashMap<String, Object> as = this.serverList.get(i);
            if (loc.equals(as.get(TAG_LOCATION).toString()))
                return as;
        }

        return null;
    }

    public static long getPingOfServer(String ip) {
        int timeOut = 3000;
        long time = 0;
        Boolean reachable;

        long BeforeTime = System.currentTimeMillis();
        try {
            reachable = InetAddress.getByName(ip).isReachable(timeOut);
            long AfterTime = System.currentTimeMillis();
            Long TimeDifference = AfterTime - BeforeTime;
            time = TimeDifference;

            Log.i("ping", ip + "===>" + time);
            return reachable ? time : -1;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void startPingTask(Handler h) {
        this.handler = h;
        if (pingTask == null) {
            pingTask = new PingScanningTask();
        }
        pingTask.startSync();
    }

    public void stopPingTask() {
        if (pingTask != null)
            pingTask.stopSync();
    }


    private final long IPCHECKER_SYNC_TIME = 5 * 60;
    private Handler handler;

    class PingScanningTask implements Runnable {

        private boolean isRunning;
        private boolean stopCmd;


        public boolean isRunning() {
            return isRunning;
        }

        @Override
        public void run() {

            stopCmd = false;
            isRunning = true;
            while (isRunning) {

                for (int i = 0; i < serverList.size(); i++) {
                    if (stopCmd)
                        break;

                    HashMap<String, Object> aserver = serverList.get(i);
                    String address = aserver.get(TAG_ADDRESS).toString();
                    long ms = getPingOfServer(address);
                    aserver.put(TAG_PING_TIME, Integer.valueOf((int) ms));
                    loadServerInfo(aserver);
                }
                if (stopCmd)
                    break;

                handler.sendEmptyMessage(100);

                for (int i = 0; i < IPCHECKER_SYNC_TIME && !stopCmd; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                if (stopCmd)
                    break;
            }

            isRunning = false;
        }

        public void stopSync() {
            stopCmd = true;
            while (isRunning) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void startSync() {
            if (isRunning)
                return;

            new Thread(this).start();
        }
    }
}


