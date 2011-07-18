package org.za.hem.ipsec_tools.racoon;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.za.hem.ipsec_tools.racoon.Ph1Dump.Item;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Admin {
	// FIXME
	public static final int MESSAGE_COMMAND = 2;
	
	private Thread mThread;
	private volatile ComSocket mCom;
	private OnCommandListener mListener;
	private volatile boolean isStopping;
	
	public Admin () {
		Log.i("ipsec-tools", "Admin ctor " + this);
		isStopping = false;
	}
	
	public interface OnCommandListener {
		public abstract void onCommand(Command cmd);
	}
	
//		com.send(Command.buildShowEvt());
	
	public void open(String socketPath) throws IOException {
		if (mCom != null)
			return;
		
		if (mCom != null) {
			mCom.close();
			// FIXME remove socket file SOCK_PATH
		}
		
		// Wait for racoon to creat local unix domain socket
		File file = new File(socketPath);
		for (int i=0; i < 10; i++) {
			if (file.exists())
				break;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		mCom = new ComSocket(socketPath);
		mCom.connect();
	}

	public void start() throws IOException {
		if (mThread != null)
			return;
		
		mThread = new Thread(new Runnable() {
			public void run() {
				int maxTries = 10;
				while (true) {
					try {
						// FIXME mCom can be null, synchronize!
						Log.i("ipsec-tools", "Before receive");
						Command cmd = mCom.receive();
						Log.i("ipsec-tools", "After receive");
						if (cmd != null)
							Message.obtain(mHandler, MESSAGE_COMMAND, cmd).sendToTarget();
					} catch (IOException e) {
						//throw new RuntimeException(e);
						Log.i("ipsec-tools", "Admin com socket EOF");
						if (--maxTries <= 0)
							throw new RuntimeException(e);
						try {
							Thread.sleep(500);
							mCom.close();
							mCom.connect();
						} catch (InterruptedException e2) {
							throw new RuntimeException(e2);
						} catch (IOException e2) {
						}
					}
				}
			}
			
		});
		mThread.start();
	}
	
	public void stop() throws IOException {
		if (mCom != null) {
			Log.i("ipsec-tools", "Stop admin");
			isStopping = true;
			mCom.close();
			try {
				if (mThread != null)
					mThread.join(1000);
			} catch (InterruptedException e) {
			}
			mCom = null;
			mThread = null;
		}
		/*File file = new File(SOCK_PATH);
		if (file.exists())
			file.delete();*/
	}
	
	// vpn-connect <ip> == establish-sa isakpm inet <srcip> <dstip>
	
	public void dumpIsakmpSA() throws IOException {
		ByteBuffer bb = Command.buildDumpIsakmpSA();
		Log.i("ipsec-tools", "dumpIsakmpSA " + bb + " " + mCom);
		mCom.send(bb);
	}
	
	public void vpnConnect(InetAddress vpnGateway) throws IOException {
		mCom.send(Command.buildVpnConnect(vpnGateway));
	}
	
	public void vpnDisconnect(InetAddress vpnGateway) throws IOException {
		Log.i("ipsec-tools", "vpn-disconnect " + vpnGateway);
		mCom.send(Command.buildVpnDisconnect(vpnGateway));		
	}
	
	public void showEvt() throws IOException {
		Log.i("ipsec-tools", "show-event");
		mCom.send(Command.buildShowEvt());
	}
	
	protected void onDumpIsakmpSA(Ph1Dump pd) {
		Iterator<Item> iter = pd.getItems().iterator();
		
		while (iter.hasNext()) {
			Item dump = iter.next();

			Log.i("ipsec-tools", "onDumpIsakmpSA " + dump.mRemote);
	
			if (mListener == null) {
				Log.i("ipsec-tools", "No listener " + this);			
				return;
			}
	
			// FIXME
			final int INITIATOR = 0;
			final int RESPONDER = 1; 
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
	
	public void setOnCommandListener(OnCommandListener listener) {
		Log.i("ipsec-tools", "setOnCommandListener " + this);
		mListener = listener;
	}
	
	private Handler mHandler = new Handler() {		
		public void handleMessage(Message msg) {
			Log.i("ipsec-tools", "handleMessage " + msg.obj);
			Command cmd = (Command)msg.obj;
			Log.i(this.getClass().toString(), "Handle command " + cmd);
			
			if (mListener != null) {
				mListener.onCommand(cmd);
			}

			if (cmd.getCmd() == Command.ADMIN_SHOW_SA &&
				cmd.getProto() == Command.ADMIN_PROTO_ISAKMP) {
				Ph1Dump ph1dump = (Ph1Dump)cmd;
				onDumpIsakmpSA(ph1dump);
			}
		}
	};
}
