package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class ComSocket {
	private String mAdminSockPath;
	private LocalSocket mSocket;
	private OutputStream mOs;
	private InputStream mIs;
	
	public ComSocket(final String adminsock_path) {
		mAdminSockPath = adminsock_path;
	}
	
	public void connect() throws IOException {
		mSocket = new LocalSocket();
		mSocket.connect(new LocalSocketAddress(mAdminSockPath,
				LocalSocketAddress.Namespace.FILESYSTEM));
		mOs = mSocket.getOutputStream();
		mIs = mSocket.getInputStream();		
	}
	

	public void send(ByteBuffer bb) throws IOException {
		Log.i("ipsec-tools", "send: " + bb);
		mOs.write(bb.array());
	}
	
	public Command receive() throws IOException {
		return Command.receive(mIs);
	}
	
	public void close() throws IOException {
		Log.i("ipsec-tools", "Close racoon com socket");
		mSocket.shutdownInput();
		mSocket.shutdownOutput();
		mSocket.close();
		mSocket = null;
		mOs.close();
		mIs.close();
	}
}
