package org.za.hem.ipsec_tools;

import java.lang.StringBuffer;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Utils {

	public static <T> String join(T[] array, String delimiter) {
	    if (array.length == 0)
	    	return "";
	    int i = 0;
	    StringBuffer buffer = new StringBuffer(array[i++].toString());
	    for (; i < array.length; i++) {
	    	buffer.append(delimiter).append(array[i++]);
	    }
	    return buffer.toString();
	}
	
	public static InetAddress getLocalAddress(InetAddress dstAddr) {
		try {
			DatagramSocket sock = new DatagramSocket();
			// TODO hardcoded
			sock.connect(dstAddr, 500);
			InetAddress srcAddr = sock.getLocalAddress();
			sock.close();
			return srcAddr;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
}
