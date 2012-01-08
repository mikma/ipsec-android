package org.za.hem.ipsec_tools.peer;


public interface OnPeerChangeListener {
	public abstract void onDeletePeer(Peer peer);
	public abstract void onCreatePeer(Peer peer);
}
