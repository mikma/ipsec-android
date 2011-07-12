package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.net.InetSocketAddress;

import android.util.Log;

public class Admin {
	public static final String SOCK_PATH = "/tmp/racoon.sock";
	
	public void foo() throws IOException {
		ComSocket com = new ComSocket(SOCK_PATH);
		
//		com.send(Command.buildShowEvt());

		// vpn-connect <ip> == establish-sa isakpm inet <srcip> <dstip>
		InetSocketAddress src = new InetSocketAddress("192.168.1.179", 0);
		InetSocketAddress dst = new InetSocketAddress("gw.hem.za.org", 0);
		com.send(Command.buildEstablishSA(src, dst));
		Command cmd = com.receive();
		handle(cmd);
		
		com.close();
	}
	
	protected void handle(Command cmd) {
		Log.i(this.getClass().toString(), "Handle command " + cmd);
	}
}
