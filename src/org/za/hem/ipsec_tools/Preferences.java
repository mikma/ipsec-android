package org.za.hem.ipsec_tools;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
 
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.global_preferences);
	}
 
	@Override
    protected void onResume() {
        super.onResume();
    }
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Let's do something a preference value changes
	}
 }