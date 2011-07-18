package org.za.hem.ipsec_tools;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.util.Log;

public class Peer {
	public static final int STATUS_DISCONNECTED = 0;
	public static final int STATUS_CONNECTED = 1;
	public static final int STATUS_PROGRESS = 2;
	public static final int STATUS_DISABLED = 3;
	
	private PeerID mID;
	private StatePreference mPref;
	private SharedPreferences mShared;
	private int mStatus;

	public Peer(Context context, PeerID id, StatePreference pref) {
		mID = id;
		mPref = pref;
		mShared = context.getSharedPreferences(
					PeerPreferences.getSharedPreferencesName(context, id),
    				Activity.MODE_PRIVATE);
		mStatus = STATUS_DISCONNECTED;
	}
	
	public void clear() {
		Editor editor = mShared.edit();
		editor.clear();
		editor.commit();
	}

	public PeerID getPeerID() {
		return mID;
	}
	
	public boolean isEnabled() {
		return mShared.getBoolean(PeerPreferences.ENABLED_PREFERENCE, true);
	}
	
	public Preference getPreference() {
		return mPref;
	}
	
	public void setPreference(StatePreference pref) {
		mPref = pref;
	}
	
	public String getName() {
		return mShared.getString(PeerPreferences.NAME_PREFERENCE, "");
	}
	
	public InetAddress getRemoteAddr() {
		String host = mShared.getString(PeerPreferences.REMOTE_ADDR_PREFERENCE, null);
		String ip = mShared.getString(PeerPreferences.REMOTE_ADDR_IP_PREFERENCE, null);
		try {
			InetAddress ipAddr = InetAddress.getByName(host);
			InetAddress addr = InetAddress.getByAddress(host, ipAddr.getAddress());
			Log.i("ipsec-tools", "getRemoteAddr " + addr);
			return addr;
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	public InetAddress getLocalAddr() {
		// FIXME remote addr may be null
		return Utils.getLocalAddress(getRemoteAddr());
	}
	
	public File getTemplateFile() {
		String addr = mShared.getString(PeerPreferences.TEMPLATE_PREFERENCE, null);
		Log.i("ipsec-tools", "getRemoteAddr " + addr);
		if (addr == null)
			return null;
		return new File(addr);
	}
	
	public int getStatus() {
		return mStatus;
	}

	public void setStatus(int status) {
		mStatus = status;
		mPref.setIconLevel(mStatus);
	}

	public void onPhase1Up() {
		setStatus(STATUS_CONNECTED);
	}

	public void onPhase1Down() {
		setStatus(STATUS_DISCONNECTED);
	}
	
	public String toString() {
		return "Peer[" + getName() + " " + getRemoteAddr() + "]";
	}
}
