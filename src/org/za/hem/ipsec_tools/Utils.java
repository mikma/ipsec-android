package org.za.hem.ipsec_tools;

import java.lang.StringBuffer;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Utils {
	public static final int ISAKMP_PORT = 500;

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
		if (dstAddr == null)
			return null;
		try {
			DatagramSocket sock = new DatagramSocket();
			sock.connect(dstAddr, ISAKMP_PORT);
			InetAddress srcAddr = sock.getLocalAddress();
			sock.close();
			return srcAddr;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
}
