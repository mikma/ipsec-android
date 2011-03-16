package org.za.hem.ipsec_tools;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;


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
	private File filesDir;
	private TextView outputView;
	private Handler handler = new Handler();
	
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
            setContentView(R.layout.ipsec_tools_activity);
            filesDir = getDir("bin", MODE_PRIVATE);
            putBinary("setkey");
            putBinary("setkey.sh");
            outputView = (TextView)findViewById(R.id.output);
            Button prefBtn = (Button) findViewById(R.id.pref_button);
            if (prefBtn != null)
            {
            prefBtn.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                            Intent settingsActivity = new Intent(getBaseContext(),
                                            Preferences.class);
                            startActivity(settingsActivity);
                    }
            });
            }
    		output(execute("/system/bin/ls " + new File(filesDir, "lib").getAbsolutePath()));
    }
    
    /*
     */
    private void putBinary(String fileName) {
    	try {
    		File file = new File(filesDir, fileName);
    		InputStream input = getAssets().open(fileName);
    		int read;
    		byte[] buffer = new byte[4096];
    		OutputStream output = new FileOutputStream(file);
    		
    		while ((read = input.read(buffer)) > 0) {
    			output.write(buffer, 0, read);
    		}
    		input.close();
    		output.close();
    		execute("/system/bin/chmod 711 " + file.getAbsolutePath());
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private String execute(String cmd) {
    	try {
    		// Executes the command.
    		Process process = Runtime.getRuntime().exec(cmd);
        
    		// Reads stdout.
    		// NOTE: You can write to stdin of the command using
    		//       process.getOutputStream().
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(process.getInputStream()));
    		int read;
    		char[] buffer = new char[4096];
    		StringBuffer output = new StringBuffer();
    		while ((read = reader.read(buffer)) > 0) {
    			output.append(buffer, 0, read);
    		}
    		reader.close();
        
    		// Waits for the command to finish.
    		process.waitFor();
        
    		return output.toString();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    private void output(final String str) {
    	Runnable proc = new Runnable() {
			public void run() {
				outputView.setText(str);
			}
    	};
    	handler.post(proc);
    }
}
