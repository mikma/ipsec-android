package org.za.hem.ipsec_tools;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.os.IBinder;
import android.content.ComponentName;;


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

public class IPsecToolsActivity extends Activity {
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
	private TextView outputView;
	private Handler handler = new Handler();
	private boolean mIsBound;
	private NativeService mBoundService;
	private NativeCommand mNative;
	
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
    	Log.i("IPsecToolsActivity", "onCreate:" + this);
            super.onCreate(savedInstanceState);
            mNative = new NativeCommand(this);
            setContentView(R.layout.ipsec_tools_activity);
            for (int i=0; i < binaries.length; i++) {
            	mNative.putBinary(binaries[i]);
            }
            outputView = (TextView)findViewById(R.id.output);
            Button prefBtn = (Button) findViewById(R.id.pref_button);
            prefBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                            Intent settingsActivity = new Intent(getBaseContext(),
                                            Preferences.class);
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
    	doBindService();    	
    }
    
    protected void onPause()
    {
    	Log.i("IPsecToolsActivity", "onPause:" + this);
    	super.onPause();
    	doUnbindService();
    	unregisterReceiver(mReceiver);
    }

    protected void onStop()
    {
    	Log.i("IPsecToolsActivity", "onStop:" + this);
    	super.onStop();
    }
    
    protected void onDestroy()
    {
    	Log.i("IPsecToolsActivity", "onDestroy:" + this);
    	super.onDestroy();
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
