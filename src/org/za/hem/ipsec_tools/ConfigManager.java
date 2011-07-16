package org.za.hem.ipsec_tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
	
	public static final String PATTERN = "\\$\\{([a-zA-Z0-9_]+)\\}";
	private Pattern mPat;
	private Map<String,String> mVariables;
	
	public ConfigManager() {
		mVariables = new HashMap<String,String>();
		mVariables.put("bindir", "/data/data/org.za.hem.ipsec_tools/app_bin");
		mVariables.put("extdir", "/sdcard");
		mVariables.put("local_addr", "192.168.1.17");
		mPat = Pattern.compile(PATTERN);
	}
	
	public void AddVariable(String key, String value) {
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
			Writer os = new FileWriter(input);
	
			try {
				String line;
				while ( (line = is.readLine()) != null) {
					os.write(substituteLine(line));
				}				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				is.close();
				os.close();
			}
		} catch (IOException e) {
			// Ignore error on close
		}
	}
}
