package org.za.hem.ipsec_tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.util.Log;

public class Peer {
	public static final int STATUS_DISCONNECTED = 0;
	public static final int STATUS_CONNECTED = 1;
	
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
		// TODO fetch status from racoon
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

	public Preference getPreference() {
		return mPref;
	}
	
	public String getName() {
		return mShared.getString(PeerPreferences.NAME_PREFERENCE, "");
	}
	
	public String getRemoteAddr() {
		String addr = mShared.getString(PeerPreferences.REMOTE_ADDR_PREFERENCE, null);
		Log.i("ipsec-tools", "getRemoteAddr " + addr);
		return addr;
	}
	
	public int getStatus() {
		return mStatus;
	}

	public void onPhase1Up() {
		mStatus = STATUS_CONNECTED;
		mPref.setIconLevel(mStatus);
	}

	public void onPhase1Down() {
		mStatus = STATUS_DISCONNECTED;
		mPref.setIconLevel(mStatus);	
	}
	
	public String toString() {
		return "Peer[" + getName() + " " + getRemoteAddr() + "]";
	}
}
