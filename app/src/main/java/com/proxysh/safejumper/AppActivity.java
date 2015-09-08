/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.proxysh.safejumper.openvpn.ConfigManager;
import com.proxysh.safejumper.service.IPChecker;
import com.proxy.sh.safejumper.R;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.MainActivity;
import de.blinkt.openvpn.api.ExternalOpenVPNService;
import de.blinkt.openvpn.api.IOpenVPNAPIService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNService.LocalBinder;
import de.blinkt.openvpn.core.OpenVpnManagementThread;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus.LogItem;
import de.blinkt.openvpn.core.VpnStatus.LogListener;
import de.blinkt.openvpn.core.VpnStatus.StateListener;


public class AppActivity extends Activity implements OnClickListener, StateListener, ByteCountListener, LogListener  {

    private static final String TAG = "AppActivity";
    public static String SHORTCUT_CONNECT = "com.proxysh.safejumper.SHORTCUT_CONNECT";
	public static String SHORTCUT_DISCONNECT = "com.proxysh.safejumper.SHORTCUT_DISCONNECT";

    private ServiceSwitcher mServiceSwitcher;

    enum ActionType {
		SignInAction,
		PanelAction,
		SettingAction,
		LogAction,
		ConnectAction,
		SafejumpAction,
		DisconnectAction
	};

	enum VpnStatusLocal {
		Connecting, 
		Connected,
		Disconnected,
		Unknown
	};
	enum RequestStatus {
		WantConnect,
		WantDisconnect,
		WantSafejump,
	}
	private Button tabBtnConnect, tabBtnDisconnect, tabBtnSetting, tabBtnSafejump, tabBtnLogs, tabBtnPanel;
	private View viewSignIn, viewServers, viewProtos, viewSettings, viewLogs, viewPanel;
	private FrameLayout frameContent;
	private View currentView;
	private ActionType currentAction;
	private VpnStatusLocal lastState;
	private RequestStatus requestState;

	private SignInAction actSignin;
	private ServerListAction actServers;
	private ProtoListAction actPorts;
	private SettingsAction actSettings;
	private WebPanelAction actPanel;
	private LogsAction actLogs;

	private static boolean backgroundMode = false;

    private VpnProfile activeVpnProfile = null;

