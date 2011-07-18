package org.za.hem.ipsec_tools;

public interface OnPeerChangeListener {
	public abstract void onDeletePeer(Peer peer);
	public abstract void onCreatePeer(Peer peer);
}
