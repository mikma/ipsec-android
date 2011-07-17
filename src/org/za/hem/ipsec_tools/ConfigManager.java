package org.za.hem.ipsec_tools;

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
import java.net.UnknownHostException;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

public class ConfigManager {
	
	public static final String PATTERN = "\\$\\{([a-zA-Z0-9_]+)\\}";
	private Pattern mPat;
	private Map<String,String> mVariables;
	private Context mContext;
	
	public ConfigManager(Context context) {
		mVariables = new HashMap<String,String>();
		//mVariables.put("bindir", "/data/data/org.za.hem.ipsec_tools/app_bin");
		//mVariables.put("extdir", "/sdcard");
		//mVariables.put("local_addr", "192.168.1.17");
		mPat = Pattern.compile(PATTERN);
		mContext = context;
	}
	
	protected void buildPeerConfig(Peer peer, Writer os) {
		// FIXME don't hardcode directory
		try {
			mVariables.put("remote_addr", InetAddress.getByName(peer.getRemoteAddr()).getHostAddress());
		} catch (UnknownHostException e) {
			Log.i("ipsec-tools", e.toString());
		}
		mVariables.put("local_addr", peer.getLocalAddr().getHostAddress());
		File input = peer.getTemplateFile();
		if (input == null)
			return;
		substitute(input, os);
	}
	
	protected File buildPeerConfig(Peer peer) throws IOException {
 		mVariables.put("local_addr", peer.getLocalAddr().getHostAddress());
		File output = new File("/data/data/org.za.hem.ipsec_tools/app_bin/" + peer.getPeerID().key + ".conf");
		FileWriter os = new FileWriter(output);
		buildPeerConfig(peer, os);
		os.close();
		return output;
	}
		
	public void build(AbstractCollection<Peer> peers) throws IOException {
		Iterator<Peer> iter = peers.iterator();
		// FIXME don't hardcode directory
		Writer out = new FileWriter("/data/data/org.za.hem.ipsec_tools/app_bin/peers.conf");
		Reader inHead = new InputStreamReader(mContext.getAssets().open("racoon.head"));
		substitute(inHead, out);
		while (iter.hasNext()) {
			Peer peer = iter.next();
			if (peer == null)
				continue;
			if (!peer.isEnabled())
				continue;
			mVariables.remove("remote_addr");
			mVariables.remove("local_addr");
			try {
				File output = buildPeerConfig(peer);
				out.write("include \"" + output.getAbsolutePath() + "\";");
			} catch (IOException e){
			}
		}
		out.close();
		// build peers.conf
	}
	
	public void addVariable(String key, String value) {
		mVariables.put(key, value);
	}

	protected String substituteLine(String line) {
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
	
	public void substitute(Reader input, Writer os) {
		BufferedReader is = new BufferedReader(input);
			
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

	public void substitute(File input, Writer os) {
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