	@SuppressLint("HandlerLeak")
	private Handler pingHandler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 100){
				actServers.refreshLocations();
			}
		}
	};
	private boolean mCmfixed = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.i("widget", "Enter onCreate" + getIntent().getAction());

		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) { 
			// Activity was brought to front and not created, 
			// Thus finishing this will get us to the last viewed activity
			finish();
			return; 
		}
		setContentView(R.layout.activity_main);

		initialize();
		IPChecker.getInstance(this);
		ConfigManager.getInstance(this);
		ProfileManager.getInstance(this);

		actSignin = new SignInAction(viewSignIn, this);
		actServers = new ServerListAction(viewServers, this);
		actPorts = new ProtoListAction(viewProtos, this);
		actPanel = new WebPanelAction(viewPanel, this);
		actSettings = new SettingsAction(viewSettings, this);
		actLogs = new LogsAction(viewLogs, this);

		lastState = VpnStatusLocal.Disconnected;
		requestState = RequestStatus.WantConnect;

        mServiceSwitcher = new ServiceSwitcher();
	}

	/** from VPN permission dialog */
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        mServiceSwitcher.onActivityResult(requestCode,resultCode,data);
	}

	private void initialize() {

		frameContent = (FrameLayout) findViewById(R.id.frameContent);

		tabBtnConnect = (Button) findViewById(R.id.tabBtnConnect);
		tabBtnDisconnect = (Button) findViewById(R.id.tabBtnDisconnect);
		tabBtnSetting = (Button) findViewById(R.id.tabBtnSetting);
		tabBtnSafejump = (Button) findViewById(R.id.tabBtnSafejump);
		tabBtnLogs = (Button) findViewById(R.id.tabBtnLogs);
		tabBtnPanel = (Button) findViewById(R.id.tabBtnPanel);

		viewSignIn = getLayoutInflater().inflate(R.layout.tab_signin, null);
		viewServers = getLayoutInflater().inflate(R.layout.tab_servers, null);
		viewProtos = getLayoutInflater().inflate(R.layout.tab_protos, null);
		viewSettings = getLayoutInflater().inflate(R.layout.tab_settings, null);
		viewLogs = getLayoutInflater().inflate(R.layout.tab_logs, null);
		viewPanel = getLayoutInflater().inflate(R.layout.tab_panel, null);

		frameContent.removeAllViews();
		currentView = null;
		switchForAction(ActionType.SignInAction);
		setConnectableButtonBar(true);

		tabBtnDisconnect.setOnClickListener(this);
		tabBtnConnect.setOnClickListener(this);
		tabBtnLogs.setOnClickListener(this);
		tabBtnPanel.setOnClickListener(this);
		tabBtnSafejump.setOnClickListener(this);
		tabBtnSetting.setOnClickListener(this);
	}

	private void switchView(View v) {

		if (v == currentView)
			return;

		//		v.clearAnimation();
		frameContent.clearDisappearingChildren();
		if (currentView != null) {
			Animation outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
			//			currentView.startAnimation(outAnimation);
			frameContent.removeView(currentView);
		}
		Animation inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
		//		v.startAnimation(inAnimation);
		frameContent.addView(v);
		currentView = v;
	}

    public void cancelNotification() {
        mServiceSwitcher.cancelNotification();
    }

    private void setEnabledButtonBar(boolean enabled) {
		tabBtnConnect.setEnabled(enabled);
		tabBtnDisconnect.setEnabled(enabled);
		tabBtnLogs.setEnabled(enabled);
		//		tabBtnPanel.setEnabled(enabled);
		tabBtnSafejump.setEnabled(enabled);
		tabBtnSetting.setEnabled(enabled);
		if (!enabled) {
			tabBtnConnect.setSelected(false);
			tabBtnDisconnect.setSelected(false);
			tabBtnLogs.setSelected(false);
			tabBtnPanel.setSelected(false);
			tabBtnSafejump.setSelected(false);
			tabBtnSetting.setSelected(false);
		}
	}

	private void setConnectableButtonBar(boolean connectable) {
		tabBtnConnect.setVisibility(connectable ? View.VISIBLE:View.GONE);
		tabBtnDisconnect.setVisibility(connectable ? View.GONE: View.VISIBLE);
	}

	private void switchForAction(ActionType action) {

		if (currentAction == action)
			return;

		currentAction = action;
		tabBtnConnect.setSelected(false);
		tabBtnDisconnect.setSelected(false);
		tabBtnLogs.setSelected(false);
		tabBtnSafejump.setSelected(false);
		tabBtnSetting.setSelected(false);
		tabBtnPanel.setSelected(false);

		switch (action) {
		case SignInAction:
			switchView(viewSignIn);
			//			setEnabledButtonBar(false);
			break;
		case PanelAction:
			switchView(viewPanel);
			tabBtnPanel.setSelected(true);
			//			actPanel.loadPanel();
			break;
		case SettingAction:
			switchView(viewSettings);
			tabBtnSetting.setSelected(true);
			break;
		case LogAction:
			switchView(viewLogs);
			tabBtnLogs.setSelected(true);
			break;
		case SafejumpAction:
			switchView(viewServers);
			tabBtnSafejump.setSelected(true);
			break;
		case ConnectAction:
			switchView(viewServers);
			tabBtnConnect.setSelected(true);
			break;

		default:
			break;
		}

	}


	@Override
	public void onClick(View v) {

		int rid = v.getId();
		switch (rid) {
		case R.id.tabBtnDisconnect:
			if (ConfigManager.isLoginned) {
				requestState = RequestStatus.WantDisconnect;
				mServiceSwitcher.stopVpn(false);
			} else {
				switchForAction(ActionType.SignInAction);
			}
			break;
		case R.id.tabBtnConnect:
			if (ConfigManager.isLoginned)
				switchForAction(ActionType.ConnectAction);
			else
				switchForAction(ActionType.SignInAction);
			break;
		case R.id.tabBtnLogs:
			if (ConfigManager.isLoginned)
				switchForAction(ActionType.LogAction);
			else
				switchForAction(ActionType.SignInAction);
			break;
		case R.id.tabBtnSetting:
			if (ConfigManager.isLoginned)
				switchForAction(ActionType.SettingAction);
			else
				switchForAction(ActionType.SignInAction);
			break;
		case R.id.tabBtnSafejump:
			if (ConfigManager.isLoginned)
				switchForAction(ActionType.SafejumpAction);
			else
				switchForAction(ActionType.SignInAction);
			break;
		case R.id.tabBtnPanel:
			switchForAction(ActionType.PanelAction);

			break;

		default:
			break;
		}
	}

	public void onUpdateLocation() {
		actServers.loadLocations();
	}

	public void onSignIn() {
		//		setEnabledButtonBar(true);
		onUpdateConfigTemplate();
		actServers.loadLocations();

		ConfigManager.isLoginned = true;

        VpnStatus.addByteCountListener(this);
		VpnStatus.addLogListener(this);

		switchForAction(ActionType.ConnectAction);

		//If set Auto Connect option, auto-connect in case of existing last vpn
		if (ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_AUTO_CONNECT)) {

			if (ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_LAST_SUCCESS)) {

				VpnProfile profile = defaultProfile();
				profile.mServerName = ConfigManager.getInstance(this).prefStringForKey(ConfigManager.PK_LAST_VPNSERVER);
				profile.mServerPort = ConfigManager.getInstance(this).prefStringForKey(ConfigManager.PK_LAST_VPNPORT);
				profile.mUsername = ConfigManager.activeUserName;
				profile.mPassword = ConfigManager.activePasswdOfUser;
				switchForAction(ActionType.SettingAction);
				requestState = RequestStatus.WantConnect;
				if (profile.mServerName != null && !profile.mServerName.isEmpty())
					startVpn(profile);

			}
		}

        actSettings.setUserName(ConfigManager.activeUserName);
        actSettings.refreshSetting();       // uses mUseAidlService set in startVpn() above

		if (!ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_DISABLE_PING))
			//start ping scanning task
			IPChecker.getInstance(this).startPingTask(pingHandler);

        registerInternetConnectionListener();
    }

    private void registerInternetConnectionListener() {
        //register internet connection listener
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(internetConnectionListener, filter);
    }

    public void onSignOut() {
		switchForAction(ActionType.SignInAction);
		IPChecker.getInstance(this).stopPingTask();
		actSignin.loadPredefinedOption();

        mServiceSwitcher.removeStateListener();
        cancelNotification();
        try {
            unregisterReceiver(internetConnectionListener);
        }
        catch(IllegalArgumentException e) {
            // when not registered
        }
        ConfigManager.isLoginned = false;
        mServiceSwitcher.stopVpn(false);
        setConnectableButtonBar(true);
	}
	
    public void onExportProfile() {
        mServiceSwitcher.addCurrentProfile();
	}

	public void onUpdateConfigTemplate() {
		if (IPChecker.mustUseOvpnTemplate) {

			VpnProfile profile = ProfileManager.getInstance(this).getProfileByName(ConfigManager.DEFAULT_VPN_PROFILE);
			if (profile != null) {
				ProfileManager.getInstance(this).removeProfile(this, profile);
			}

			profile = importOvpnTemplate();
			if (profile != null) {
				ProfileManager.getInstance(this).addProfile(profile);
			}
		}
	}

	public void onSelectService(String protocol, String port) {

		switchForAction(ActionType.SettingAction);

		VpnProfile profile = defaultProfile();

		profile.mUseUdp = protocol.equals("UDP") ? true:false;
		profile.mServerPort = port;

		profile.mUsername = ConfigManager.activeUserName;
		profile.mPassword = ConfigManager.activePasswdOfUser;

		if (currentAction == ActionType.SafejumpAction)
			requestState = RequestStatus.WantSafejump;
		else
			requestState = RequestStatus.WantConnect;

		mServiceSwitcher.stopVpn(false);
		startVpn(profile);
	}

	public void onSelectServer(HashMap<String, Object> server) {

		VpnProfile profile = defaultProfile();
		profile.mServerName = server.get(IPChecker.TAG_ADDRESS).toString();

		if (tabBtnSafejump.isSelected()) {
			onSelectService("TCP", "443");
		} else {
			switchView(viewProtos);
		}
	}


	private VpnProfile defaultProfile() {

		VpnProfile profile = ProfileManager.getInstance(this).getProfileByName(ConfigManager.DEFAULT_VPN_PROFILE);
		if (profile == null) {
			profile = new VpnProfile(ConfigManager.DEFAULT_VPN_PROFILE);

			profile.mPersistTun = true;
			profile.mUsePull = true;
			profile.mUseLzo = true;
			profile.mUseRandomHostname = true;
			profile.mNobind = true;
			profile.mVerb = "3";
			profile.mOverrideDNS = false;
			profile.mAuthenticationType = VpnProfile.TYPE_USERPASS;
//			profile.mDNS1 = "8.8.8.8";
//			profile.mDNS2 = "8.8.4.4";
			profile.mSearchDomain = null;
			profile.mUseRandomHostname = false;
			if (ConfigManager.writeCaFile(this))
				profile.mCaFilename = this.getCacheDir() + "/proxysh.crt";

			profile.mUseUdp = false;
			profile.mServerPort = "443";
			profile.mServerName = "";
			ProfileManager.getInstance(this).addProfile(profile);
		}

		return profile;
	}

	private VpnProfile importOvpnTemplate() {
		
		try {
			//extract ca certificate file and ovpn config file from template file
			File cacheDir = getCacheDir();
			File templateFile = new File(cacheDir, IPChecker.OVPN_TEMPLATE_FILENAME);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile)));
			File caCertFile = new File(cacheDir, "proxysh_ca.crt");
			if (caCertFile.exists()) {
				caCertFile.delete();
			}
			File ovpnFile = new File(cacheDir, "ovpn.conf");
			if (ovpnFile.exists()) {
				ovpnFile.delete();
			}
			FileWriter writer = new FileWriter(ovpnFile);
			String line = null;
			String cfg = "";
			while ((line = reader.readLine()) != null) {
				if (line.equals("<ca>")) {
					FileWriter fw = new FileWriter(caCertFile);
					try {
						while ((line = reader.readLine()) != null && !line.equals("</ca>")) {
							fw.write(line);
							fw.write("\n");
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						fw.flush();
						fw.close();
					}
				}
                else if(line.contains("%%")) {
                    // We do not support tokens %% in the template, so skip such lines.
                    // Otherwise config parser fails.
                }
                else {
					writer.write(line);
					writer.write("\n");
				}
			}
			writer.flush();
			writer.close();
			
			//parse config 
			ConfigParser cp = new ConfigParser();
			InputStreamReader isr = new InputStreamReader(new FileInputStream(ovpnFile));
			cp.parseConfig(isr);
			VpnProfile vp = cp.convertProfile();
			isr.close();
			
			vp.mName = ConfigManager.DEFAULT_VPN_PROFILE;
			vp.mCaFilename = caCertFile.getAbsolutePath();
//			vp.mOverrideDNS = false;
//			vp.mDNS1 = "8.8.8.8";
//			vp.mDNS2 = "8.8.4.4";
			return vp;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public void appLog(final String s) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				actLogs.appLog(s);
			}
		});
	}

	@Override
	public void updateByteCount(final long in, final long out, final long diffin, final long diffout) {
		if (!ConfigManager.isLoginned)
			return;

		this.runOnUiThread(new Runnable() {
			public void run() {
				actSettings.setConnectionStatistics(diffin, diffout, OpenVpnManagementThread.mBytecountInterval, mServiceSwitcher.mUseAidlService);
			}
		});
	}

	@Override
	public void updateState(String state, String logmessage,
			int localizedResId, final ConnectionStatus level) {

		if (!ConfigManager.isLoginned)
			return;

		VpnStatusLocal currentStatus;

		switch (level) {
            case LEVEL_CONNECTED:
                currentStatus = VpnStatusLocal.Connected;
                break;
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
                currentStatus = VpnStatusLocal.Connecting;
                break;
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_VPNPAUSED:
            case LEVEL_NOTCONNECTED:
            case UNKNOWN_LEVEL:
            default:
                currentStatus = VpnStatusLocal.Disconnected;
                break;
		}

		if (lastState == currentStatus)
			return;

		final String location;
		final String ip;
		final String proto;
		final String load;
		final int pings;
		final int noteResourceId;

		switch (currentStatus) {
		case Connected:
			noteResourceId = R.string.connected_note;
			break;
		case Connecting:
			noteResourceId = R.string.connecting_note;
			break;
		default:
			noteResourceId = R.string.disconnected_note;
			break;
		}

		if (currentStatus == VpnStatusLocal.Connected || currentStatus == VpnStatusLocal.Connecting) 
		{
			VpnProfile profile = defaultProfile();
			final boolean isUseUdp = profile.mUseUdp;
			final String port = profile.mServerPort;
			ip = profile.mServerName;
			HashMap<String, Object> serverInfo = IPChecker.getInstance(this).serverForVpnByIp(ip);
			location = (String) serverInfo.get(IPChecker.TAG_LOCATION);
			load = serverInfo.get(IPChecker.TAG_SERVERLOAD).toString();
			pings = ((Integer) serverInfo.get(IPChecker.TAG_PING_TIME)).intValue();
			proto = (isUseUdp ? "UDP":"TCP") + " " + port;
			//update listview and connect/disconnect button on ui
			this.runOnUiThread(new Runnable() {
				public void run() {
					actServers.markActiveLocation(location);
					actPorts.markActiveService(isUseUdp ?"UDP":"TCP", port);
					setConnectableButtonBar(false);
					switchForAction(ActionType.SettingAction);
                    actSettings.setConnectionStatistics(0, 0, 1, mServiceSwitcher.mUseAidlService);
				}
			});
			if (currentStatus == VpnStatusLocal.Connected) {
				//save params
				ConfigManager.isConnected = true;
				ConfigManager.getInstance(this).setPrefBool(ConfigManager.PK_LAST_SUCCESS, true);
				ConfigManager.getInstance(this).setPrefString(ConfigManager.PK_LAST_VPNSERVER, ip);
				ConfigManager.getInstance(this).setPrefString(ConfigManager.PK_LAST_VPNPORT, port);
				ConfigManager.getInstance(this).setPrefString(ConfigManager.PK_LAST_PROTO, isUseUdp ? "UDP":"TCP");
			} else
			{
				ConfigManager.isConnected = false;
			}
		}
        else {
			Log.i("tt", "----------------------enter disconnect");
			ConfigManager.isConnected = false;
			location = "Unknown";
			load = "";
			ip = "";
			proto = "";
			pings = -1;
			this.runOnUiThread(new Runnable() {
				public void run() {
					actServers.clearActiveLocation();
					actPorts.clearActiveService();
					//show connect button and jump to Setting
					setConnectableButtonBar(true);
					switchForAction(ActionType.SettingAction);
					actSettings.setConnectionStatistics(0, 0, 1, mServiceSwitcher.mUseAidlService);
				}
			});
		}

		if (requestState == RequestStatus.WantSafejump) {

			if (currentStatus != VpnStatusLocal.Disconnected) {
				runOnUiThread(new Runnable() {
					public void run() {
						actSettings.setConnectionInfo(location, ip, proto, load, pings);
						actSettings.setConnectionState(noteResourceId);
					}
				});

			}
		} else {
			runOnUiThread(new Runnable() {
				public void run() {
					actSettings.setConnectionInfo(location, ip, proto, load, pings);
					actSettings.setConnectionState(noteResourceId);
				}
			});
		}

		if (/*(*/lastState == VpnStatusLocal.Connected/* || lastState == VpnStatus.Connecting)*/ && currentStatus == VpnStatusLocal.Disconnected) {

			if (requestState != RequestStatus.WantDisconnect && ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_DROP_RECONNECT)) {
				startVpn(defaultProfile());

//			} else if (ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_SWITCHOFF_ROAMING)) {
//				disableDataRoaming();
			} else if (ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_KILL_INTERNET)) {
				killInternet();
			}

		}
		lastState = currentStatus;
	}
	@Override
	public void newLog(final LogItem logItem) {
		Log.d("OpenVPN",logItem.getString(AppActivity.this));
		runOnUiThread(new Runnable() {
			public void run() {
				actLogs.vpnLog(logItem.getString(AppActivity.this));
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		IPChecker.getInstance(this).stopPingTask();
		backgroundMode = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		backgroundMode = false;
		if (ConfigManager.isLoginned && !ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_DISABLE_PING))
			IPChecker.getInstance(this).startPingTask(pingHandler);

//        mServiceSwitcher.bindToService();

		if (getIntent() !=null && OpenVPNService.DISCONNECT_VPN.equals(getIntent().getAction())) {
			mServiceSwitcher.stopVpn(false);
		}
		if (getIntent() != null && SHORTCUT_CONNECT.equals(getIntent().getAction())) {
			if (!ConfigManager.isConnected) {
				if (ConfigManager.isLoginned) {
					if (activeVpnProfile == null) {
						activeVpnProfile = defaultProfile();
						activeVpnProfile.mUsername = ConfigManager.activeUserName;
						activeVpnProfile.mPassword = ConfigManager.activePasswdOfUser;
						if (ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_LAST_SUCCESS)) {
							activeVpnProfile.mServerName = ConfigManager.getInstance(this).prefStringForKey(ConfigManager.PK_LAST_VPNSERVER);
							activeVpnProfile.mServerPort = ConfigManager.getInstance(this).prefStringForKey(ConfigManager.PK_LAST_VPNPORT);
							activeVpnProfile.mUseUdp = ConfigManager.getInstance(this).prefStringForKey(ConfigManager.PK_LAST_PROTO).equals("UDP") ? true:false;
						} else {
							activeVpnProfile.mServerName = (String) IPChecker.getInstance(this).bestServerForVpn().get(IPChecker.TAG_ADDRESS);
							activeVpnProfile.mServerPort = "443";
							activeVpnProfile.mUseUdp = false;
						}
					}
					switchForAction(ActionType.SettingAction);
					requestState = RequestStatus.WantConnect;
					startVpn(activeVpnProfile);
					getIntent().setAction(null);
				}
			}
			getIntent().setAction(null);
		} else if (getIntent() != null && SHORTCUT_DISCONNECT.equals(getIntent().getAction())) {
			mServiceSwitcher.stopVpn(false);
			getIntent().setAction(null);
		}
        if (ConfigManager.isLoginned) {
            registerInternetConnectionListener();
        }
	}


	@Override
	protected void onStop() {
		super.onStop();
//        unregisterReceiver(internetConnectionListener);
        IPChecker.getInstance(this).stopPingTask();
//        mServiceSwitcher.unbindFromService();
//        mServiceSwitcher.removeStateListener();
	}

	@Override
	protected void onDestroy() {
        super.onDestroy();
        removeListeners();
	}

    private void removeListeners() {
        mServiceSwitcher.removeStateListener();
        cancelNotification();
        mServiceSwitcher.stopVpn(true);
        try {
            unregisterReceiver(internetConnectionListener);
        }
        catch(IllegalArgumentException e) {
            // when not registered
        }
        VpnStatus.removeLogListener(this);
    }

    @Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void startVpn(final VpnProfile profile) {
		HashMap<String, Object> serverInfo = IPChecker.getInstance(this).serverForVpnByIp(profile.mServerName);
		final String location = (String) serverInfo.get(IPChecker.TAG_LOCATION);
		final String load = serverInfo.get(IPChecker.TAG_SERVERLOAD).toString();
		final int pings = ((Integer) serverInfo.get(IPChecker.TAG_PING_TIME)).intValue();
		final String proto = (profile.mUseUdp ? "UDP":"TCP") + " " + profile.mServerPort;

		Log.i("start", "start time" + System.currentTimeMillis());
		activeVpnProfile = profile;
		mServiceSwitcher.startVPN(profile);
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                actSettings.setConnectionInfo(location, profile.mServerName, proto, load, pings);
                actSettings.setConnectionState(R.string.ready_note);
                actSettings.refreshSetting();
            }
        });
        Log.i("end", "end time" + System.currentTimeMillis());
	}

	private boolean isVpnRunning() {
		final ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		final List<RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

		for (RunningServiceInfo runningServiceInfo : services) {
			if (runningServiceInfo.service.getClassName().equals(OpenVPNService.class.getName())) {
				return true;
			}
		}
		return false;
	}

    private void moveOptionsToConnection(VpnProfile profile) {
        profile.mConnections = new Connection[1];
        Connection conn = new Connection();

        conn.mServerName = profile.mServerName;
        conn.mServerPort = profile.mServerPort;
        conn.mUseUdp = profile.mUseUdp;
        conn.mCustomConfiguration = "";

        profile.mConnections[0] = conn;

    }

	protected void killInternet() {
		//Disable wifi
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(false);
		//Disable mobile data
		try {
			final ConnectivityManager conman = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final Class conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField.get(conman);
			final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);
			setMobileDataEnabledMethod.invoke(iConnectivityManager, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void disablePing() {
		IPChecker.getInstance(this).stopPingTask();
	}

	public void enablePing() {
		if (ConfigManager.isLoginned && !ConfigManager.getInstance(this).prefBoolForKey(ConfigManager.PK_DISABLE_PING))
			IPChecker.getInstance(this).startPingTask(pingHandler);
	}

	private BroadcastReceiver internetConnectionListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
			if (cm == null)
				return;
			if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()) {
				// Send here
				if (backgroundMode && !ConfigManager.isConnected) {
					if (lastState == VpnStatusLocal.Disconnected && ConfigManager.getInstance(AppActivity.this).prefBoolForKey(ConfigManager.PK_DROP_RECONNECT)) {
						if (!ConfigManager.isConnected) {
							if (activeVpnProfile == null) {
								activeVpnProfile = defaultProfile();
								activeVpnProfile.mUsername = ConfigManager.activeUserName;
								activeVpnProfile.mPassword = ConfigManager.activePasswdOfUser;
								if (ConfigManager.getInstance(AppActivity.this).prefBoolForKey(ConfigManager.PK_LAST_SUCCESS)) {
									activeVpnProfile.mServerName = ConfigManager.getInstance(AppActivity.this).prefStringForKey(ConfigManager.PK_LAST_VPNSERVER);
									activeVpnProfile.mServerPort = ConfigManager.getInstance(AppActivity.this).prefStringForKey(ConfigManager.PK_LAST_VPNPORT);
									activeVpnProfile.mUseUdp = ConfigManager.getInstance(AppActivity.this).prefStringForKey(ConfigManager.PK_LAST_PROTO).equals("UDP") ? true:false;
								} else {
									return;
								}
							}
							switchForAction(ActionType.SettingAction);
							requestState = RequestStatus.WantConnect;
							startVpn(activeVpnProfile);
						}
					}
				}
			} else {
			}

		}
	};

    public boolean isExternalOpenVPN() {
        return mServiceSwitcher.mUseAidlService;
    }

    enum ServiceSwitcherStatus {
        NOT_CONNECTED_TO_SERVICE,
        LAUNCHING,
        WORKING
    }

    class ServiceSwitcher {

        private static final int START_PROFILE_EMBEDDED = 2;
        private static final int ICS_OPENVPN_PERMISSION = 7;
        private static final int START_VPN_CMD = 102;

        private boolean mUseAidlService;
        private Handler mHandler;
        private IOpenVPNAPIService mAidlService = null;
        private VpnProfile mActiveVpnProfile = null;

        private ServiceSwitcherStatus state = ServiceSwitcherStatus.NOT_CONNECTED_TO_SERVICE;

        ServiceSwitcher() {
        }

        private void bindToService() {
            if(mUseAidlService) {
                Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
                icsopenvpnService.setPackage("de.blinkt.openvpn");
                bindService(icsopenvpnService, mAidlConnection, Context.BIND_AUTO_CREATE);
            }
            else {
                Intent intent = new Intent(AppActivity.this, OpenVPNService.class);
                intent.setAction(OpenVPNService.START_SERVICE);
                bindService(intent, vpnServiceConn, Context.BIND_AUTO_CREATE);
            }
        }

        private void prepareStartProfile(int requestCode) throws RemoteException {
            Intent requestpermission = mAidlService.prepareVPNService();
            if(requestpermission == null) {
                onActivityResult(requestCode, Activity.RESULT_OK, null);
            } else {
                // Have to call an external Activity since services cannot used onActivityResult
                startActivityForResult(requestpermission, requestCode);
            }
        }

        /**
         * Class for interacting with the main interface of the service.
         */
        private ServiceConnection mAidlConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  We are communicating with our
                // service through an IDL interface, so get a client-side
                // representation of that from the raw service object.

                mAidlService = IOpenVPNAPIService.Stub.asInterface(service);
                onServiceBind();

//                try {
//                    // Request permission to use the API
//                    Intent i = mAidlService.prepare(getPackageName());
//                    if (i!=null) {
//                        startActivityForResult(i, ICS_OPENVPN_PERMISSION);
//                    } else {
//                        onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
//                    }
//
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                mAidlService = null;
            }
        };

        private OpenVPNService vpnService;
        private ServiceConnection vpnServiceConn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                LocalBinder binder = (LocalBinder) service;
                vpnService = binder.getService();
                onServiceBind();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                vpnService = null;
            }

        };

        private void onServiceBind() {
//            startStateListener();
            if(state == ServiceSwitcherStatus.LAUNCHING) {
                startVPN1();
            }
            if(!mUseAidlService) {
				vpnService.setConfigurationIntent(getPendingIntent());
				vpnService.setNotificationIntent(getPendingIntent());
            }

        }

		PendingIntent getPendingIntent() {
			Intent intent;
			// Let the configure Button show the Log
			intent = new Intent(AppActivity.this.getBaseContext(), AppActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			PendingIntent startLW = PendingIntent.getActivity(AppActivity.this, 0, intent, 0);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

			return startLW;
		}

		private void launchVPN () {

            if (mActiveVpnProfile == null)
                return;
            moveOptionsToConnection(mActiveVpnProfile);
            int vpnok = mActiveVpnProfile.checkProfile(AppActivity.this);
            if(vpnok!= R.string.no_error_found) {
                Log.e(TAG, "checkProfile failed: "+getString(vpnok));
                return;
            }

            Intent intent = VpnService.prepare(AppActivity.this);

            // Check if we want to fix /dev/tun
            boolean usecm9fix = false;
            boolean loadTunModule = false;

            if(loadTunModule)
                execeuteSUcmd("insmod /system/lib/modules/tun.ko");

            if(usecm9fix && !mCmfixed ) {
                execeuteSUcmd("chown system /dev/tun");
            }

            if (intent != null) {
                VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                        ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                //start query
                try {
                    startActivityForResult(intent, START_VPN_CMD);
                } catch (ActivityNotFoundException ane) {
                    VpnStatus.logError(R.string.no_vpn_support_image);
                }
            } else {
                onActivityResult(START_VPN_CMD, Activity.RESULT_OK, null);
            }
        }

        private void execeuteSUcmd(String command) {
            ProcessBuilder pb = new ProcessBuilder("su","-c",command);
            try {
                Process p = pb.start();
                int ret = p.waitFor();
                if(ret==0)
                    mCmfixed=true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stopVpn(boolean onDestroy) {
            if(state == ServiceSwitcherStatus.NOT_CONNECTED_TO_SERVICE) {
                return;
            }
            state = ServiceSwitcherStatus.NOT_CONNECTED_TO_SERVICE;
//            if (mActiveVpnProfile == null)
//                return;
            if(mUseAidlService) {
                try {
                    // do not stop external VPN in onDestroy()
                    if(mAidlService != null && !onDestroy)
                        mAidlService.disconnect();
                } catch (RemoteException e) {
                    Log.e(TAG, "stopVpn()", e);
                }
            }
            else {
                if (vpnService != null && vpnService.getManagement() != null)
                    vpnService.getManagement().stopVPN();
                stopService(new Intent(AppActivity.this, OpenVPNService.class));
                cancelNotification();
            }
            mServiceSwitcher.unbindFromService();
        }

        public void unbindFromService() {
            if(mUseAidlService) {
                unbindService(mAidlConnection);
            }
            else {
                unbindService(vpnServiceConn);
            }
        }

        private IOpenVPNStatusCallback.Stub statusCallbackAidl = new IOpenVPNStatusCallback.Stub() {
            /**
             * This is called by the remote service regularly to tell us about
             * new values.  Note that IPC calls are dispatched through a thread
             * pool running in each process, so the code executing here will
             * NOT be running in our main thread like most other things -- so,
             * to update the UI, we need to use a Handler to hop over there.
             */
            @Override
            public void newStatus(String uuid, String state, String message, String level) throws RemoteException {
                AppActivity.this.updateState(state, message, 0, parseConnectionStatus(level));
            }
        };

        public void startStateListener() {
            if(mUseAidlService) {
                try {
                    mAidlService.registerStatusCallback(statusCallbackAidl);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            else {
                VpnStatus.addStateListener(AppActivity.this);
            }
        }

        public void removeStateListener() {
            if(mUseAidlService) {
                try {
                    mAidlService.unregisterStatusCallback(statusCallbackAidl);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            else {
                VpnStatus.removeStateListener(AppActivity.this);
            }
        }

        private void startVPN1() {
            if(mUseAidlService) {
                try {
                    // Request permission to use the API
                    Intent i = mAidlService.prepare(getPackageName());
                    if (i!=null) {
                        startActivityForResult(i, ICS_OPENVPN_PERMISSION);
                    } else {
                        onActivityResult(ICS_OPENVPN_PERMISSION, Activity.RESULT_OK,null);
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
//                try {
//                    prepareStartProfile(START_PROFILE_EMBEDDED);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
            }
            else {
                launchVPN();
            }
        }

        public void startVPN(VpnProfile profile) {
            mActiveVpnProfile = profile;
            int externalVpn = ConfigManager.getInstance(AppActivity.this).prefIntForKey(ConfigManager.PK_ICS_OPENVPN);
            boolean isIcsOpenVpnPresent;
            boolean isIcsOpenVpnPresentNewer = false;
            try {
                PackageInfo pinfo = getPackageManager().getPackageInfo("de.blinkt.openvpn", 0);
                if(pinfo != null) {
                    isIcsOpenVpnPresentNewer = ExternalOpenVPNService.VERSION_CODE < pinfo.versionCode;
                }
                isIcsOpenVpnPresent = true;
            } catch (PackageManager.NameNotFoundException e) {
                Log.i(TAG,"ics-openvpn not found");
                isIcsOpenVpnPresent = false;
            }
            switch (externalVpn) {
                case ConfigManager.ICS_OPENVPN_EXTERNAL:
                    mUseAidlService = isIcsOpenVpnPresent;
                    break;
                case ConfigManager.ICS_OPENVPN_BUILTIN:
                    mUseAidlService = false;
                    break;
                case ConfigManager.ICS_OPENVPN_AUTO:
                    mUseAidlService = isIcsOpenVpnPresentNewer;
                    break;
            }
            state = ServiceSwitcherStatus.LAUNCHING;
            bindToService();
        }

        public void addCurrentProfile() {
            if(!mUseAidlService) {
                Log.e(TAG,"addCurrentProfile() cannot be called for builtin OpenVPN");
                return;
            }
            moveOptionsToConnection(mActiveVpnProfile);
            int vpnok = mActiveVpnProfile.checkProfile(AppActivity.this);
            if(vpnok!= R.string.no_error_found) {
                Log.e(TAG, "checkProfile failed: "+getString(vpnok));
                return;
            }
            final String profileStr = mActiveVpnProfile.getConfigFile(AppActivity.this, false, true);

            HashMap<String, Object> serverInfo = IPChecker.getInstance(AppActivity.this).serverForVpnByIp(mActiveVpnProfile.mServerName);
            final String location = (String) serverInfo.get(IPChecker.TAG_LOCATION);
            final String proto = (mActiveVpnProfile.mUseUdp ? "UDP":"TCP") + " " + mActiveVpnProfile.mServerPort;
            String profileName = location + ": " + proto;

            LayoutInflater li = LayoutInflater.from(AppActivity.this);
            View promptsView = li.inflate(R.layout.add_profile_dialog, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AppActivity.this);
            alertDialogBuilder.setView(promptsView);

            final EditText userInput = (EditText) promptsView.findViewById(R.id.editProfileName);
            userInput.setText(profileName);

            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try {
                                        boolean ok = mAidlService.addVPNProfile(userInput.getText().toString(), profileStr);
                                        Toast.makeText(AppActivity.this, ok ? "Profile added to OpenVPN" : "Can't export profile",Toast.LENGTH_LONG).show();
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }

        private class startOpenVpnThread extends Thread {

            @Override
            public void run() {
                VPNLaunchHelper.startOpenVpn(mActiveVpnProfile, getBaseContext());
            }
        }

        /** from VPN permission dialog */
        void onActivityResult (int requestCode, int resultCode, Intent data) {

            if(!mUseAidlService) {
                if (requestCode == START_VPN_CMD) {
                    if (resultCode == Activity.RESULT_OK) {
                        int needpw = mActiveVpnProfile.needUserPWInput(false);
                        if (needpw != 0) {
                            VpnStatus.updateStateString("USER_VPN_PASSWORD", "", R.string.state_user_vpn_password,
                                    ConnectionStatus.LEVEL_AUTH_FAILED);
                        } else {
                            new startOpenVpnThread().start();
                            startStateListener();
                        }
                    }
                    else if (resultCode == Activity.RESULT_CANCELED) {
                        // User does not want us to start, so we just vanish
                        VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled,
                                ConnectionStatus.LEVEL_NOTCONNECTED);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                actSettings.setConnectionState(R.string.disconnected_note);
                            }
                        });

                    }
                }
            }
            else {
                if (requestCode == ICS_OPENVPN_PERMISSION && resultCode == Activity.RESULT_OK) {
                    startEmbeddedProfile(mActiveVpnProfile);
                    startStateListener();
                }
            }
        }

        public void cancelNotification() {
            // remove status bar notifications for local service only
            if(!mUseAidlService && vpnService!=null) {
				vpnService.setConfigurationIntent(null);
				vpnService.setNotificationIntent(null);
                vpnService.cancelNotification();
            }
        }

        private void startEmbeddedProfile(VpnProfile profile) {
            try {
                moveOptionsToConnection(profile);
                int vpnok = profile.checkProfile(AppActivity.this);
                if(vpnok!= R.string.no_error_found) {
                    Log.e(TAG, "checkProfile failed: "+getString(vpnok));
                    return;
                }
                String ss = profile.getConfigFile(AppActivity.this, false, true);
                mAidlService.startVPN(ss);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        private ConnectionStatus parseConnectionStatus(String statusName) {
            for(ConnectionStatus status : ConnectionStatus.values()) {
                if(status.name().equals(statusName)) {
                    Log.d(TAG,"parseConnectionStatus: "+statusName+"="+status.toString());
                    return status;
                }
            }
            Log.d(TAG,"parseConnectionStatus: "+statusName+"="+ConnectionStatus.UNKNOWN_LEVEL.toString());
            return ConnectionStatus.UNKNOWN_LEVEL;
        }

    }

}
