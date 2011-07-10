package org.za.hem.ipsec_tools;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lamerman.FileDialog;

/*
 * Register
 * android.telephony.TelephonyManager.DATA_CONNECTED
 * android.telephony.TelephonyManager.DATA_DISCONNECTED
 * 
 * Context.getSystemService(Context.CONNECTIVITY_SERVICE).
 * CONNECTIVITY_ACTION
 */

/**
 * 
 * @author mikael
 *
 */

public class IPsecToolsActivity extends PreferenceActivity
		implements OnPreferenceClickListener {
	final private String binaries[] = {
			"libcrypto.so",
			"libipsec.so",
			"libracoonlib.so",
			"libssl.so",
			"openssl",
			"racoon",
			"racoonctl",
			"racoon.sh",
			"setkey",
			"setkey.sh"
 	};
	private Handler handler = new Handler();
	private boolean mIsBound;
	private NativeService mBoundService;
	private NativeCommand mNative;
	private static final String ADD_PREFERENCE = "addPref";
	private static final String PEERS_PREFERENCE = "peersPref";
	private static final String COUNT_PREFERENCE = "countPref";
	private Map<PeerID, Preference> peers;
	private PeerID selectedID;
	
	/*
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(LOG_TAG, ex.toString());
	    }
	    return null;
*/
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		final Activity activity = this;

		addPreferencesFromResource(R.xml.preferences);

        mNative = new NativeCommand(this);
        for (int i=0; i < binaries.length; i++) {
        	mNative.putBinary(binaries[i]);
        }

		Preference addPref = findPreference(ADD_PREFERENCE);
		addPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
                Intent settingsActivity = new Intent(getBaseContext(),
                        PeerPreferences.class);
                PeerID id = createPeer();
                settingsActivity.putExtra(PeerPreferences.EXTRA_ID, id.intValue());
                startActivity(settingsActivity);
				return true;
			}
		});

    	// For each id, update name
		PreferenceGroup peersPref = (PreferenceGroup)findPreference(PEERS_PREFERENCE);
    	peersPref.removeAll();
        SharedPreferences sharedPreferences =
        	getPreferenceScreen().getSharedPreferences();
        PeerID id = new PeerID(0);
        int count = sharedPreferences.getInt(COUNT_PREFERENCE,0);
        peers = new HashMap<PeerID, Preference>();
        
        for (int i = 0; i < count; i++) {
        	String key = id.toString();
        	Log.i("IPsecToolsActivity", "Add pref: " + key);
    		Preference peerPref = new Preference(this);
    		peerPref.setKey(key);
    		peerPref.setSummary(R.string.connect_peer);
    		peerPref.setOnPreferenceClickListener(this);
        	if (sharedPreferences.getBoolean(key, true)) {
            	Log.i("IPsecToolsActivity", "Add peerPref: " + key);
        		peersPref.addPreference(peerPref);
        	}
    		peers.put(id, peerPref);
    		id = id.next();
        }
    	Log.i("IPsecToolsActivity", "Add peerPref[0]: " + peers.get(new PeerID(0)));
        
        /*    	
    	Log.i("IPsecToolsActivity", "onCreate:" + this);
            setContentView(R.layout.ipsec_tools_activity);

            Button prefBtn = (Button) findViewById(R.id.pref_button);
            prefBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                            Intent settingsActivity = new Intent(getBaseContext(),
                                            PeerPreferences.class);
                            // FIXME
        					settingsActivity.putExtra(PeerPreferences.EXTRA_ID,
        							17);
                            startActivity(settingsActivity);
                    }
            });
            Button startBtn = (Button) findViewById(R.id.start_button);
            startBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	// TODO start service
                    	output("Starting VPN...");
                    	doBindService();
                    }
            });
            Button stopBtn = (Button) findViewById(R.id.stop_button);
            stopBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                    	// TODO start service
                    	output("Stopping VPN...");
                    	doUnbindService();
                    }
            });
            */
    }

    protected void connectPeer(final PeerID id) {
    	// FIXME
    }
    
    protected void disconnectPeer(final PeerID id) {
    	// FIXME
    }
    
    protected void togglePeer(final PeerID id) {
    	// FIXME
    }

    protected void editPeer(final PeerID id) {
        Intent settingsActivity = new Intent(getBaseContext(),
                PeerPreferences.class);
        settingsActivity.putExtra(PeerPreferences.EXTRA_ID, id.intValue());
        startActivity(settingsActivity);
    }
    
    protected void deletePeer(PeerID id) {
		PreferenceGroup peersPref = (PreferenceGroup)findPreference(PEERS_PREFERENCE);
		Preference peerPref = peers.get(id);
    	Log.i("IPsecToolsActivity", "Remove peerPref: " + peers.size() + " " + id + " " + peerPref + " " + peers.get(new PeerID(0)));
		peersPref.removePreference(peerPref);

		SharedPreferences.Editor editor;
		
		// Hide peer
		SharedPreferences sharedPreferences =
        	getPreferenceScreen().getSharedPreferences();
		editor = sharedPreferences.edit();
		editor.putBoolean(id.toString(), false);
		editor.commit();

		// Clear peer preferences
		SharedPreferences peerPreferences =
			getSharedPreferences(
					PeerPreferences.getSharedPreferencesName(this, id),
					Activity.MODE_PRIVATE);
		editor = peerPreferences.edit();
		editor.clear();
		editor.commit();
    }
    
    protected PeerID createPeer()
    {
    	PreferenceGroup peersPref = (PreferenceGroup)findPreference(PEERS_PREFERENCE);
        SharedPreferences sharedPreferences =
        	getPreferenceScreen().getSharedPreferences();
        // Start transaction
        SharedPreferences.Editor editor = sharedPreferences.edit();

    	Set<PeerID> set = peers.keySet();
    	PeerID[] ids = new PeerID[set.size()];
    	set.toArray(ids);
    	Arrays.sort(ids);
    	
    	PeerID last = new PeerID();
    	for (int i = 0; i < ids.length; i++) {
    		PeerID id = ids[i];
    		if (id != last.next()) {
    			break;
    		}
    		last = id;
    	}
    	
    	PeerID newId = last.next();
    	String key = newId.toString();

    	Preference peerPref = new Preference(this);
    	peerPref.setKey(key);
    	peerPref.setSummary(R.string.connect_peer);
    	peerPref.setOnPreferenceClickListener(this);
    	peersPref.addPreference(peerPref);
        peers.put(newId, peerPref);
        if (peerPref != peers.get(new PeerID(newId.intValue()))) {
        	throw new RuntimeException();
        }
    	
        editor.putBoolean(key, true);
        editor.commit();
    	return newId;
    }
    
    protected void onStart()
    {
    	Log.i("IPsecToolsActivity", "onStart:" + this);
    	super.onStart();
    }
    
    protected void onResume()
    {
    	Log.i("IPsecToolsActivity", "onResume:" + this);
    	super.onResume();
    	registerReceiver(mReceiver, new IntentFilter("org.za.hem.ipsec_tools.DESTROYED"));
        registerForContextMenu(getListView());
    	//doBindService();    	

		SharedPreferences sharedPreferences =
        	getPreferenceScreen().getSharedPreferences();

		Set<PeerID> keys = peers.keySet();
    	Iterator<PeerID> iter = keys.iterator();
    	while (iter.hasNext()) {
    		PeerID id = iter.next();
    		
    		if (sharedPreferences.getBoolean(id.toString(), true)) {
    			SharedPreferences peerPreferences =
    				getSharedPreferences(
    						PeerPreferences.getSharedPreferencesName(this, id),
    						Activity.MODE_PRIVATE);
    			String name = peerPreferences.getString(PeerPreferences.NAME_PREFERENCE, "");
    	
    			peers.get(id).setTitle(name);
    		}
    	}
        
        // Set up a listener whenever a key changes
    	// TODO register all peer listeners
        //sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    
    protected void onPause()
    {
    	Log.i("IPsecToolsActivity", "onPause:" + this);
    	super.onPause();
    	//doUnbindService();
    	unregisterReceiver(mReceiver);
		unregisterForContextMenu(getListView());

    	// Unregister the listener whenever a key changes
    	// TODO unregister all peer listeners
    	//getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }
    
    @Override
    protected void onStop()
    {
    	Log.i("IPsecToolsActivity", "onStop:" + this);
    	super.onStop();
    }
    
    @Override
    protected void onDestroy()
    {
    	Log.i("IPsecToolsActivity", "onDestroy:" + this);
    	super.onDestroy();
    }
        
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		ListView list = (ListView)v;
		Preference pref = (Preference)list.getItemAtPosition(info.position);
		
		try {
			selectedID = PeerID.fromString(pref.getKey());
		
			if (selectedID.isValid()) {
				Logger.getLogger(IPsecToolsActivity.class.getName()).log(
						Level.WARNING, "onCreateContextMenu " + info.id + " " + info.position + " " + pref);
		
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.peer_menu, menu);
			}
		} catch (PeerID.KeyFormatException e) {
			Logger.getLogger(IPsecToolsActivity.class.getName()).log(
					Level.SEVERE, "onCreateContextMenu " + e);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	
		switch (item.getItemId()) {
		case R.id.connect_peer:
			connectPeer(selectedID);
			return true;
		case R.id.disconnect_peer:
			disconnectPeer(selectedID);
			return true;
		case R.id.edit_peer:
			editPeer(selectedID);
			return true;
		case R.id.delete_peer:
			deletePeer(selectedID);
			return true;
		default:
			return super.onContextItemSelected(item);
	  }
	}
	
	@Override
	public void onContextMenuClosed(Menu menu) {
		selectedID = null;
	}
	
	@Override
	public boolean onPreferenceClick(Preference arg0) {
		try {
			PeerID id = PeerID.fromString(arg0.getKey());
			Log.i("IPsecToolsActivity", "Click " + id);
			togglePeer(id);
			return true;
		} catch (PeerID.KeyFormatException e) {
			return false;
		}
	}

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		//output("Receive destroyed");
            Log.i("LocalIPSecToolsActivity", "received destroyed");
    	}  	
    };
    
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        mBoundService = ((NativeService.NativeBinder)service).getService();
	        output("Connected");
	        // Tell the user about this for our demo.
//	        Toast.makeText(Binding.this, R.string.native_service_connected,
	//                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;
	        output("Disconnected");
	  //      Toast.makeText(Binding.this, R.string.native_service_disconnected,
	    //            Toast.LENGTH_SHORT).show();
	    }
	};
	
	void doBindService() {
		// Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		//ComponentName nativeService = startService(new Intent(IPsecToolsActivity.this, 
	    //        NativeService.class));
	    bindService(new Intent(IPsecToolsActivity.this, 
	            NativeService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
        	//stopService(new Intent(IPsecToolsActivity.this, 
        	//		NativeService.class));
	        mIsBound = false;
	    }
	}
	
    private void output(final String str) {
    	int duration = Toast.LENGTH_SHORT;

    	Toast toast = Toast.makeText(this, str, duration);
    	toast.show();
    }
}
