package org.za.hem.ipsec_tools.racoon;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;

// FIXME change to list/array of dumps.
public class Ph1Dump extends Command {
	
	private ArrayList<Item> mItems;
	
	public class Item {
		int mStatus;
		int mSide;
		InetSocketAddress mRemote;
		InetSocketAddress mLocal;
		short mVersion;
		short mEType;
		long mTimeCreated;
		int mPh2Cnt;

		public Item(
				// index
				int status, int side,
				InetSocketAddress remote,
				InetSocketAddress local,
				short version,
				short eType,
				long timeCreated, // TODO change to time?
				int ph2Cnt) {
			mStatus = status;
			mSide = side;
			mRemote = remote;
			mLocal = local;
			mVersion = version;
			mEType = eType;
			mTimeCreated = timeCreated;
			mPh2Cnt = ph2Cnt;
		}
	}
	
	protected Ph1Dump(int len) {
		super(ADMIN_SHOW_SA, ADMIN_PROTO_ISAKMP, len);
		mItems = new ArrayList<Item>();
	}
	
	public AbstractCollection<Item> getItems() { return mItems; }

	protected static Ph1Dump create(int len, byte[] data) {
		ByteBuffer bb = wrap(data);
		Ph1Dump pd = new Ph1Dump(len);
		
		int count = len / (16 + 4 + 4 + 128 + 128 + 2 + 2 + 4 + 4);
		
		for (int i = 0; i<count; i++) {
			// FIXME use cookie
			byte[] cookie = new byte[16];
			// TODO read i_ck u_char cookie_t[8]
			bb.get(cookie, 0, 8);
			// TODO read r_ck u_char cookie_t[8]
			bb.get(cookie, 8, 8);
			int status = bb.getInt();
			int side = bb.getInt();
			InetSocketAddress remote = getSocketAddressStorage(bb);
			InetSocketAddress local = getSocketAddressStorage(bb);
			short version = getUnsignedByte(bb);
			short eType = getUnsignedByte(bb);
			long timeCreated = bb.getInt();
			int ph2Cnt = bb.getInt();
			
			pd.mItems.add(pd.new Item(status, side, remote, local,
				version, eType, timeCreated, ph2Cnt));
		}
		return pd;
	}
}
