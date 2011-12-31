package org.za.hem.ipsec_tools;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.Preference;
import android.util.Log;

/**
 * Peer controller object
 * 
 * @author mikael
 *
 */
public class Peer implements OnSharedPreferenceChangeListener {
	public static final int STATUS_DISCONNECTED = 0;
	public static final int STATUS_CONNECTED = 1;
	public static final int STATUS_PROGRESS = 2;
	public static final int STATUS_DISABLED = 3;
	public static final int STATUS_BUSY = 4;
	public static final int STATUS_NUM = 5;
	
	public static final int[] STATUS_SUMMARY = {
		R.string.connect_peer,
		R.string.disconnect_peer,
		R.string.progress_peer,
		R.string.connect_peer,
	};
	
	/* 
	 * Icons
	 * 
	 * 0 - presence_invisible	grey dot
     * 1 - presence_online		green dot
     * 2 - presence_away		blue clock
     * 3 - presence_offline		grey cross
     * 4 - presence_busy		red dash
	 */

	public static final int[] STATUS_ICON = {
		
	};
	
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
		mStatus = -1;
		setStatus(STATUS_DISCONNECTED);
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

	public boolean canConnect() {
		return isEnabled() && mStatus == STATUS_DISCONNECTED;
	}

	public boolean canDisconnect() {
		return mStatus == STATUS_CONNECTED || mStatus == STATUS_PROGRESS;
	}
	
	public boolean isConnected() {
		return mStatus == STATUS_CONNECTED;
	}
	
	public boolean isDisconnected() {
		return mStatus == STATUS_DISCONNECTED;
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
			InetAddress ipAddr = InetAddress.getByName(ip);
			InetAddress addr = InetAddress.getByAddress(host, ipAddr.getAddress());
			Log.i("ipsec-tools", "getRemoteAddr " + addr);
			return addr;
		} catch (UnknownHostException e) {
			return null;
		}
	}
	
	public InetAddress getLocalAddr() {
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
		if (mStatus != status && status < STATUS_NUM) {
			mStatus = status;
			if (mPref != null) {
				mPref.setIconLevel(mStatus);
				mPref.setSummary(STATUS_SUMMARY[mStatus]);
			}
			Log.i("ipsec-tools", "setStatus " + getName() + " "+ mStatus);
		}
	}

	/** Called when Phase 1 goes up */
	public void onPhase1Up() {
		setStatus(STATUS_CONNECTED);
	}

	/** Called when Phase 1 goes down */
	public void onPhase1Down() {
		setStatus(isEnabled() ? STATUS_DISCONNECTED : STATUS_DISABLED);
	}
	
	/** Called when initiating disconnect */
	public void onConnect() {
    	setStatus(STATUS_PROGRESS);
	}
	
	/** Called when initiating disconnect */
	public void onDisconnect() {
    	setStatus(STATUS_PROGRESS);
	}

	public void onPreferenceActivityResume() {
		mShared.registerOnSharedPreferenceChangeListener(this);
		updatePreferenceName();
	}
	
	public void onPreferenceActivityPause() {
		mShared.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		//Called when a shared preference is changed, added, or removed.
		Log.i("ipsec-tools", "peer pref " + key + " changed");
		if (key.equals(PeerPreferences.NAME_PREFERENCE)) {
			updatePreferenceName();
		}
	}
	
	protected void updatePreferenceName() {
		getPreference().setTitle(getName());		
	}
	
	public String toString() {
		return "Peer[" + getName() + " " + getRemoteAddr() + "]";
	}
}
