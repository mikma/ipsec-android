package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class ComSocket {
	private LocalSocket mSocket;
	private OutputStream mOs;
	
	public ComSocket(final String adminsock_path) throws IOException {
		mSocket = new LocalSocket();
		mSocket.connect(new LocalSocketAddress(adminsock_path,
				LocalSocketAddress.Namespace.FILESYSTEM));
		mOs = mSocket.getOutputStream();
	}
	

	public void send(ByteBuffer bb) throws IOException {
		Log.i("ipsec-android", "req: " + bb);
		mOs.write(bb.array());
	}
	
	public Command receive() throws IOException {
		return null;
	}
	
	public void close() throws IOException {
		mOs.close();
		mSocket.close();
	}
}
