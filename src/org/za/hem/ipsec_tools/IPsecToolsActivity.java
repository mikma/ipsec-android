package org.za.hem.ipsec_tools;

import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
			"racoon.sh",
			"racoonctl.sh",
			"setkey.sh"
 	};
    // FIXME debugging
	private final boolean DEBUG = true;

	private Handler handler = new Handler();
	private boolean mIsBound;
	private NativeService mBoundService;
	private NativeCommand mNative;
	private static final String ADD_PREFERENCE = "addPref";
	private static final String PEERS_PREFERENCE = "peersPref";
	private static final String COUNT_PREFERENCE = "countPref";
	private ArrayList<Preference> mPeers;
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

        if (DEBUG) {
        	NativeCommand.system("killall racoon");
        }

		addPreferencesFromResource(R.xml.preferences);

        mNative = new NativeCommand(this);
        for (int i=0; i < binaries.length; i++) {
        	mNative.putBinary(binaries[i]);
        }
        try {
			mNative.putZipBinaries("ipsec-tools.zip");
		} catch (IOException e) {
			throw new RuntimeException(e);
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
        int count = sharedPreferences.getInt(COUNT_PREFERENCE,0);
        mPeers = new ArrayList<Preference>(count);
        
    	Log.i("IPsecToolsActivity", "Count: " + count);
        for (int i = 0; i < count; i++) {
        	PeerID id = new PeerID(i);
        	String key = id.toString();
        	Log.i("IPsecToolsActivity", "Add pref: " + key);
        	if (sharedPreferences.getBoolean(key, true)) {
        		Preference peerPref = new Preference(this);
        		peerPref.setKey(key);
        		peerPref.setSummary(R.string.connect_peer);
        		peerPref.setOnPreferenceClickListener(this);
        		peerPref.setWidgetLayoutResource(R.layout.peer_widget_off);
            	Log.i("IPsecToolsActivity", "Add peerPref: " + key);
        		peersPref.addPreference(peerPref);
        		mPeers.add(peerPref);
        	} else {
        		mPeers.add(null);
        	}
    		id = id.next();
        }
        
        if (DEBUG) {
        	doBindService();
        }
    }
    
    protected void startService() {
    	if (mIsBound)
    		return;
    	doBindService();
    }
    
    protected void stopService() {
    	if (!mIsBound)
    		return;
    	doUnbindService();
    }

    protected void connectPeer(final PeerID id) {
    	// FIXME hardcoded!
    	if (mBoundService == null)
    		return;
    		
    	SharedPreferences peerPreferences =
    		getSharedPreferences(
    				PeerPreferences.getSharedPreferencesName(this, id),
    				Activity.MODE_PRIVATE);
		String addr = peerPreferences.getString(PeerPreferences.REMOTE_ADDR_PREFERENCE, "");
   		mBoundService.vpnConnect(addr);
    	
    }
    
    protected void disconnectPeer(final PeerID id) {
    	// FIXME hardcoded!
    	if (mBoundService == null)
    		return;
        	
    	SharedPreferences peerPreferences =
    		getSharedPreferences(
    				PeerPreferences.getSharedPreferencesName(this, id),
    				Activity.MODE_PRIVATE);
    	String addr = peerPreferences.getString(PeerPreferences.REMOTE_ADDR_PREFERENCE, "");
    	mBoundService.vpnDisconnect(addr);
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
		Preference peerPref = mPeers.get(id.intValue());
    	Log.i("IPsecToolsActivity", "Remove peerPref: " + mPeers.size() + " " + id + " " + peerPref);
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

        int empty = mPeers.indexOf(null);
        if (empty == -1) {
        	empty = sharedPreferences.getInt(COUNT_PREFERENCE, 0);
        	mPeers.ensureCapacity(empty + 1);
        }
    	
    	PeerID newId = new PeerID(empty);
    	String key = newId.toString();

    	Preference peerPref = new Preference(this);
    	peerPref.setKey(key);
    	peerPref.setSummary(R.string.connect_peer);
    	peerPref.setOnPreferenceClickListener(this);
    	peersPref.addPreference(peerPref);
        mPeers.set(empty, peerPref);
    	
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
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(NativeService.ACTION_DESTROYED);
    	filter.addAction(NativeService.ACTION_PHASE1_UP);
    	filter.addAction(NativeService.ACTION_PHASE1_DOWN);
    	registerReceiver(mReceiver, filter);
        registerForContextMenu(getListView());

		SharedPreferences sharedPreferences =
        	getPreferenceScreen().getSharedPreferences();

		for (int i=0; i < mPeers.size(); i++) {
    		PeerID id = new PeerID(i);

    		if (sharedPreferences.getBoolean(id.toString(), true)
    				&& mPeers.get(i) != null ) {
    			SharedPreferences peerPreferences =
    				getSharedPreferences(
    						PeerPreferences.getSharedPreferencesName(this, id),
    						Activity.MODE_PRIVATE);
    			String name = peerPreferences.getString(PeerPreferences.NAME_PREFERENCE, "");
    	
    			mPeers.get(i).setTitle(name);
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.start_service:
	        startService();
	        return true;
	    case R.id.stop_service:
	        stopService();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		
    		if (action.equals(NativeService.ACTION_PHASE1_UP)) {
    			// FIXME hardcoded Peer
    			mPeers.get(1).setWidgetLayoutResource(R.layout.peer_widget);
    		} else if (action.equals(NativeService.ACTION_PHASE1_DOWN)) {
    			// FIXME hardcoded Peer
    			mPeers.get(1).setWidgetLayoutResource(R.layout.peer_widget_off);    			
    		}
    		//output("Receive destroyed");
            Log.i("ipsec-tools", "broadcast received: " + intent);
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
		ComponentName nativeService = startService(new Intent(IPsecToolsActivity.this, 
	            NativeService.class));
	    bindService(new Intent(IPsecToolsActivity.this, 
	            NativeService.class), mConnection, 0);
	    mIsBound = true;
	}
	
	void doUnbindService() {
	    if (mIsBound) {
	        // Detach our existing connection.
	        unbindService(mConnection);
        	stopService(new Intent(IPsecToolsActivity.this, 
        			NativeService.class));
	        mIsBound = false;
	    }
	}
	
    private void output(final String str) {
    	int duration = Toast.LENGTH_SHORT;

    	Toast toast = Toast.makeText(this, str, duration);
    	toast.show();
    }
}
