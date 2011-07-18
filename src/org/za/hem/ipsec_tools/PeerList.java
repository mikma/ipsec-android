package org.za.hem.ipsec_tools;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

public class PeerList extends ArrayList<Peer> {

	/**
	 * Serial for Serializable 
	 */
	private static final long serialVersionUID = -3584858864706289236L;
	
	public PeerList(int capacity) {
		super(capacity);
	}

	protected Peer findForRemote(final InetSocketAddress sa) {
    	InetAddress addr = sa.getAddress();
    	Iterator<Peer> iter = iterator();
    	
    	while (iter.hasNext()) {
    		Peer peer = iter.next();
    		if (peer == null)
    			continue;
    		if (!peer.isEnabled())
    			continue;
			InetAddress peerAddr = peer.getRemoteAddr();
    		if (peerAddr != null && peerAddr.equals(addr))
    			return peer;
    	}

    	return null;
    }
}
