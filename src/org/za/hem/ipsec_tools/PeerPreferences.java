package org.za.hem.ipsec_tools;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.lamerman.FileDialog;

// Order "public protected private static final transient volatile"

/**
 * @author mikael
 * IPsec peer preference activity
 */
public class PeerPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String EXTRA_ID = "org.za.hem.ipsec_tools.ID";
	
	static final String TEMPLATE_PREFERENCE = "templatePref";
	static final String NAME_PREFERENCE = "namePref";
	static final String ENABLED_PREFERENCE = "enabledPref";
	static final String REMOTE_ADDR_PREFERENCE = "remoteAddrPref";
	static final String REMOTE_ADDR_IP_PREFERENCE = "remoteAddrIpPref";
	
	// FIXME
	private static final int REQUEST_SAVE = 1;
	private static final int REQUEST_LOAD = 2;
	
	private PeerID mID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mID = new PeerID(getIntent().getIntExtra(EXTRA_ID, -1));
		
		PreferenceManager manager = getPreferenceManager();
		manager.setSharedPreferencesName(getSharedPreferencesName(this, mID));
		addPreferencesFromResource(R.xml.peer_preferences);

		// Get the template preference
		Preference customPref = findPreference(TEMPLATE_PREFERENCE);
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PeerPreferences.this.getBaseContext(),
						FileDialog.class);
				intent.putExtra(FileDialog.START_PATH,
								Environment.getExternalStorageDirectory().getAbsolutePath());
				PeerPreferences.this.startActivityForResult(intent, REQUEST_SAVE);
				return true;
			}
		});
		
		Preference remoteAddrPref = findPreference(REMOTE_ADDR_PREFERENCE);
		remoteAddrPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange (Preference preference, Object newValue) {
		        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
    			String addr = (String)newValue;
	    		try {
					InetAddress ip = InetAddress.getByName(addr);
	    			Editor editor = sharedPreferences.edit();
	    			editor.putString(REMOTE_ADDR_IP_PREFERENCE, ip.getHostAddress());
	    			editor.commit();
				} catch (UnknownHostException e) {
					Log.i("ipsec-tools", e.toString());
					final Context context = preference.getContext();
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle(android.R.string.dialog_alert_title);
					String msgFormat = context.getString(R.string.unknown_host_name);
					String msg = String.format(msgFormat, addr);
					builder.setMessage(msg);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.show();
					return false;
		    	}
				return true;
			}
		});
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, final Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == REQUEST_SAVE) {
				System.out.println("Saving...");
			} else if (requestCode == REQUEST_LOAD) {
				System.out.println("Loading...");
			}
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);

			SharedPreferences templatePreference = getSharedPreferences(
					getSharedPreferencesName(this, mID), Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = templatePreference.edit();
			editor.putString(TEMPLATE_PREFERENCE, filePath);
			editor.commit();
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Logger.getLogger(PeerPreferences.class.getName()).log(
					Level.WARNING, "file not selected");
	    }

	}
	
	@Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        
        Map<String, ?> map = sharedPreferences.getAll();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext()){
        	String key = iter.next();
    		Preference pref= getPreferenceScreen().findPreference(key);
    		Object val = map.get(key);
    		UpdateSummary(pref, key, val);
        }
        
        // Set up a listener whenever a key changes            
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
	
	@Override
	protected void onPause() {
		super.onPause();

		// Unregister the listener whenever a key changes            
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i("ipsec-tools", "onSharedPreferenceChanged key:" + key);

		Map<String, ?> map = sharedPreferences.getAll();
		Preference pref= getPreferenceScreen().findPreference(key);
		Object val = map.get(key);
		UpdateSummary(pref, key, val);
	}
    
    @Override
	public void onBackPressed() {
        SharedPreferences sharedPreferences = getPreferenceScreen()
        	.getSharedPreferences();
    	if (sharedPreferences.getString(NAME_PREFERENCE, "").length() == 0) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(android.R.string.dialog_alert_title);
    		builder.setIcon(android.R.drawable.ic_dialog_alert);
    		builder.setMessage(R.string.msg_fill_in_name);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog alert = builder.create();
    		alert.show();
    		return;
    	}
		finish();
	}
    
    private void UpdateSummary(Preference pref, String key, Object val) {
        SharedPreferences sharedPreferences = getPreferenceScreen()
    	.getSharedPreferences();

    	if (key.equals(TEMPLATE_PREFERENCE)) {
    		pref.setSummary(val.toString());
    	} else if (key.equals(REMOTE_ADDR_PREFERENCE)) {
    		String host = (String)val;
    		String ip = sharedPreferences.getString(REMOTE_ADDR_IP_PREFERENCE,null);
    		if (ip != null && !ip.equals(host))
    			pref.setSummary(host + "/" + ip);
    		else
    			pref.setSummary(host);
    	} else if (pref instanceof EditTextPreference) {
			pref.setSummary(val.toString());
		} else if (pref instanceof CheckBoxPreference) {
		} else if (pref instanceof ListPreference) {
			pref.setSummary("List " + val);
		}
    }
	
	public static String getSharedPreferencesName(Context context, PeerID id) {
	    return context.getPackageName() + "_" + id;
	}
}
