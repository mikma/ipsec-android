package org.za.hem.ipsec_tools.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.za.hem.ipsec_tools.ConfigManager;
import org.za.hem.ipsec_tools.IPsecToolsActivity;
import org.za.hem.ipsec_tools.NativeCommand;
import org.za.hem.ipsec_tools.R;
import org.za.hem.ipsec_tools.racoon.Admin;
import org.za.hem.ipsec_tools.racoon.Command;
import org.za.hem.ipsec_tools.racoon.Event;
import org.za.hem.ipsec_tools.racoon.Ph1Dump;
import org.za.hem.ipsec_tools.racoon.Ph1Dump.Item;

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
	public static final int HANDLER_DUMP_ISAKMP_SA = 4;
	
	public static final String PACKAGE = "org.za.hem.ipsec_tool.service";
	public static final String ACTION_NOTIFICATION = PACKAGE + ".NOTIFICATION";
	public static final String ACTION_DESTROYED = PACKAGE + ".DESTROYED";
	public static final String ACTION_PHASE1_UP = PACKAGE + ".PHASE1_UP";
	public static final String ACTION_PHASE1_DOWN = PACKAGE + ".PHASE1_DOWN";
	public static final String ACTION_VPN_CONNECT = PACKAGE + ".VPN_CONNECT";
	public static final String ACTION_VPN_DISCONNECT = PACKAGE + ".VPN_DISCONNECT";
	public static final String ACTION_SERVICE_READY = PACKAGE + ".SERVICE_READY";
	
	private HandlerThread mWorker;
	private NotificationManager mNM;
	private Admin mAdmin;
	private Admin mAdminCmd;
	
	// Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.native_service_started;
    private String mSocketPath;
    private int mPid = -1;

    static public class NativeBinder extends Binder {
    	NativeService mService;
    	
    	protected void setService(NativeService service) {
    		mService = service;
    	}
    	
    	protected void clearService() {
    		mService = null;
    	}
    	
        public NativeService getService() {
            return mService;
        }
    }
    
    private Handler mWorkerHandler;

	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final NativeBinder mBinder = new NativeBinder();
    
   	@Override
    public void onCreate() {
   		mBinder.setService(this);
   		
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
	       		case HANDLER_DUMP_ISAKMP_SA:
	       			onDumpIsakmpSA();
	       			break;
	       		default:
	       			Log.i("ipsec-tools", "Unhandled " + msg.obj);
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
        
        if (intent == null || intent.getAction() == null ) {
			mAdmin = new Admin(mSocketPath);
			mAdminCmd = new Admin(mSocketPath);

        	Log.i("LocalService", "Start thread");
        	new Thread(new Runnable() {
        		public void run() {
        			// FIXME DEBUGing code
        			startRacoon();
            		//Message.obtain(mHandler, HANDLER_RACOON_STARTED).sendToTarget();
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
			mAdmin.close();
		} catch (IOException e) {
		}
		mAdmin = null;
		mAdminCmd = null;
		
		// Kill racoon instance
		if (mPid > 0) {
   			Log.i("ipsec-tools", "kill racoon  " + mPid);
			File binDir = this.getDir("bin", 0);
			String out = NativeCommand.system(new File(binDir, "killracoon.sh").getAbsolutePath() + " " + mPid);
   			Log.i("ipsec-tools", "outt " + out);
			mPid = -1;
		}
		
		// TODO clear setkey?
		
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_DESTROYED);
		sendBroadcast(broadcastIntent);
		
        Log.i("ipsec-tools", "Destroyed");

        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);
        
        // Clear service reference in binder
        mBinder.clearService();

        // Tell the user we stopped.
        //Toast.makeText(this, R.string.native_service_stopped, Toast.LENGTH_SHORT).show();
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	protected void onHandleIntent(Intent intent) {
		
	}
	
	public int reloadConf() {
		return mAdminCmd.reloadConf();
	}
	
	public boolean isRacoonRunning() {
		return mPid > 0;
	}
	
	public void dumpIsakmpSA() {
		Log.i("ipsec-tools", "dumpIsakmpSA");
		Message msg = mWorkerHandler.obtainMessage(HANDLER_DUMP_ISAKMP_SA);
		msg.sendToTarget();
	}
	
	protected void onDumpIsakmpSA() {
		Command cmd = mAdminCmd.dumpIsakmpSA();
		Ph1Dump pd = (Ph1Dump)cmd;
		if (pd == null)
			return;
		Iterator<Item> iter = pd.getItems().iterator();
			
		while (iter.hasNext()) {
			Item dump = iter.next();

			Log.i("ipsec-tools", "onDumpIsakmpSA " + dump.mRemote);
	
			if (mListener == null) {
				Log.i("ipsec-tools", "No listener " + this);			
				return;
			}
	
			final int INITIATOR = 0;
			//final int RESPONDER = 1; 
			InetSocketAddress ph1src;
			InetSocketAddress ph1dst;
			
			if (dump.mSide == INITIATOR) {
				ph1src = dump.mLocal;
				ph1dst = dump.mRemote;
			} else {
				ph1src = dump.mRemote;
				ph1dst = dump.mLocal;
			}
			
			Event evt = new Event(Command.ADMIN_PROTO_ISAKMP, -1, Event.EVT_PHASE1_UP,
					dump.mTimeCreated, ph1src, ph1dst, -1);
			mListener.onCommand(evt);
		}
	}

	public void vpnConnect(String gw) {
		Message msg = mWorkerHandler.obtainMessage(HANDLER_VPN_CONNECT);
		msg.obj = gw;
		msg.sendToTarget();
	}
	
	protected void onVpnConnect(String gw) {
		try {
			InetAddress addr = InetAddress.getByName(gw);
			mAdminCmd.vpnConnect(addr);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void vpnDisconnect(String gw) {
		Message msg = mWorkerHandler.obtainMessage(HANDLER_VPN_DISCONNECT);
		msg.obj = gw;
		msg.sendToTarget();		
	}
	
	public void onVpnDisconnect(String gw) {
		try {
			InetAddress addr = InetAddress.getByName(gw);
			mAdminCmd.vpnDisconnect(addr);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
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

    private void startRacoon() {
		Process process = null;
		
		try {
			Log.i("LocalService", "Start process");
			File binDir = this.getDir("bin", 0);
			
			// Kill old racoon instances
			NativeCommand.system(new File(binDir, "killracoon.sh").getAbsolutePath() + " all");
			
			// Remove racoon socket since we need to
			// detect when the socket is created
    		File socketFile = new File(mSocketPath);
			if (socketFile.exists())
				socketFile.delete();
			
			// Remove racoon pid file
    		File pidFile = new File(binDir, ConfigManager.PIDFILE);
			if (pidFile.exists())
				pidFile.delete();

			// TODO check getExternalStorageState()
			File ipsecDir = new File(Environment.getExternalStorageDirectory(), "ipsec");
			process = new ProcessBuilder()
    		.command(new File(binDir, "racoon.sh").getAbsolutePath(),
    				"-v", "-d",
    				"-f", new File(binDir, ConfigManager.PEERS_CONFIG).getAbsolutePath(),
    				"-l", new File(ipsecDir, "racoon.log").getAbsolutePath())
    		.redirectErrorStream(true)
    		.start();

    		InputStream in = process.getInputStream();
    		OutputStream out = process.getOutputStream();
    		
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(in), 8192);
    		int read;
    		char[] buffer = new char[4096];
    		while ((read = reader.read(buffer)) > 0) {
    		}
    		reader.close();
    		in.close();
    		out.close();

    		// Wait for racoon to create local unix domain socket and pid file
    		for (int i=0; i < 10; i++) {
    			if (socketFile.exists() && pidFile.exists())
    				break;
    			try {
    				Thread.sleep(500);
    			} catch (InterruptedException e) {
    				break;
    			}
    		}
    		
    		// Read PID
    		BufferedReader pidReader =
    				new BufferedReader(new FileReader(pidFile), 128);
    		String pidStr = pidReader.readLine();
    		pidReader.close();
    		if (pidStr == null)
    			throw new RuntimeException("Failed to start racoon");
    		mPid = Integer.parseInt(pidStr);
    		Log.i("LocalService", "racoon pid '" + mPid + "'");

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
    			onRacoonStarted();
    			break;
    		}
    	}
    };
    
    private Admin.OnCommandListener mListener = new Admin.OnCommandListener() {
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
					broadcastIntent.putExtra("remote_addr", evt.getPh1dst());
					sendBroadcast(broadcastIntent);
				}
			} else {
				Log.i("ipsec-tools", "Unhandled command " + cmd);						
			}
			
			Log.i("ipsec-tools", "Command received " + cmd);
		}
    };

    private void onRacoonStarted() {
    	try {
			mAdmin.setOnCommandListener(mListener);
			mAdmin.showEvt();
			
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(ACTION_SERVICE_READY);
			sendBroadcast(broadcastIntent);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }

}
