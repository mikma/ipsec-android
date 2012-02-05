package org.za.hem.ipsec_tools.racoon;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Event extends Command {
	
	/* type */
	public static final int EVT_RACOON_QUIT			= 0x0001;

	public static final int EVT_PHASE1_UP			= 0x0100;
	public static final int EVT_PHASE1_DOWN			= 0x0101;
	public static final int EVT_PHASE1_NO_RESPONSE	= 0x0102;
	public static final int EVT_PHASE1_NO_PROPOSAL	= 0x0103;
	public static final int EVT_PHASE1_AUTH_FAILED	= 0x0104;
	public static final int EVT_PHASE1_DPD_TIMEOUT	= 0x0105;
	public static final int EVT_PHASE1_PEER_DELETED	= 0x0106;
	public static final int EVT_PHASE1_MODE_CFG		= 0x0107;
	public static final int EVT_PHASE1_XAUTH_SUCCESS= 0x0108;
	public static final int EVT_PHASE1_XAUTH_FAILED	= 0x0109;

	public static final int EVT_PHASE2_NO_PHASE1	= 0x0200;
	public static final int EVT_PHASE2_UP			= 0x0201;
	public static final int EVT_PHASE2_DOWN			= 0x0202;
	public static final int EVT_PHASE2_NO_RESPONSE	= 0x0203;

	private int mType;
	private long mTimeStamp;
	private InetSocketAddress mPh1src;
	private InetSocketAddress mPh1dst;
	private long mPh2MsgId;
	private boolean mSynthetic;
	
	public int getType() {
		return mType;
	}

	public long getTimeStamp() {
		return mTimeStamp;
	}

	public InetSocketAddress getPh1src() {
		return mPh1src;
	}

	public InetSocketAddress getPh1dst() {
		return mPh1dst;
	}

	public long getPh2MsgId() {
		return mPh2MsgId;
	}
	
	public boolean getSynthetic() {
		return mSynthetic;
	}
	
	public void setSynthetic(boolean synthetic) {
		mSynthetic = synthetic;
	}

	public Event(int proto, int len,
					int type, long timeStamp,
					InetSocketAddress ph1src, InetSocketAddress ph1dst, long ph2MsgId) {
		super(ADMIN_SHOW_EVT, proto, len);
		mType = type;
		mTimeStamp = timeStamp;
		mPh1src = ph1src;
		mPh1dst = ph1dst;
		mPh2MsgId = ph2MsgId;
		mSynthetic = false;
	}
	
	protected static Event create(int proto, int len, byte[] data) {
		ByteBuffer bb = wrap(data);
		
		int type = (int)getUnsignedInt(bb);
		long timeStamp = bb.getInt();
		InetSocketAddress ph1src = getSocketAddressStorage(bb);
		InetSocketAddress ph1dst = getSocketAddressStorage(bb);
		long ph2MsgId = getUnsignedInt(bb);

		return new Event(proto, len, type, timeStamp, ph1src, ph1dst, ph2MsgId);
	}

	public String toString() {
		return "Event: " + mType + " " + mTimeStamp + " " + mPh1src + " " + mPh1dst + " " + mPh2MsgId;
	}
}
