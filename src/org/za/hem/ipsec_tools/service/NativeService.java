package org.za.hem.ipsec_tools.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;

import org.za.hem.ipsec_tools.IPsecToolsActivity;
import org.za.hem.ipsec_tools.R;
import org.za.hem.ipsec_tools.R.drawable;
import org.za.hem.ipsec_tools.R.string;
import org.za.hem.ipsec_tools.racoon.Admin;
import org.za.hem.ipsec_tools.racoon.Command;
import org.za.hem.ipsec_tools.racoon.Event;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.IBinder;
import android.util.Log;


public class NativeService extends Service {
	public static final int HANDLER_RACOON_STARTED = 1;
	
	/* Worker handlers */
	public static final int HANDLER_INTENT = 1;
	public static final int HANDLER_VPN_CONNECT = 2;
	public static final int HANDLER_VPN_DISCONNECT = 3;
	
	public static final String PACKAGE = "org.za.hem.ipsec_tool";
	public static final String ACTION_NOTIFICATION = PACKAGE + ".NOTIFICATION";
	public static final String ACTION_DESTROYED = PACKAGE + ".DESTROYED";
	public static final String ACTION_PHASE1_UP = PACKAGE + ".PHASE1_UP";
	public static final String ACTION_PHASE1_DOWN = PACKAGE + ".PHASE1_DOWN";
	public static final String ACTION_VPN_CONNECT = PACKAGE + ".VPN_CONNECT";
	public static final String ACTION_VPN_DISCONNECT = PACKAGE + ".VPN_DISCONNECT";
	
	private HandlerThread mWorker;
	private NotificationManager mNM;
	private Admin mAdmin;
	
	// Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.native_service_started;
    private String mSocketPath;

    public class NativeBinder extends Binder {
        NativeService getService() {
            return NativeService.this;
        }
    }
    
    private Handler mWorkerHandler;

	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new NativeBinder();
    
   	@Override
    public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mSocketPath = new File(getDir("bin", 0), "racoon.sock").getPath();
		mWorker = new HandlerThread("NativeService");
		mWorker.start();

	    mWorkerHandler = new Handler(mWorker.getLooper()) {
	       	@Override
	    	public void handleMessage(Message msg) {
	       		switch (msg.what) {
	       		case HANDLER_INTENT:
		       		onHandleIntent((Intent)msg.obj);
		       		break;
	       		case HANDLER_VPN_CONNECT:
	       			onVpnConnect((String)msg.obj);
	       			break;
	       		case HANDLER_VPN_DISCONNECT:
	       			onVpnDisconnect((String)msg.obj);
	       			break;
	       		}
	       		
	    	}
	    };

	    // Display a notification about us starting.  We put an icon in the status bar.
		showNotification();
	}
   	   
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        
        if (intent.getAction() == null ) {
			mAdmin = new Admin();
			/*
			try {
				// FIXME DEBUG
				mAdmin.stop();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}*/

        	Log.i("LocalService", "Start thread");
        	new Thread(new Runnable() {
        		public void run() {
        			// FIXME DEBUGing code
        			//doRun();
            		Message.obtain(mHandler, HANDLER_RACOON_STARTED).sendToTarget();
        		}
        	}).start();
        } else if (intent.getAction().equals(ACTION_NOTIFICATION)) {
        	Log.i("ipsec-tools", "Notification");
        } else {
        	Message msg = mWorkerHandler.obtainMessage(HANDLER_INTENT);
        	msg.obj = intent;
        	msg.arg1 = flags;
        	msg.arg2 = startId;
        	msg.sendToTarget();
        }
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	
	@Override
    public void onDestroy() {
		mWorkerHandler.getLooper().quit();
		try {
			mAdmin.stop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mAdmin = null;
		
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_DESTROYED);
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
	
	protected void onHandleIntent(Intent intent) {
		
	}
	
	public void vpnConnect(String gw) {
		Message msg = mWorkerHandler.obtainMessage(HANDLER_VPN_CONNECT);
		msg.obj = gw;
		msg.sendToTarget();
	}
	
	protected void onVpnConnect(String gw) {
		// FIXME
		Admin adminCmd = new Admin();
		try {
			adminCmd.start(mSocketPath);
			InetAddress addr = InetAddress.getByName(gw);
			adminCmd.vpnConnect(addr);
			// TODO wait for acknowledge
			Thread.sleep(1000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				adminCmd.stop();
			} catch (IOException e) {
			}
		}
	}
	
	public void vpnDisconnect(String gw) {
		Message msg = mWorkerHandler.obtainMessage(HANDLER_VPN_DISCONNECT);
		msg.obj = gw;
		msg.sendToTarget();		
	}
	
	public void onVpnDisconnect(String gw) {
		// FIXME
		Admin adminCmd = new Admin();
		try {
			adminCmd.start(mSocketPath);
			InetAddress addr = InetAddress.getByName(gw);
			adminCmd.vpnDisconnect(addr);
			// TODO wait for acknowledge
			Thread.sleep(1000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				adminCmd.stop();
			} catch (IOException e) {
			}
		}
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

    private void doRun() {
		Process process = null;
		
		try {
			Log.i("LocalService", "Start process");
			File binDir = this.getDir("bin", 0);
			// TODO check getExternalStorageState()
			File ipsecDir = new File(Environment.getExternalStorageDirectory(), "ipsec");
			process = new ProcessBuilder()
    		.command(new File(binDir, "racoon.sh").getAbsolutePath(),
    				"-v", "-d",
    				"-f", new File(binDir, "racoon.conf").getAbsolutePath(),
    				"-l", new File(ipsecDir, "racoon.log").getAbsolutePath())
    		.redirectErrorStream(true)
    		.start();

    		InputStream in = process.getInputStream();
    		OutputStream out = process.getOutputStream();
    		
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(in));
    		int read;
    		char[] buffer = new char[4096];
    		while ((read = reader.read(buffer)) > 0) {
    		}
    		reader.close();
    		in.close();
    		out.close();
    		Message.obtain(mHandler, HANDLER_RACOON_STARTED).sendToTarget();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
		finally {
			if (process != null)
				process.destroy();
    	}
    }
    
    private Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case HANDLER_RACOON_STARTED:
    			foo();
    			break;
    		}
    	}
    };

    private void foo() {
    	try {
    		mAdmin.start(mSocketPath);
			mAdmin.showEvt();
			mAdmin.setOnCommandListener(new Admin.OnCommandListener() {
				public void onCommand(Command cmd) {
					if (cmd instanceof Event) {
						Event evt = (Event)cmd;
						String action = null;
						switch (evt.getType()) {
						case Event.EVT_PHASE1_UP:
							action = ACTION_PHASE1_UP;
							break;
						case Event.EVT_PHASE1_DOWN:
							action = ACTION_PHASE1_DOWN;
							break;
						default:
							Log.i("ipsec-tools", "Unhandled event type");
							break;
						}
						if (action != null) {
							Intent broadcastIntent = new Intent();
							broadcastIntent.setAction(action);
							//broadcastIntent.setData(Uri.parse("context://"+cer.getKey)));
							//broadcastIntent.putExtra("reading",cer);
							//broadcastIntent.addCategory("nl.vu.contextframework.CONTEXT");
							sendBroadcast(broadcastIntent);
						}
					}
					
					Log.i("ipsec-tools", "Command received " + cmd);
				}
			});

    	} catch (IOException e) {
    		throw new RuntimeException(e);
/*    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);*/
		}
    }
}
