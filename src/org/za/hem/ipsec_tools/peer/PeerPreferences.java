package org.za.hem.ipsec_tools.peer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

import org.za.hem.ipsec_tools.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
 * IPsec peer preference activity
 * Managed by Peer object.
 *
 * @author mikael
 */
public class PeerPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String EXTRA_ID = "org.za.hem.ipsec_tools.ID";
	
	static final String TEMPLATE_PREFERENCE = "templatePref";
	static final String CERT_PREFERENCE = "certPref";
	static final String KEY_PREFERENCE = "keyPref";
	static final String NAME_PREFERENCE = "namePref";
	static final String ENABLED_PREFERENCE = "enabledPref";
	static final String REMOTE_ADDR_PREFERENCE = "remoteAddrPref";
	static final String REMOTE_ADDR_IP_PREFERENCE = "remoteAddrIpPref";
	static final String DNS1_PREFERENCE = "dns1Pref";
	static final String DNS2_PREFERENCE = "dns2Pref";
	static final String PSK_PREFERENCE = "pskPref";
	
	static final int REQUEST_TEMPLATE = 1;
	static final int REQUEST_CERT = 2;
	static final int REQUEST_KEY = 3;
	
	private PeerID mID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mID = new PeerID(getIntent().getIntExtra(EXTRA_ID, -1));
		
		PreferenceManager manager = getPreferenceManager();
		manager.setSharedPreferencesName(getSharedPreferencesName(this, mID));
		addPreferencesFromResource(R.xml.peer_preferences);

		setFilePathListener(TEMPLATE_PREFERENCE, REQUEST_TEMPLATE);
		setFilePathListener(CERT_PREFERENCE, REQUEST_CERT);
		setFilePathListener(KEY_PREFERENCE, REQUEST_KEY);

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

	private void setFilePathListener(String pref, final int requestCode) {
		Preference customPref = findPreference(pref);
		SharedPreferences shared = getSharedPreferences(
				getSharedPreferencesName(this, mID), Activity.MODE_PRIVATE);
		String tmpl = shared.getString(pref, null);
		final String startPath;
		if (tmpl != null) {
			File tmplFile = new File(tmpl);
			startPath = tmplFile.getParentFile().getAbsolutePath(); 
		} else {
			startPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(PeerPreferences.this.getBaseContext(),
						FileDialog.class);
				intent.putExtra(FileDialog.START_PATH, startPath);
				PeerPreferences.this.startActivityForResult(intent, requestCode);
				return true;
			}
		});
	}
		

	@Override
	protected void onActivityResult (int requestCode, int resultCode, final Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case REQUEST_TEMPLATE:
				putFilePath(TEMPLATE_PREFERENCE, data);
				break;
			case REQUEST_CERT:
				putFilePath(CERT_PREFERENCE, data);
				break;
			case REQUEST_KEY:
				putFilePath(KEY_PREFERENCE, data);
				break;
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Logger.getLogger(PeerPreferences.class.getName()).log(
					Level.WARNING, "file not selected");
	    }

	}

	private void putFilePath(String pref, final Intent data) {
		String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
	
		SharedPreferences preference = getSharedPreferences(
			getSharedPreferencesName(this, mID), Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = preference.edit();
		editor.putString(pref, filePath);
		editor.commit();
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

    	if (key.equals(TEMPLATE_PREFERENCE) ||
	    key.equals(CERT_PREFERENCE) ||
	    key.equals(KEY_PREFERENCE)) {
    		pref.setSummary(val.toString());
    	} else if (key.equals(REMOTE_ADDR_PREFERENCE)) {
    		String host = (String)val;
    		String ip = sharedPreferences.getString(REMOTE_ADDR_IP_PREFERENCE,null);
    		if (ip != null && !ip.equals(host))
    			pref.setSummary(host + "/" + ip);
    		else
    			pref.setSummary(host);
	} else if (key.equals(PSK_PREFERENCE)) {
		String psk = (String)val;
		if (psk.length() <= 0) {
			pref.setSummary("");
		} else {
			pref.setSummary("********");
		}
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
