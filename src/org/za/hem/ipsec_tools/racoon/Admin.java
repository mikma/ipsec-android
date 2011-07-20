package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class Admin {
	// FIXME
	public static final int MESSAGE_COMMAND = 2;
	
	private Thread mThread;
	private volatile ComSocket mCom;
	private ArrayBlockingQueue<Command> mQueue;
	private OnCommandListener mListener;
	private volatile boolean isStopping;
	private String mSocketPath;
	
	public Admin(String socketPath) {
		Log.i("ipsec-tools", "Admin ctor " + this);
		mSocketPath = socketPath;
		mCom = new ComSocket(mSocketPath);
		isStopping = false;
		mQueue = new ArrayBlockingQueue<Command>(1);
		mListener = new OnCommandListener() {
			public void onCommand(Command cmd) {
				Log.i("ipsec-tools", "onCommand " + cmd);
				mQueue.add(cmd);
			}
		};
	}
	
	public interface OnCommandListener {
		public abstract void onCommand(Command cmd);
	}
	
	protected void open() throws IOException {
		if (mCom != null) {
			if (mCom.isConnected())
				throw new IOException("Already connected.");
		}
		
		mCom.connect();
	}

	protected void start() throws IOException {
		if (mThread != null)
			return;
		
		isStopping = false;
		
		mThread = new Thread(new Runnable() {
			public void run() {
				while (!isStopping) {
					try {
						// FIXME mCom can be null, synchronize!
						Log.i("ipsec-tools", "Before receive " + Thread.currentThread());
						Command cmd = mCom.receive();
						Log.i("ipsec-tools", "After receive " + Thread.currentThread());

						if (cmd !=null ) {
							if (mListener != null) {
								mListener.onCommand(cmd);
							}
						}
					} catch (IOException e) {
						//throw new RuntimeException(e);
						Log.i("ipsec-tools", "Admin com socket EOF " + e);
						//mCom.close();
						return;
					}
				}
			}
			
		});
		mThread.start();
	}
	
	public void close() throws IOException {
		if (mCom.isConnected()) {
			Log.i("ipsec-tools", "Stop admin");
			isStopping = true;
			mCom.close();
			try {
				if (mThread != null)
					mThread.join(1000);
			} catch (InterruptedException e) {
			}
			mThread = null;
			Log.i("ipsec-tools", "Stopped admin");
		}
	}
	
	// vpn-connect <ip> == establish-sa isakpm inet <srcip> <dstip>
	
	protected Command call(ByteBuffer bb) throws IOException {
		open();
		mQueue.clear();
		start();
		mCom.send(bb);
		try {
			Command cmd = mQueue.poll(1000, TimeUnit.MILLISECONDS);
			Log.i("ipsec-tools", "call result: " + cmd);
			return cmd;
		} catch (InterruptedException e) {
			Log.i("ipsec-tools", "call interrupted");
			// Todo contruct timeout Command?
			return null;
		} finally {
			close();
		}
	}
	
	public Command dumpIsakmpSA() throws IOException {
		return call(Command.buildDumpIsakmpSA());
	}
	
	public void vpnConnect(InetAddress vpnGateway) throws IOException {
		call(Command.buildVpnConnect(vpnGateway));
	}
	
	public void vpnDisconnect(InetAddress vpnGateway) throws IOException {
		Log.i("ipsec-tools", "vpn-disconnect " + vpnGateway);
		call(Command.buildVpnDisconnect(vpnGateway));		
	}
	
	public void showEvt() throws IOException {
		Log.i("ipsec-tools", "show-event");
		open();
		start();
		mCom.send(Command.buildShowEvt());
	}
	
	public void setOnCommandListener(OnCommandListener listener) {
		Log.i("ipsec-tools", "setOnCommandListener " + this);
		mListener = listener;
	}
}
