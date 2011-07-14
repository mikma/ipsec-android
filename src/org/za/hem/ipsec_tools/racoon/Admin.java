package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import android.util.Log;

public class Admin {
	public static final String SOCK_PATH = "/tmp/racoon.sock";
	
	public void foo() throws IOException {
		ComSocket com = new ComSocket(SOCK_PATH);
		
//		com.send(Command.buildShowEvt());
		
		InetAddress dstAddr = InetAddress.getByName("gw.hem.za.org");
		DatagramSocket sock = new DatagramSocket();
		sock.connect(dstAddr, 4500);
		InetAddress srcAddr = sock.getLocalAddress();

		// vpn-connect <ip> == establish-sa isakpm inet <srcip> <dstip>
		InetSocketAddress dst = new InetSocketAddress(dstAddr, 0);
		//InetSocketAddress src = new InetSocketAddress("192.168.1.179", 0);
		InetSocketAddress src = new InetSocketAddress(srcAddr, 0);
		Log.i("ipsec-tools", "vpn-connect " + src + "->" + dst);
		com.send(Command.buildEstablishSA(src, dst));
		Command cmd = com.receive();
		handle(cmd);
		cmd = com.receive();
		handle(cmd);
		
		com.close();
	}
	
	protected void handle(Command cmd) {
		Log.i(this.getClass().toString(), "Handle command " + cmd);
	}
}
