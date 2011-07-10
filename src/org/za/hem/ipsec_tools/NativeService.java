package org.za.hem.ipsec_tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class NativeService extends Service {
	final public static String ACTION_NOTIFICATION = "org.za.hem.ipsec_tools.NOTIFICATION";
	
	private NotificationManager mNM;
	
	// Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.native_service_started;

    public class NativeBinder extends Binder {
        NativeService getService() {
            return NativeService.this;
        }
    }

	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new NativeBinder();
    
   	@Override
    public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting.  We put an icon in the status bar.
		showNotification();
	}
    
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        
        if (intent.getAction() == null ) {
        	new Thread(new Runnable() {
        		public void run() {
        			foo();
        		}
        	}).start();
        } else if (intent.getAction().equals(ACTION_NOTIFICATION)) {
        	Log.i("LocalService", "Notification");
        }
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	
	@Override
    public void onDestroy() {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction("org.za.hem.ipsec_tools.DESTROYED");
		//broadcastIntent.setData(Uri.parse("context://"+cer.getKey)));
		//broadcastIntent.putExtra("reading",cer);
		//broadcastIntent.addCategory("nl.vu.contextframework.CONTEXT");
		sendBroadcast(broadcastIntent);
		
        Log.i("LocalService", "Destroyed");

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.native_service_stopped, Toast.LENGTH_SHORT).show();
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.native_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT; 
        	
        //Intent intent = new Intent(this, NativeService.class);
        //intent.setAction(ACTION_NOTIFICATION);
        Intent intent = new Intent(this, IPsecToolsActivity.class);
        
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getService(this, 0,
                intent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.native_service_label),
                       text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    private void foo() {

		/*
		NativeCommand command = new NativeCommand(NativeService.this);

		Process process = new ProcessBuilder()
    		.command("/system/bin/ping", "android.com")
    		.redirectErrorStream(true)
    		.start();
    	try {
    		InputStream in = process.getInputStream();
    		OutputStream out = process.getOutputStream();

    		readStream(in);
    	}
    	finally {
    		process.destroy();
    	}
    	*/
    }
}
