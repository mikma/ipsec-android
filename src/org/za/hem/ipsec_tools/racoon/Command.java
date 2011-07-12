package org.za.hem.ipsec_tools.racoon;

import java.net.InetSocketAddress;
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

	public static ByteBuffer buildShowEvt() {
		return buildHeader(ADMIN_SHOW_EVT, 0, 0);					
	}
	
	public static ByteBuffer buildEstablishSA(
			InetSocketAddress src,
			InetSocketAddress dst) {
		
		// FIXME INET6
		short prefixLen = 32;
		
		ByteBuffer index = buildComIndexes(
				src, prefixLen,
				dst, prefixLen,
				(short)0);
		Log.i("ipsec-android", "index: " + index);
		int indexLen = index.position();
		index.rewind();

		ByteBuffer header = buildHeader(ADMIN_ESTABLISH_SA,
										ADMIN_PROTO_ISAKMP,
										indexLen);
		Log.i("ipsec-android", "header: " + header);
		
		header.put(index);
		Log.i("ipsec-android", "header: " + header);
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
	
	protected static ByteBuffer allocate(int len) {
		ByteBuffer bb = ByteBuffer.allocate(len);
		bb.order(ByteOrder.nativeOrder());
		return bb;
	}
	
	protected static void putUnsignedByte(ByteBuffer bb, short value) {
		bb.put((byte)(value & 0xff));
	}

	protected static void putUnsignedShort(ByteBuffer bb, int value) {
		bb.putShort((short)(value & 0xffff));
	}

	protected static void putUnsignedInt(ByteBuffer bb, long value) {
		bb.putInt((int)(value & 0xffffffff));
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
			Log.i("ipsec-android", "begin addr: " + bb);
			bb.put(addr);
			Log.i("ipsec-android", "end addr: " + bb);
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
			short prefs,
			InetSocketAddress dst,
			short prefd,
			short upperLayerProto) {
		ByteBuffer bb = allocate(4 + 128 + 128);
		
		putUnsignedByte(bb, prefs);
		putUnsignedByte(bb, prefd);
		putUnsignedByte(bb, upperLayerProto);
		putUnsignedByte(bb, (short)0); // reserved
		putSocketAddressStorage(bb, src);
		putSocketAddressStorage(bb, dst);
		return bb;
	}
}
