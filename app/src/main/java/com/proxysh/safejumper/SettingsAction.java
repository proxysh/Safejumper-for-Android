/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.proxysh.safejumper.openvpn.ConfigManager;
import com.proxysh.safejumper.service.IPChecker;

import com.proxy.sh.safejumper.R;

import de.blinkt.openvpn.api.ConfirmDialog;

public class SettingsAction implements OnCheckedChangeListener, OnClickListener, SignInCallbackInterface {

    private final String[] _RATE_UNITS = {
            "B/s",
            "KB/s",
            "MB/s",
            "GB/s",
            "TB/s",
    };

    private AppActivity owner;
    private CheckBox checkAutoConnect,
            checkAutoLaunch,
            checkAutoReconnect,
            checkSwithoffRoaming,
            checkDisablePing,
            checkAllDisplay,
            checkKillInternet;
    private Button buttonLogout, buttonExport;
    private TextView textCurrentuser;
    private TextView textCurrentLocation;
    private TextView textLoad, textIp, textPingTime, textPort;
    private TextView textInBytes, textOutBytes;
    private TextView textState;

    private ProgressDialog dialog = null;


    public SettingsAction(View v, AppActivity owner) {

        this.owner = owner;

        checkAutoConnect = (CheckBox) v.findViewById(R.id.checkAutoConnect);
        checkAutoLaunch = (CheckBox) v.findViewById(R.id.checkAutoLaunch);
        checkAutoReconnect = (CheckBox) v.findViewById(R.id.checkAutoReconnect);
        checkSwithoffRoaming = (CheckBox) v.findViewById(R.id.checkSwithoffRoaming);
        checkDisablePing = (CheckBox) v.findViewById(R.id.checkDisablePing);
        checkAllDisplay = (CheckBox) v.findViewById(R.id.checkAllDisplay);
        checkKillInternet = (CheckBox) v.findViewById(R.id.checkKillInternet);

        buttonLogout = (Button) v.findViewById(R.id.buttonLogout);
        buttonExport = (Button) v.findViewById(R.id.buttonExport);

        textCurrentuser = (TextView) v.findViewById(R.id.textCurrentuser);
        textCurrentLocation = (TextView) v.findViewById(R.id.textCurrentLocation);
        textLoad = (TextView) v.findViewById(R.id.textLoad);
        textIp = (TextView) v.findViewById(R.id.textIp);
        textPingTime = (TextView) v.findViewById(R.id.textPingTime);
        textPort = (TextView) v.findViewById(R.id.textPort);
        textInBytes = (TextView) v.findViewById(R.id.textInBytes);
        textOutBytes = (TextView) v.findViewById(R.id.textOutBytes);
        textState = (TextView) v.findViewById(R.id.textState);

        //load from preference
        checkAutoConnect.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_AUTO_CONNECT));
        checkAutoLaunch.setChecked(ConfigManager.autoLaunchOnBoot(owner));
        checkAutoReconnect.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_DROP_RECONNECT));
        checkSwithoffRoaming.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_SWITCHOFF_ROAMING));
        checkDisablePing.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_DISABLE_PING));
        checkAllDisplay.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_ALL_DISPLAY));
        checkKillInternet.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_KILL_INTERNET));

        //event listener register
        checkAutoConnect.setOnCheckedChangeListener(this);
        checkAutoLaunch.setOnCheckedChangeListener(this);
        checkAutoReconnect.setOnCheckedChangeListener(this);
        checkSwithoffRoaming.setOnCheckedChangeListener(this);
        checkDisablePing.setOnCheckedChangeListener(this);
        checkAllDisplay.setOnCheckedChangeListener(this);
        checkKillInternet.setOnCheckedChangeListener(this);
        buttonLogout.setOnClickListener(this);
        buttonExport.setOnClickListener(this);

    }

    public void refreshSetting() {
        checkDisablePing.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_DISABLE_PING));
        checkAutoLaunch.setChecked(ConfigManager.autoLaunchOnBoot(owner));
        checkAllDisplay.setChecked(ConfigManager.getInstance(owner).prefBoolForKey(ConfigManager.PK_ALL_DISPLAY));

        buttonExport.setVisibility(owner.isExternalOpenVPN() ? View.VISIBLE : View.GONE);
    }

    public void setConnectionInfo(String loc, String ip, String proto, String load, int pings) {

        textCurrentLocation.setText(loc);
        textLoad.setText(load.isEmpty() ? "N/A" : load + " %");
        textIp.setText(ip);
        textPort.setText(proto);
        textPingTime.setText(pings == -1 ? "TBD" : (pings + "ms"));
    }

    public void setConnectionState(int noteResourceId) {

        if (noteResourceId == R.string.connected_note) {
            textState.setTextColor(owner.getResources().getColor(R.color.ConnectedTextColor));
            textState.setText(owner.getString(R.string.connected_note));
        } else if (noteResourceId == R.string.connecting_note) {
            textState.setTextColor(owner.getResources().getColor(R.color.ConnectingTextColor));
            textState.setText(owner.getString(R.string.connecting_note));
        } else if (noteResourceId == R.string.disconnected_note) {
            textState.setTextColor(owner.getResources().getColor(R.color.DisconnectedTextColor));
            textState.setText(owner.getString(R.string.disconnected_note));
        } else {
            textState.setTextColor(owner.getResources().getColor(R.color.ConnectingTextColor));
            textState.setText(owner.getString(R.string.connecting_note));
        }
    }

    public void setConnectionStatistics(long inbytes, long outbytes, int timeInterval, boolean notAvailable) {
        if (notAvailable) {
            textInBytes.setText("N/A");
            textOutBytes.setText("N/A");
        } else {
            if (inbytes < 0) inbytes = 0;
            if (outbytes < 0) outbytes = 0;
            textInBytes.setText(formatBytesCount(inbytes / timeInterval, _RATE_UNITS));
            textOutBytes.setText(formatBytesCount(outbytes / timeInterval, _RATE_UNITS));
        }
    }

    public void setUserName(String name) {
        textCurrentuser.setText(name);
    }

    private String formatBytesCount(long bytes, String[] units) {

        String formatted;
        String u;
        int i = 0;
        double v = (double) bytes;
        double n = (double) bytes;
        while (n > 999.0) {
            i++;
            double divisor = Math.pow(10.0, ((double) (i * 3)));
            n = v / divisor;
        }
        if (i >= units.length) {
            u = "***";
        } else {
            u = units[i];
        }
        if (i == 0) {
            formatted = String.valueOf(bytes);
        } else {
            int n100 = (int) Math.round(n * 100.0);
            int displayInt = n100 / 100;
            int displayFract = n100 - (displayInt * 100);
            if (n100 < 1000) {
                formatted = String.format("%d.%02d", displayInt, displayFract);
            } else if (n100 < 10000) {
                formatted = String.format("%d.%01d", displayInt, (displayFract + 5) / 10);
            } else if (n100 < 100000) {
                if (displayFract > 49 && displayInt < 999) {
                    displayInt++;
                }
                formatted = String.valueOf(displayInt);
            } else {
                formatted = "***";
            }
        }

        return formatted + " " + u;

    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean checked) {
        String tag = null;
        switch (v.getId()) {
            case R.id.checkAutoConnect:
                tag = ConfigManager.PK_AUTO_CONNECT;
                break;
            case R.id.checkAutoLaunch:
                ConfigManager.setAutoLaunchOnBoot(owner, checked);
                return;
            case R.id.checkAutoReconnect:
                tag = ConfigManager.PK_DROP_RECONNECT;
                break;
            case R.id.checkSwithoffRoaming:
                tag = ConfigManager.PK_SWITCHOFF_ROAMING;
                break;
            case R.id.checkDisablePing:
                tag = ConfigManager.PK_DISABLE_PING;
                if (checked == true) {
                    owner.disablePing();
                } else {
                    owner.enablePing();
                }
                break;
            case R.id.checkKillInternet:
                tag = ConfigManager.PK_KILL_INTERNET;
                break;
            case R.id.checkAllDisplay:
                tag = ConfigManager.PK_ALL_DISPLAY;
                buttonLogout.setEnabled(false);
                checkAutoConnect.setEnabled(false);
                checkAutoLaunch.setEnabled(false);
                checkAutoReconnect.setEnabled(false);
                checkSwithoffRoaming.setEnabled(false);
                checkDisablePing.setEnabled(false);
                checkAllDisplay.setEnabled(false);

                if (dialog == null) {
                    dialog = ProgressDialog.show(owner, "",
                            owner.getString(R.string.waiting_assist), true);
                } else {
                    if (!dialog.isShowing()) {
                        dialog = ProgressDialog.show(owner, "",
                                owner.getString(R.string.waiting_assist), true);
                        IPChecker.getInstance(null).registerTo(ConfigManager.activeUserName,
                                ConfigManager.activePasswdOfUser, checked, this);
                    }
                }

                break;
            default:
                break;
        }
        if (tag != null) {
            ConfigManager.getInstance(owner).setPrefBool(tag, checked);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonLogout) {
            //disconnect
            owner.onSignOut();
        } else if (v.getId() == R.id.buttonExport) {
            owner.onExportProfile();
        }
    }

    @Override
    public void signInFinished(boolean signInStatus) {
        dialog.dismiss();
        buttonLogout.setEnabled(true);
        checkAutoConnect.setEnabled(true);
        checkAutoLaunch.setEnabled(true);
        checkAutoReconnect.setEnabled(true);
        checkSwithoffRoaming.setEnabled(true);
        checkDisablePing.setEnabled(true);
        checkAllDisplay.setEnabled(true);
        if (signInStatus) {
            owner.onUpdateLocation();
        }
    }
}
