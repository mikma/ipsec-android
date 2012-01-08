package org.za.hem.ipsec_tools.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
    	Log.i("ipsec-tools", "ConnectivityReceiver");
    	Log.i("ipsec-tools", "ConnectivityReceiver " + arg1.getExtras());
	}
}
