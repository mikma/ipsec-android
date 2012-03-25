package org.za.hem.ipsec_tools.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.za.hem.ipsec_tools.peer.Peer;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

/**
 * Racoon and setkey config files builder.
 * 
 * @author mikael
 *
 */
public class ConfigManager {
	
	public static final String PATTERN = "\\$\\{([a-zA-Z0-9_]+)\\}";
	public static final String CONFIG_POSTFIX = ".conf";
	public static final String PEERS_CONFIG = "peers.conf";
	public static final String SETKEY_CONFIG = "setkey.conf";
	public static final String RACOON_HEAD = "racoon.head";
	public static final String SETKEY_HEAD = "setkey.head";
	public static final String PIDFILE = "racoon.pid";
	
	public enum Action {NONE, ADD, DELETE, UPDATE};
	
	// Variables usable in config files
	private static final String VAR_BINDIR = "bindir";
	private static final String VAR_EXTDIR = "extdir";
	private static final String VAR_REMOTE_ADDR = "remote_addr";
	private static final String VAR_LOCAL_ADDR = "local_addr";
	private static final String VAR_UID = "uid";
	private static final String VAR_GID = "gid";
	private static final String VAR_NAME = "name";
	private static final String VAR_ACTION = "action";
	

	private Pattern mPat;
	private Map<String,String> mVariables;
	private Context mContext;
	private File mBinDir;
	
	public ConfigManager(Context context) {
		mBinDir = context.getDir("bin", Context.MODE_PRIVATE);
		mVariables = new HashMap<String,String>();
		mVariables.put(VAR_BINDIR, mBinDir.getAbsolutePath());
		mVariables.put(VAR_EXTDIR, Environment.getExternalStorageDirectory().getAbsolutePath());
		mVariables.put(VAR_UID, "" + Process.myUid());
		mVariables.put(VAR_GID, "" + Process.myUid());
		mPat = Pattern.compile(PATTERN);
		mContext = context;
	}
	
	protected File getPeerConfigFile(Peer peer) {
		return new File(mBinDir, peer.getPeerID().toString() + CONFIG_POSTFIX);
	}
	
	/**
	 * Build racoon config for one peer and setkey portion
	 * @param peer
	 * @param racoonOs
	 * @param setkeyUpOs
	 * @param setkeyDownOs
	 * @throws IOException
	 */
	private void writePeerConfig(Action action, Peer peer, Writer racoonOs,
			Writer setkeyOs) throws IOException {
		InetAddress addr = peer.getRemoteAddr();
		if (addr != null)
			mVariables.put(VAR_REMOTE_ADDR, addr.getHostAddress());
		mVariables.put(VAR_LOCAL_ADDR, peer.getLocalAddr().getHostAddress());
		mVariables.put(VAR_NAME, peer.getName());
		mVariables.put(VAR_ACTION, actionToString(action));
		File tmpl = peer.getTemplateFile();
		if (tmpl == null)
			return;
		PolicyFile policy = new PolicyFile(tmpl);
		if (racoonOs != null)
			substitute(policy.getRacoonConfStream(), racoonOs);
		if (action != Action.NONE && setkeyOs != null)
			substitute(policy.getSetkeyConfStream(), setkeyOs);
	}
	
	private static String actionToString(Action action) {
		switch (action) {
		case NONE:
			return "none";
		case ADD:
			return "add";
		case DELETE:
			return "delete";
		case UPDATE:
			return "update";
		default:
			throw new RuntimeException("Unknown action: " + action);
		}
	}

	/**
	 * Build racoon config for one peer and setkey portion
	 * @param action
	 * @param peer
	 * @param setkeyOs
	 * @return racoon config file
	 * @throws IOException
	 */
	public File buildPeerConfig(Action action, Peer peer, Writer setkeyOs) throws IOException {
 		mVariables.put(VAR_LOCAL_ADDR, peer.getLocalAddr().getHostAddress());
		
 		File racoonFile = getPeerConfigFile(peer);
		FileWriter racoonOs = new FileWriter(racoonFile);
		
		writePeerConfig(action, peer, racoonOs, setkeyOs);
		racoonOs.close();
		return racoonFile;
	}
	
	/**
	 * 
	 * @param peers
	 * @param updateAllPeers
	 * @throws IOException
	 */
	public void build(AbstractCollection<Peer> peers,
			boolean addAllPeers) throws IOException {
		Iterator<Peer> iter = peers.iterator();
		Writer out = null;
		Writer setkeyOut = null;
		Reader inHead = null;
		Reader setkeyHead = null;

		try {
			out = new FileWriter(new File(mBinDir, PEERS_CONFIG));
			inHead = new InputStreamReader(mContext.getAssets().open(RACOON_HEAD));
			substitute(inHead, out);
			
			setkeyOut = new FileWriter(new File(mBinDir, SETKEY_CONFIG));
			setkeyHead = new InputStreamReader(mContext.getAssets().open(SETKEY_HEAD));
			substitute(setkeyHead, setkeyOut);
			
			while (iter.hasNext()) {
				Peer peer = iter.next();
				if (peer == null)
					continue;
				if (!peer.isEnabled())
					continue;
				mVariables.remove(VAR_REMOTE_ADDR);
				mVariables.remove(VAR_LOCAL_ADDR);
				mVariables.remove(VAR_NAME);
				try {
					File output;
					if (addAllPeers)
						output = buildPeerConfig(Action.ADD, peer, setkeyOut);
					else {
						writePeerConfig(Action.NONE, peer, null, setkeyOut);
						output = getPeerConfigFile(peer);
					}
					out.write("include \"" + output.getAbsolutePath() + "\";\n");
				} catch (IOException e){
				}
			}
		} finally {
			if (out != null)
				out.close();
			if (setkeyOut != null)
				setkeyOut.close();
			if (inHead != null)
				inHead.close();
			if (setkeyHead != null)
				setkeyHead.close();
		}
		// build peers.conf
	}
	
	public void addVariable(String key, String value) {
		mVariables.put(key, value);
	}

	private String substituteLine(String line) {
		StringBuffer buf = new StringBuffer();
		Matcher m = mPat.matcher(line);

		while (m.find()) {
			String var = m.group(1);
			String value;
			
			if (mVariables.containsKey(var)) {
				value = mVariables.get(var);
			} else {
				value = "";
			}

			m.appendReplacement(buf, value);
		}
		m.appendTail(buf);
		buf.append('\n');

		return buf.toString();
	}
	
	private void substitute(Reader input, Writer os) {
		BufferedReader is = new BufferedReader(input, 8192);
			
		try {
			String line;
			while ( (line = is.readLine()) != null) {
				os.write(substituteLine(line));
			}				
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	private void substitute(java.io.InputStream input, Writer os) {
		substitute(new InputStreamReader(input), os);
	}

	private void substitute(File input, Writer os) {
		Reader is = null;
		try {
			is = new FileReader(input);
			substitute(is, os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}
	}
}
