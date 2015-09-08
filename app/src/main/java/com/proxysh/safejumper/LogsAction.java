/*
 * Copyleft (/c) MMXV, Proxy.sh
 * Distributed under the GNU GPL v2
 */
package com.proxysh.safejumper;

import java.util.Date;

import android.content.ClipData;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.proxy.sh.safejumper.R;

public class LogsAction implements OnClickListener {

	private Button btnVpnLog, btnAppLog;
	private TextView textVpnLog, textSysLog, textAppLog;
	private ScrollView scrollViewVpnLog, scrollViewSysLog, scrollViewAppLog;

	private FrameLayout frameLogView;
	private AppActivity owner;
	private View current;
	private TextView activeView;

	public LogsAction(View v, AppActivity owner) {

		this.owner = owner;
		btnAppLog = (Button) v.findViewById(R.id.btnAppLog);
		btnVpnLog = (Button) v.findViewById(R.id.btnVpnLog);
		textAppLog = (TextView) v.findViewById(R.id.textAppLog);
		textVpnLog = (TextView) v.findViewById(R.id.textVpnLog);
		textSysLog = (TextView) v.findViewById(R.id.textSysLog);
		scrollViewAppLog = (ScrollView) v.findViewById(R.id.scrollViewAppLog);
		scrollViewVpnLog = (ScrollView) v.findViewById(R.id.scrollViewVpnLog);
		scrollViewSysLog = (ScrollView) v.findViewById(R.id.scrollViewSysLog);
		frameLogView = (FrameLayout) v.findViewById(R.id.frameLogView);

		btnAppLog.setOnClickListener(this);
		btnVpnLog.setOnClickListener(this);

		btnAppLog.setSelected(true);
		frameLogView.removeAllViews();
		frameLogView.addView(scrollViewAppLog);
		current = scrollViewAppLog;
		
		textAppLog.setOnTouchListener(new DoubleTapListener(owner));
		textSysLog.setOnTouchListener(new DoubleTapListener(owner));
		textVpnLog.setOnTouchListener(new DoubleTapListener(owner));
	}

	private void switchView(View v) {
		if (current != v) {
			frameLogView.addView(v);
			frameLogView.removeView(current);
			current = v;
		}
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnAppLog:
			btnAppLog.setSelected(true);
			btnVpnLog.setSelected(false);
			switchView(scrollViewAppLog);
			break;
		case R.id.btnVpnLog:
			btnAppLog.setSelected(false);
			btnVpnLog.setSelected(true);
			switchView(scrollViewVpnLog);
			break;
		default:
			break;
		}
	}

	public void appLog(String s) {
		textAppLog.append(currentTime() + ":" + s + "\n");
	}

	public void vpnLog(String s) {
		textVpnLog.append(s + "\n");

	}
	private String currentTime() {
		return DateFormat.getDateFormat(owner.getApplicationContext()).format(new Date());
	}
}

class DoubleTapListener implements OnTouchListener {

	/* variable for counting two successive up-down events */
	int clickCount = 0;
	/*variable for storing the time of first click*/
	long startTime;
	/* variable for calculating the total time*/
	long duration;
	/* constant for defining the time duration between the click that can be considered as double-tap */
	static final int MAX_DURATION = 500;
	
	private Context c;
	
	public DoubleTapListener(Context c) {
		this.c = c;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch(event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			clickCount++;
	        if (clickCount == 1){
	            startTime = System.currentTimeMillis();
	        
	        } else if(clickCount == 2) {
	            long duration =  System.currentTimeMillis() - startTime;
	            if (duration <= MAX_DURATION) {                    
	                if (v instanceof TextView) {
	                	copyToClipboard(((TextView)v).getText().toString());
	                }
	                clickCount = 0;
	                duration = 0;
	            }else{
	                clickCount = 1;
	                startTime = System.currentTimeMillis();
	            }
	        }
	        break; 
		}
		return true;    
	}
	
	private void copyToClipboard(String s) {

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
			android.content.ClipboardManager clipboard =  (android.content.ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE); 
			ClipData clip = ClipData.newPlainText("safejumper-log", s);
			clipboard.setPrimaryClip(clip); 
		} else{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE); 
			clipboard.setText(s);
		}
		Toast.makeText(c, c.getString(R.string.text_copied_to_clip), Toast.LENGTH_SHORT).show();
	}
}
