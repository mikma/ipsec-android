package org.za.hem.ipsec_tools.peer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import org.za.hem.ipsec_tools.R;
import org.za.hem.ipsec_tools.service.NativeService;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * Peer List Controller
 *
 * @author mikael
 *
 */
public class PeerList extends ArrayList<Peer> {

	public static final int HANDLER_VPN_CONNECT = 1;
	public static final int HANDLER_VPN_DISCONNECT = 2;
	public static final int HANDLER_DUMP_ISAKMP_SA = 3;
	
	/**
	 * Serial for Serializable 
	 */
	private static final long serialVersionUID = -3584858864706289236L;
	
	private ArrayList<Peer> mPeers;
	private OnPeerChangeListener mListener;
	private HandlerThread mHandlerThread;
	private Handler mHandler;
	private NativeService mBoundService;
	
	public PeerList(int capacity) {
		super(capacity);
		mBoundService = null;
		mPeers = this;
		mHandler = null;
		mHandlerThread = null;
	}
	
	private void startHandler() {
		mHandlerThread  = new HandlerThread("PeerList");
		mHandlerThread.start();
		mHandler = new Handler(mHandlerThread.getLooper()) {
			public void handleMessage(Message msg) {
				String addr;
				switch (msg.what) {
				case HANDLER_VPN_CONNECT:
			    	addr = (String)msg.obj;
			   		mBoundService.vpnConnect(addr);
					break;
				case HANDLER_VPN_DISCONNECT:
			    	addr = (String)msg.obj;
			   		mBoundService.vpnDisconnect(addr);
					break;
				case HANDLER_DUMP_ISAKMP_SA:
				    mBoundService.dumpIsakmpSA();
				    break;
				}	
			}
		};		
	}
	
	private void stopHandler() {
		mHandlerThread.quit();
		mHandlerThread = null;
		mHandler = null;
	}
	
	public void setService(NativeService service) {
		if (service == null)
			throw new NullPointerException();

		mBoundService = service;		
		if (mHandlerThread == null)
			startHandler();
	}
	
	public void clearService() {
		mBoundService = null;
	}
	
	public Peer get(PeerID id) {
		int i = id.intValue();
		return mPeers.get(i);
	}
	
	protected void set(PeerID id, Peer peer) {
		mPeers.set(id.intValue(), peer);
	}
	
	public void setOnPeerChangeListener(OnPeerChangeListener listener) {
		mListener = listener;
	}

	public Peer findForRemote(final InetSocketAddress sa) {
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
	
    public PeerID createPeer(Context context)
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
    	
    public void deletePeer(final PeerID id, Context context) {
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
    

	
    public void edit(Context context, final PeerID id) {
    	Peer peer = mPeers.get(id.intValue());
       	if (peer.isConnected()) {
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

    public void dumpIsakmpSA() {
    	Message msg = mHandler.obtainMessage(HANDLER_DUMP_ISAKMP_SA);
    	msg.sendToTarget();
    }
    
    public void connect(final PeerID id) {
    	if (mBoundService == null)
    		return;
    	
    	Peer peer = get(id);
    	InetAddress addr = peer.getRemoteAddr();
    	if (addr == null)
    		throw new NullPointerException();
    	Log.i("ipsec-tools", "connectPeer " + addr);
    	peer.onConnect();

    	Message msg = mHandler.obtainMessage(HANDLER_VPN_CONNECT);
    	msg.obj = addr.getHostAddress();
    	msg.sendToTarget();
    }
    
    public void disconnect(final PeerID id) {
    	if (mBoundService == null) {
    		Log.i("ipsec-tools", "No service");
    		return;
    	}
    	
    	Peer peer = get(id);
    	InetAddress addr = peer.getRemoteAddr();
    	if (addr == null)
    		throw new NullPointerException();
    	Log.i("ipsec-tools", "disconnectPeer " + addr);
    	peer.onDisconnect();
    	Message msg = mHandler.obtainMessage(HANDLER_VPN_DISCONNECT);
    	msg.obj = addr.getHostAddress();
    	msg.sendToTarget();
    }
    
    public void toggle(final PeerID id) {
    	Peer peer = get(id);
    	Log.i("ipsec-tools", "togglePeer " + id + " " + peer);
    	if (peer.canDisconnect())
    		disconnect(id);
    	else if (peer.canConnect())
    		connect(id);
    }
}
