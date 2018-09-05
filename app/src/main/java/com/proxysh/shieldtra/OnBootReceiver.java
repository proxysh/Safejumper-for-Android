/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.shieldtra;

import com.proxysh.shieldtra.openvpn.ConfigManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class OnBootReceiver extends BroadcastReceiver {

	// Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
	@Override
	public void onReceive(Context context, Intent intent) {

		final String action = intent.getAction();
		Log.i("haha", "-------");
		if(Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			lauchShieldtra(context);
		}
	}

	void lauchShieldtra(Context context) {
		
		if (!ConfigManager.autoLaunchOnBoot(context)) 
			return;
		
		Intent startVpnIntent = new Intent(Intent.ACTION_MAIN);
		startVpnIntent.setClass(context, AppActivity.class);
		startVpnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(startVpnIntent);
	}
}
