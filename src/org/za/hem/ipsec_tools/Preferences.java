package org.za.hem.ipsec_tools;

import java.util.Map;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.EditTextPreference;
import android.widget.Toast;
 
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private EditTextPreference mEditTextPreference;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
          
		mEditTextPreference = (EditTextPreference)getPreferenceScreen().findPreference("editTextPref");
		
		PreferenceCategory mPeers = (PreferenceCategory)getPreferenceScreen().findPreference("peers");
		mPeers.addPreference(new IPsecPreference(this));
                                              
		// Get the custom preference
		Preference customPref = (Preference) findPreference("customPref");
			customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					Toast.makeText(getBaseContext(),
							"The custom preference has been clicked",
							Toast.LENGTH_LONG).show();
                			SharedPreferences customSharedPreference = getSharedPreferences(
                					"myCustomSharedPrefs", Activity.MODE_PRIVATE);
                			SharedPreferences.Editor editor = customSharedPreference
                			.edit();
                			editor.putString("myCustomPref",
                			"The preference has been clicked");
                			editor.commit();
                	return true;
                }
              });
                
	}
 
	@Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        // Setup the initial values
        mEditTextPreference.setSummary("Current value is " + sharedPreferences.getString("editTextPref", ""));
        
        Map<String, ?> map = sharedPreferences.getAll();
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext()){        
        	Object val = map.get(iter.next());
        	if (val instanceof EditTextPreference) {
        		EditTextPreference pref = (EditTextPreference)val;
        		pref.setSummary("Current value " + pref.getText());
        	}
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
		// Let's do something a preference value changes
		if (key.equals("editTextPref")) {
			mEditTextPreference.setSummary(sharedPreferences.getString(key, ""));
	    }
	}

	    
	boolean CheckboxPreference;
	String ListPreference;
	String editTextPreference;
	String ringtonePreference;
	String secondEditTextPreference;
	String customPref;
 
	private void getPrefs() {
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager
		.getDefaultSharedPreferences(getBaseContext());
		CheckboxPreference = prefs.getBoolean("checkboxPref", true);
		ListPreference = prefs.getString("listPref", "nr1");
		editTextPreference = prefs.getString("editTextPref",
		"Nothing has been entered");
		ringtonePreference = prefs.getString("ringtonePref",
		"DEFAULT_RINGTONE_URI");
		secondEditTextPreference = prefs.getString("SecondEditTextPref",
		"Nothing has been entered");
		// Get the custom preference
		SharedPreferences mySharedPreferences = getSharedPreferences(
				"myCustomSharedPrefs", Activity.MODE_PRIVATE);
                customPref = mySharedPreferences.getString("myCusomPref", "");
	}
}