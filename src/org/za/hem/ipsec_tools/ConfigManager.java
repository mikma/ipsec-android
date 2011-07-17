package org.za.hem.ipsec_tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
	
	public static final String PATTERN = "\\$\\{([a-zA-Z0-9_]+)\\}";
	private Pattern mPat;
	private Map<String,String> mVariables;
	
	public ConfigManager() {
		mVariables = new HashMap<String,String>();
		//mVariables.put("bindir", "/data/data/org.za.hem.ipsec_tools/app_bin");
		//mVariables.put("extdir", "/sdcard");
		//mVariables.put("local_addr", "192.168.1.17");
		mPat = Pattern.compile(PATTERN);
	}
	
	protected File buildPeerConfig(Peer peer) {
		// FIXME don't hardcode directory
		mVariables.put("remote_addr", peer.getRemoteAddr());
		mVariables.put("local_addr", peer.getLocalAddr().getHostAddress());
		File input = peer.getTemplateFile();
		if (input == null)
			return null;
		File output = new File("/data/data/org.za.hem.ipsec_tools/app_bin/" + peer.getPeerID().key + ".conf");		
		substitute(input, output);
		return output;
	}

	public void build(AbstractCollection<Peer> peers) throws IOException {
		Iterator<Peer> iter = peers.iterator();
		// FIXME don't hardcode directory
		Writer out = new FileWriter("/data/data/org.za.hem.ipsec_tools/app_bin/peers.conf");
		while (iter.hasNext()) {
			Peer peer = iter.next();
			if (peer == null)
				continue;
			mVariables.remove("remote_addr");
			mVariables.remove("local_addr");
			File file = buildPeerConfig(peer);
			if (file != null)
				out.write("include \"" + file.getAbsolutePath() + "\";\n");
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
	
	/**
	 * @param args
	 */
	public void substitute(File input, File output) {

		try {
			BufferedReader is = new BufferedReader(new FileReader(input));
			Writer os = new FileWriter(output);
	
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
				is.close();
				os.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
