package org.za.hem.ipsec_tools;

import java.lang.Exception;

import android.util.Log;

class PeerID implements Comparable<PeerID> {
	public static final String PREFIX = "peer_";
	int id;
	String key;
	
	public static class KeyFormatException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7506071001428989998L;
	}
	
	public PeerID(int theId) {
		id = theId;
		key = PREFIX + id;
	}
	
	public PeerID() {
		id = -1;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof PeerID) {
			return ((PeerID)o).id == id;
		} else {
			return false;
		}
	}
	
	@Override
	public int compareTo(PeerID arg0) {
    	Log.i("IPsecToolsActivity", "Compare: " + arg0);

		return id - arg0.id;
	}

	public PeerID next() {
		return new PeerID(id + 1);
	}
	
	public boolean isValid() {
		return id >= 0;
	}
	
	public String toString() {
		return key;
	}
	
	public int intValue() {
		return id;
	}
	
	public static boolean isKey(String key) {
		return key.startsWith(PREFIX);
	}
	
	public static PeerID fromString(String key) throws KeyFormatException {
		if (!isKey(key))
			throw new KeyFormatException();
		
		String idStr = key.substring(PREFIX.length());
		try {
			int id = Integer.parseInt(idStr);
			return new PeerID(id);
		} catch(NumberFormatException e) {
			throw new KeyFormatException();
		}
	}

}
