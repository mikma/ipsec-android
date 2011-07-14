package org.za.hem.ipsec_tools.racoon;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Admin {
	// FIXME
	public static final String SOCK_PATH = "/data/data/org.za.hem.ipsec_tools/app_bin/racoon.sock";
	public static final int MESSAGE_COMMAND = 2;
	
	private Thread mThread;
	private ComSocket mCom;
	private OnCommandListener mListener;
	
	public interface OnCommandListener {
		public abstract void onCommand(Command cmd);
	}
	
//		com.send(Command.buildShowEvt());
	
	public void start() throws IOException {
		if (mCom != null && mThread != null)
			return;
		
		if (mCom != null) {
			mCom.close();
			// FIXME remove socket file SOCK_PATH
		}
		
		// Wait for racoon to creat local unix domain socket
		File file = new File(SOCK_PATH);
		for (int i=0; i < 10; i++) {
			if (file.exists())
				break;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
		}
		
		mCom = new ComSocket(SOCK_PATH);
		
		mThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						Command cmd = mCom.receive();
						if (cmd != null)
							Message.obtain(mHandler, MESSAGE_COMMAND, cmd).sendToTarget();
					}
				} catch (IOException e) {
					//throw new RuntimeException(e);
				}
				Log.i("ipsec-tools", "Admin com socket EOF");
			}
			
		});
		mThread.start();
	}
	
	public void stop() throws IOException {
		if (mCom != null) {
			Log.i("ipsec-tools", "Stop admin");
			mCom.close();
			try {
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
	
	public void setOnCommandListener(OnCommandListener listener) {
		mListener = listener;
	}
	
	private Handler mHandler = new Handler() {		
		public void handleMessage(Message msg) {
			Command cmd = (Command)msg.obj;
			Log.i(this.getClass().toString(), "Handle command " + cmd);
			if (mListener != null)
				mListener.onCommand(cmd);
		}
	};
}
