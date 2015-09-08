/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper.openvpn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import de.blinkt.openvpn.api.ExternalOpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class ConfigManager {

	public static final String DEFAULT_VPN_PROFILE="Proxys.sh VPN";
	public static final String PREF_NAME = "safejumper_pref";
	public static final String PREF_USERLIST = "_safejumper_users";
	private static final String CREDENTIAL_CIPHER_KEY = "safejumper,proxy.sh";

	private static final String PK_AUTO_LAUNCH = "autoLaunch";
	public static final String PK_AUTO_CONNECT = "autoConnect";
	public static final String PK_DROP_RECONNECT = "reconnectDrop";
	public static final String PK_SWITCHOFF_ROAMING = "switchOffRoaming";
	public static final String PK_DISABLE_PING = "disablePing";
	public static final String PK_ALL_DISPLAY = "allDisplay";
	public static final String PK_KILL_INTERNET = "killInternet";
	public static final String PK_SAVE_CREDENTIALS = "saveCredential";
	public static final String PK_LAST_USER = "lastUserName";
	public static final String PK_LAST_VPNSERVER = "lastVpnServer";
	public static final String PK_LAST_VPNPORT = "lastVpnPort";
	public static final String PK_LAST_PROTO = "lastProtocol";
	public static final String PK_LAST_SUCCESS = "lastSuccess";
    public static final String PK_ICS_OPENVPN = "ics-openvpn";

    // values for PK_ICS_OPENVPN
    public static final int ICS_OPENVPN_AUTO = 0;
    public static final int ICS_OPENVPN_BUILTIN = 1;
    public static final int ICS_OPENVPN_EXTERNAL = 2;

	public static Boolean isLoginned = false;
	public static Boolean isConnected = false;
	public static String activeUserName = null;
	public static String activePasswdOfUser = null;

	private Context context;
	private SharedPreferences pref;
	
	private static ConfigManager instance = null;
	
	private static void checkInstance(Context c) {
		if (instance == null)
			instance = new ConfigManager(c);
	}
	
	private ConfigManager(Context c) {
		this.context = c;
		this.pref = c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
	}
	
	synchronized public static ConfigManager getInstance(Context c) {
		checkInstance(c);
		return instance;
	}

	public boolean prefBoolForKey(String key) {
		return pref.getBoolean(key, false);
	}

	public String prefStringForKey(String key) {
		return pref.getString(key, null);
	}

	public int prefIntForKey(String key) {
		return pref.getInt(key, 0);
	}

	public void setPrefBool(String key, boolean value) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	public void setPrefString(String key, String value) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(key, value);
		editor.commit();
	}

    public void setPrefInt(String key, int value) {
        pref.edit().putInt(key, value).commit();
    }

    public void setUserObject(String username, String passwd) {

		SharedPreferences users = context.getSharedPreferences(PREF_USERLIST, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = users.edit();
		String cipher = encrypt(passwd);
		editor.putString(username, (cipher == null) ? "":cipher);
		editor.commit();
	}

	public String getUserObject(String usernmae) {

		SharedPreferences users = context.getSharedPreferences(PREF_USERLIST, Context.MODE_PRIVATE);
		String cipher = users.getString(usernmae, null);
		return cipher != null ? decrypt(cipher) : null;
	}

	public static String encrypt(String toEncrypt) {

		try {
			byte [] keys = Arrays.copyOf(CREDENTIAL_CIPHER_KEY.getBytes(), 16);
			//Create your Secret Key Spec, which defines the key transformations
			SecretKeySpec skeySpec = new SecretKeySpec(keys, "AES");


			//Get the cipher
			Cipher cipher = Cipher.getInstance("AES");

			//Initialize the cipher
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

			//Encrypt the string into bytes
			byte[ ] encryptedBytes = cipher.doFinal(toEncrypt.getBytes());

			//Convert the encrypted bytes back into a string
			String encrypted = Base64.encodeToString(encryptedBytes, 0);

			return encrypted;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String decrypt(String encryptedText) {
		try {
			byte [] keys = Arrays.copyOf(CREDENTIAL_CIPHER_KEY.getBytes(), 16);

			SecretKeySpec skeySpec = new SecretKeySpec(keys, "AES");

			Cipher cipher = Cipher.getInstance("AES");

			cipher.init(Cipher.DECRYPT_MODE, skeySpec);

			byte[] toDecrypt = Base64.decode(encryptedText, 0);

			byte[] encrypted = cipher.doFinal(toDecrypt);

			return new String(encrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean writeCaFile(Context c) {

		File caout = new File(c.getCacheDir(), "proxysh.crt");
		if (caout.exists())
			return true;

		try {
			InputStream cain = c.getAssets().open("proxysh.crt");
			FileOutputStream fout = new FileOutputStream(caout);

			byte buf[]= new byte[512];

			int lenread = cain.read(buf);
			while(lenread> 0) {
				fout.write(buf, 0, lenread);
				lenread = cain.read(buf);
			}
			fout.close();
			return true;

		} catch (IOException e) {
			VpnStatus.logInfo("Failed getting ca certificate");
		}
		return true;
	}
	
	public static boolean autoLaunchOnBoot(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		return prefs.getBoolean(PK_AUTO_LAUNCH, false);
	}
	
	public static void setAutoLaunchOnBoot(Context c, boolean allow) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		Editor prefsedit = prefs.edit();
		prefsedit.putBoolean(PK_AUTO_LAUNCH, allow);
		prefsedit.apply();
	}

    /**
     * Returns true if external OpenVPN is newer
     * @return
     */
    public boolean checkExternalOpenVpnVersion() {
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo("de.blinkt.openvpn", 0);
            if(pinfo != null) {
                return ExternalOpenVPNService.VERSION_CODE < pinfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("ConfigManager","ics-openvpn not found");
        }
        return false;
    }

    public boolean ifExternalOpenVpnInstalled() {
        try {
            context.getPackageManager().getPackageInfo("de.blinkt.openvpn", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("ConfigManager","ics-openvpn not found");
        }
        return false;
    }

    /**
	 * In ics-openvpn it's Build.FAVOR = "ovpn3"
	 * @return always false in current implementation
	 */
	public static boolean isOvpn3Enabled() {
		// TODO: read from settings
		return false;
	}
}
