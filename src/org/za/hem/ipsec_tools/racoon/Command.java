package org.za.hem.ipsec_tools.racoon;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class Command {
	/*
	 * Version field in request is valid.
	 */
	public static final int ADMIN_FLAG_VERSION	= 0x8000;
	public static final int ADMIN_FLAG_LONG_REPLY	= 0x8000;

	/*
	 * No data follows as the data.
	 * These don't use proto field.
	 */
	public static final int ADMIN_RELOAD_CONF	= 0x0001;
	public static final int ADMIN_SHOW_SCHED	= 0x0002;
	public static final int ADMIN_SHOW_EVT		= 0x0003;

	/*
	 * No data follows as the data.
	 * These use proto field.
	 */
	public static final int ADMIN_SHOW_SA		= 0x0101;
	public static final int ADMIN_FLUSH_SA		= 0x0102;

	/*
	 * The admin_com_indexes follows, see below.
	 */
	public static final int ADMIN_DELETE_SA		= 0x0201;
	public static final int ADMIN_ESTABLISH_SA	= 0x0202;
	public static final int ADMIN_DELETE_ALL_SA_DST	= 0x0204;	/* All SA for a given peer */

	public static final int ADMIN_GET_SA_CERT	= 0x0206;

	/*
	 * The admin_com_indexes and admin_com_psk follow, see below.
	 */
	public static final int ADMIN_ESTABLISH_SA_PSK	= 0x0203;

	/*
	 * user login follows
	 */
	public static final int ADMIN_LOGOUT_USER	= 0x0205;  /* Delete SA for a given Xauth user */

	/*
	 * Range 0x08xx is reserved for privilege separation, see privsep.h 
	 */

	/* the value of proto */
	public static final int ADMIN_PROTO_ISAKMP	= 0x01ff;
	public static final int ADMIN_PROTO_IPSEC	= 0x02ff;
	public static final int ADMIN_PROTO_AH		= 0x0201;
	public static final int ADMIN_PROTO_ESP		= 0x0202;
	public static final int ADMIN_PROTO_INTERNAL	= 0x0301;
	
	public static final int AF_INET = 2;
	public static final int AF_INET6 = 10;
	
	public static final int HEADER_LEN = 8;
	
	private int mLen;
	private int mCmd;
	private short mErrno;
	private int mProto;

	public int getLen() {
		return mLen;
	}

	public int getCmd() {
		return mCmd;
	}

	public short getErrno() {
		return mErrno;
	}

	public int getProto() {
		return mProto;
	}
	
	protected Command(int cmd, int proto, int len) {
		mCmd = cmd;
		mProto = proto;
		mLen = len;
	}

	public static Command receive(InputStream is) throws IOException {
		int res;
		byte[] buf = new byte[HEADER_LEN];
		
		res = is.read(buf, 0, HEADER_LEN);
		if (res < 0)
			// TODO handle graceful, stream closed by Admin.
			throw new IOException("Bad data");
		if (res != HEADER_LEN)
			throw new IOException("Expected " + HEADER_LEN + " bytes, got " + res);
		
		ByteBuffer bb = wrap(buf);
		Log.i("ipsec-tools", "receive: " + bb);

		int lenLow = getUnsignedShort(bb);
		int cmd = getUnsignedShort(bb);
		int pos = bb.position();
		short errno = bb.getShort(pos);
		int lenHigh = getUnsignedShort(bb);
		int proto = getUnsignedShort(bb);
		int len;
		
		if (errno != 0 && (cmd & ADMIN_FLAG_LONG_REPLY) == 0) {
			Command c = new Command(cmd, proto, lenLow);
			c.mErrno = errno;
			return c;
		}

		if ((cmd & ADMIN_FLAG_LONG_REPLY) != 0) {
			len = lenLow + (lenHigh << 16);
		} else {
			len = lenLow;
		}
		
		final int dataLen = len - HEADER_LEN; 
		byte[] dataBuf = new byte[dataLen];
		
		int p = 0;
		while (p < dataLen) {
			Log.i("ipsec-tools", "Read dataLen:" + dataLen + " p:" + p);
			
			if ((res = is.read(dataBuf, p, dataLen - p)) < 0) {
				throw new RuntimeException("read");
			}
			
			p = p + res;
		}
		
		int c = cmd & ~ADMIN_FLAG_LONG_REPLY;
		Log.i("ipsec-tools", "Command: " + c);
		
		switch (cmd & ~ADMIN_FLAG_LONG_REPLY) {
		case ADMIN_SHOW_SCHED:
			return null;
		case ADMIN_SHOW_EVT:
			if (len == HEADER_LEN)
				return null;
			else
				return Event.create(proto, len, dataBuf);
		case ADMIN_GET_SA_CERT:
			return null;
		case ADMIN_SHOW_SA:
			switch (proto) {
			case ADMIN_PROTO_ISAKMP:
				return Ph1Dump.create(len, dataBuf);
			default:
				return null;
			}
		default:
			return new Command(cmd, proto, len);
		}
	}
	
	public static ByteBuffer buildShowEvt() {
		return buildHeader(ADMIN_SHOW_EVT, 0, 0);					
	}

	public static ByteBuffer buildVpnConnect(InetAddress vpnGateway) {
		InetAddress srcAddr = getLocalAddress(vpnGateway);
		InetSocketAddress dst = new InetSocketAddress(vpnGateway, 0);
		InetSocketAddress src = new InetSocketAddress(srcAddr, 0);
		Log.i("ipsec-tools", "vpn-connect " + src + "->" + dst);
	
		int prefixLenSrc = src.getAddress().getAddress().length * 8;
		int prefixLenDst = dst.getAddress().getAddress().length * 8;
		
		return buildEstablishSA(
				ADMIN_PROTO_ISAKMP,
				src, prefixLenSrc,
				dst, prefixLenDst);
	}
	
	public static ByteBuffer buildEstablishSA(
			int proto,
			InetSocketAddress src, int prefixLenSrc,
			InetSocketAddress dst, int prefixLenDst) {
		
		ByteBuffer index = buildComIndexes(
				src, prefixLenSrc,
				dst, prefixLenDst,
				0);
		int indexLen = index.position();
		index.rewind();

		ByteBuffer header = buildHeader(ADMIN_ESTABLISH_SA,
										proto,
										indexLen);
		
		header.put(index);
		return header;
	}
	
	/**
	 * Delete all security association to destination.
	 */
	public static ByteBuffer buildVpnDisconnect(
			InetAddress vpnGateway) {
		
		int prefixLen = vpnGateway.getAddress().length * 8;
		InetAddress anyAddr;
				
		try {
			if (prefixLen == 32) { 
				anyAddr = InetAddress.getByName("0.0.0.0");
			} else if (prefixLen == 128){
				anyAddr = InetAddress.getByName("::");
			} else {
				throw new RuntimeException("Invalid destination address length.");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		
		InetSocketAddress src = new InetSocketAddress(anyAddr, 0);
		InetSocketAddress dst = new InetSocketAddress(vpnGateway, 0);
		return buildDeleteAllSADst(
				ADMIN_PROTO_ISAKMP,
				src, prefixLen, dst, prefixLen);
	}

	/**
	 * Delete all security association to destination.
	 */
	public static ByteBuffer buildDeleteAllSADst(
			int proto,
			InetSocketAddress src, int prefixLenSrc,
			InetSocketAddress dst, int prefixLenDst) {
		
		ByteBuffer index = buildComIndexes(
				src, prefixLenSrc,
				dst, prefixLenDst,
				0);
		int indexLen = index.position();
		index.rewind();

		ByteBuffer header = buildHeader(ADMIN_DELETE_ALL_SA_DST,
										proto,
										indexLen);
		
		header.put(index);
		return header;
	}

	public static ByteBuffer buildDumpIsakmpSA() {
		ByteBuffer header = buildHeader(ADMIN_SHOW_SA,
										ADMIN_PROTO_ISAKMP,
										0);
		return header;
	}

	protected static ByteBuffer buildHeader(int cmd,
											int proto, int len) {
		int totalLen = len + 8;
		ByteBuffer bb = allocate(totalLen);
		putUnsignedShort(bb, totalLen);
		putUnsignedShort(bb, ADMIN_FLAG_VERSION | cmd);
		putUnsignedShort(bb, 1);
		putUnsignedShort(bb, proto);
		return bb;
	}
	
	// TODO move to net utilities?
	public static InetAddress getLocalAddress(InetAddress dstAddr) {
		try {
			DatagramSocket sock = new DatagramSocket();
			sock.connect(dstAddr, 500);
			InetAddress srcAddr = sock.getLocalAddress();
			sock.close();
			return srcAddr;
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected static ByteBuffer allocate(int len) {
		ByteBuffer bb = ByteBuffer.allocate(len);
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}
	
	protected static ByteBuffer wrap(byte[] b) {
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}
	
	protected static short getUnsignedByte(ByteBuffer bb) {
		return ((short)(bb.get() & 0xff));
	}

	protected static void putUnsignedByte(ByteBuffer bb, short value) {
		bb.put((byte)(value & 0xff));
	}

	protected static int getUnsignedShort(ByteBuffer bb) {
		return ((int)bb.getShort() & 0xffff);
	}
	
	protected static void putUnsignedShort(ByteBuffer bb, int value) {
		bb.putShort((short)(value & 0xffff));
	}

	protected static long getUnsignedInt(ByteBuffer bb) {
		return ((long)bb.getInt() & 0xffffffffL);
	}
	
	protected static long getUnsignedInt(ByteBuffer bb, int pos) {
		return ((long)bb.getInt(pos) & 0xffffffffL);
	}

	protected static void putUnsignedInt(ByteBuffer bb, long value) {
		bb.putInt((int)(value & 0xffffffffL));
	}

	protected static InetSocketAddress getSocketAddressStorage(ByteBuffer bb) {
		ByteOrder tmp = bb.order();
		int pos = bb.position();
		int family = getUnsignedShort(bb);
		bb.order(ByteOrder.BIG_ENDIAN);
		int port = getUnsignedShort(bb);
		byte[] addr;
		
		switch (family) {
		case AF_INET:
			addr = new byte[4];
			long intAddr = getUnsignedInt(bb, bb.position());
			Log.i("ipsec-tools", "sa " + family + " " + port + " " + intAddr);
			bb.get(addr);
			break;
		case AF_INET6:
			getUnsignedInt(bb); // Should be 0
			addr = new byte[16];
			bb.get(addr);
			getUnsignedInt(bb); // Should be 0
			break;
		default:
			bb.order(tmp);
			throw new RuntimeException("Unknown address family: " + family);
		}
		
		bb.position(pos + 128);
		bb.order(tmp);
		
		try {
			return new InetSocketAddress(InetAddress.getByAddress(addr), port);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static void putSocketAddressStorage(ByteBuffer bb,
			InetSocketAddress sa) {
		byte[] addr = sa.getAddress().getAddress();
		
		ByteOrder tmp = bb.order();
		int pos = bb.position();
		
		if (addr.length == 4) {
			putUnsignedShort(bb, AF_INET);
			bb.order(ByteOrder.BIG_ENDIAN);
			putUnsignedShort(bb, sa.getPort());
			bb.put(addr);
		} else if (addr.length == 16){
			putUnsignedShort(bb, AF_INET6);
			bb.order(ByteOrder.BIG_ENDIAN);
			putUnsignedShort(bb, sa.getPort());
			putUnsignedInt(bb, 0);
			bb.put(addr);
			putUnsignedInt(bb, 0);
		} else {
			throw new RuntimeException("Invalid length: " + addr.length);
		}
		
		bb.position(pos + 128);
		bb.order(tmp);
	}

	protected static ByteBuffer buildComIndexes(
			InetSocketAddress src,
			int srcPrefixLen,
			InetSocketAddress dst,
			int dstPrefixLen,
			int upperLayerProto) {
		ByteBuffer bb = allocate(4 + 128 + 128);
		
		putUnsignedByte(bb, (short)srcPrefixLen);
		putUnsignedByte(bb, (short)dstPrefixLen);
		putUnsignedByte(bb, (short)upperLayerProto);
		putUnsignedByte(bb, (short)0); // reserved
		putSocketAddressStorage(bb, src);
		putSocketAddressStorage(bb, dst);
		return bb;
	}
	
	public String toString() {
		return "Command: " + mCmd + " " + mProto + " " + mErrno + " " + mLen;
	}
}
