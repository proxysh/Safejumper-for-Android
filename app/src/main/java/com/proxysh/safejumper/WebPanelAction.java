/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper;

import com.proxy.sh.safejumper.R;
import android.annotation.SuppressLint;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@SuppressLint("SetJavaScriptEnabled")
public class WebPanelAction {

		private static String SURL_PANEL = "https://proxy.sh/panel/clientarea.php";
//	private static String SURL_PANEL = "https://proxy.sh/paneldev/clientarea.php";
	private WebView webPanel;
	private AppActivity owner;

	public WebPanelAction(View v, AppActivity owner) {

		this.owner = owner;
		webPanel = (WebView) v.findViewById(R.id.webPanel);
		webPanel.getSettings().setJavaScriptEnabled(true);
		webPanel.setWebViewClient(new WebViewClient(){
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView v, String url) {
				v.loadUrl(url);
				return true;
			}
		});
		
		loadPanel();
	}

	public void loadPanel() {
		webPanel.loadUrl(SURL_PANEL);
	}
}
