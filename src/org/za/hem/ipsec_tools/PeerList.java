package org.za.hem.ipsec_tools;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import org.za.hem.ipsec_tools.service.NativeService;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;

public class PeerList extends ArrayList<Peer> {

	/**
	 * Serial for Serializable 
	 */
	private static final long serialVersionUID = -3584858864706289236L;
	
	private ArrayList<Peer> mPeers;
	private OnPeerChangeListener mListener;
	
	public PeerList(int capacity) {
		super(capacity);
		mPeers = this;
	}
	
	protected Peer get(PeerID id) {
		return mPeers.get(id.intValue());
	}
	
	protected void set(PeerID id, Peer peer) {
		mPeers.set(id.intValue(), peer);
	}
	
	protected void setOnPeerChangeListener(OnPeerChangeListener listener) {
		mListener = listener;
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
	
    protected PeerID createPeer(Context context)
    {
        int empty = mPeers.indexOf(null);
        if (empty == -1) {
        	empty = mPeers.size();
        	Log.i("ipsec-tools", "Size " + mPeers.size());
        }
        mPeers.ensureCapacity(empty+1);

    	Log.i("ipsec-tools", "New id " + empty);
        PeerID newId = new PeerID(empty);
    	Peer peer = new Peer(context, newId, null);
    	if (empty >= mPeers.size())
    		mPeers.add(peer);
    	else
    		mPeers.set(empty, peer);
        
        if (mListener != null)
        	mListener.onCreatePeer(peer);
        
        return newId;
    }
    	
    protected void deletePeer(final PeerID id, Context context) {
    	final Peer peer = get(id);
    	
		Log.i("ipsec-tools", "deletePeer");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.title_delete_peer);
		String msgFormat = context.getResources().getString(R.string.msg_delete_peer);  
		String msg = String.format(msgFormat, peer.getName());
		builder.setMessage(msg);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				// do something when the OK button is clicked
				if (mListener != null)
					mListener.onDeletePeer(peer);

				peer.clear();
				set(id, null);
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				  // do something when the Cancel button is clicked
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
		Log.i("ipsec-tools", "After show");
    }
    

	
    protected void edit(Context context, final PeerID id) {
    	Peer peer = mPeers.get(id.intValue());
       	if (peer.getStatus() == Peer.STATUS_CONNECTED) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(context);
    		builder.setIcon(android.R.drawable.ic_dialog_alert);
    		builder.setTitle(peer.getName());
    		builder.setMessage(R.string.msg_disconnect_first);
    		builder.setPositiveButton(android.R.string.ok, null);
    		AlertDialog alert = builder.create();
    		alert.show();
    		return;
    	}

        Intent settingsActivity = new Intent(context,
                PeerPreferences.class);
        settingsActivity.putExtra(PeerPreferences.EXTRA_ID, id.intValue());
        context.startActivity(settingsActivity);
    }
    
    protected void connect(final PeerID id, NativeService mBoundService) {
    	if (mBoundService == null)
    		return;
    	
    	Peer peer = get(id);
    	InetAddress addr = peer.getRemoteAddr();
    	if (addr == null)
    		// FIXME error message
    		return;
    	Log.i("ipsec-tools", "connectPeer " + addr);
    	peer.setStatus(Peer.STATUS_PROGRESS);

/*    	ConfigManager cm = new ConfigManager(this);
        try {
			cm.build(mPeers);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}*/
   		mBoundService.vpnConnect(addr.getHostAddress());   	
    }
    
    protected void disconnect(final PeerID id, NativeService mBoundService) {
    	if (mBoundService == null) {
    		Log.i("ipsec-tools", "No service");
    		return;
    	}
    	
    	Peer peer = get(id);
    	InetAddress addr = peer.getRemoteAddr();
    	if (addr == null)
    		// FIXME error message
    		return;
    	Log.i("ipsec-tools", "disconnectPeer " + addr);
    	peer.setStatus(Peer.STATUS_PROGRESS);
    	mBoundService.vpnDisconnect(addr.getHostAddress());
    }
    
    protected void toggle(final PeerID id, NativeService mBoundService) {
    	Peer peer = get(id);
    	Log.i("ipsec-tools", "togglePeer " + id + " " + peer);
    	if (peer.getStatus() == Peer.STATUS_CONNECTED)
    		disconnect(id, mBoundService);
    	else
    		connect(id, mBoundService);
    }
}
