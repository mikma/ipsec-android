package org.za.hem.ipsec_tools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class NativeCommand {
	private File mBinDir;
	private File mSystemBin;
	private Context mContext;
	private File mBinGetProp;
	private File mBinSetProp;
	
	public NativeCommand(Context context) {
		mContext = context;
	    mBinDir = context.getDir("bin", Context.MODE_PRIVATE);
	    mSystemBin = new File(Environment.getRootDirectory(), "bin");
	    mBinGetProp = new File(mSystemBin, "getprop");
	    mBinSetProp = new File(mSystemBin, "setprop");
	}

	
	/**
     * Copy binary file from assets into bin directory.
     */
    public void putBinary(String fileName) {
    	try {
    		File file = new File(mBinDir, fileName);
    		InputStream input = mContext.getAssets().open(fileName);
    		int read;
    		byte[] buffer = new byte[4096];
    		OutputStream output = new FileOutputStream(file);
    		
    		while ((read = input.read(buffer)) > 0) {
    			output.write(buffer, 0, read);
    		}
    		input.close();
    		output.close();
    		chmod(file, 711);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Copy binary file from ZIP assets into bin directory.
     */
    public void putZipBinary(ZipInputStream zis, ZipEntry ze) throws IOException {
		String fileName = ze.getName();
		File file = new File(mBinDir, fileName);
		
		if (file.lastModified() >= ze.getTime()) {
			Log.i("ipsec-tools", "File fresh:" + file);
			return;
		}
		
		int read;
		byte[] buffer = new byte[4096];
		OutputStream output = new FileOutputStream(file);
		
		try {
			while ((read = zis.read(buffer)) > 0) {
				output.write(buffer, 0, read);
			}
			
			chmod(file, 711);
		} finally {
			output.close();
		}
    }
    
	/**
     * Copy binary files from ZIP assets into bin directory.
     */
    public void putZipBinaries(String zipName) throws IOException {
		ZipInputStream zis =
			new ZipInputStream(mContext.getAssets().open(zipName));

		try {
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				putZipBinary(zis, ze);
			}
		} finally {
			 zis.close();
		}		 
    }

    /**
     * Set file mode
     * @param file File to modify
     * @param mode New file mode
     */
    private void chmod(File file, int mode) {
		system(new File(mSystemBin, "chmod").getAbsolutePath() + " " + mode + " " + file.getAbsolutePath());
    }


	/**
	 * Set file owner
	 * @param file File to modify
	 * @param user New file user
	 * @param group New file group (or null)
	 */
	public void chown(File file, String user, String group) {
		String param;

		if (group == null)
			param = user;
		else
			param = user + "." + group;

		system(new File(mSystemBin, "chown").getAbsolutePath() + " " + param + " " + file.getAbsolutePath());
	}


    /**
     * Run system command wait for and return result
     * @param cmd System command to run
     * @return stdout
     */
    public static String system(String cmd) {
    	try {
    		// Executes the command.
    		Process process = Runtime.getRuntime().exec("su");
    		
    		DataOutputStream os = new DataOutputStream(process.getOutputStream());            
			Log.i("ipsec-tools", "su command:" + cmd);   			
            os.writeBytes(cmd+"\n");
        
            os.writeBytes("exit\n");  
            os.flush();

    		// Reads stdout.
    		// NOTE: You can write to stdin of the command using
    		//       process.getOutputStream().
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(process.getInputStream()), 8192);
    		int read;
    		char[] buffer = new char[4096];
    		StringBuffer output = new StringBuffer();
    		while ((read = reader.read(buffer)) > 0) {
    			output.append(buffer, 0, read);
    		}
    		reader.close();
        
    		BufferedReader errReader = new BufferedReader(
    				new InputStreamReader(process.getErrorStream()), 8192);
    		int errRead;
    		char[] errBuffer = new char[4096];
    		StringBuffer error = new StringBuffer();
    		while ((errRead = errReader.read(errBuffer)) > 0) {
    			error.append(errBuffer, 0, errRead);
    		}
    		errReader.close();

            // Waits for the command to finish.
    		process.waitFor();
    		
    		if (error.length() > 0)
    			Log.i("ipsec-tools", "System cmd error:" + error);
        
			Log.i("ipsec-tools", "System cmd output:" + output);
    		return output.toString();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    public static String system(String prog, String[] parameters)
    {
    	StringBuffer buf = new StringBuffer(4096);
    	
    	buf.append(prog);
    	for ( String str : parameters ) {
    		buf.append(' ');
    		buf.append(str);
    	}
    	
    	return system(buf.toString());
    }

    public String ls(String[] parameters) {
    	return system(new File(mSystemBin, "ls").getAbsolutePath(), parameters);
    }

    public String getprop(String name) {
    	String value = system(mBinGetProp.getAbsolutePath() + " " + name);
    	return value.trim();
    }

    public void setprop(String name, String value) {
    	system(mBinSetProp.getAbsolutePath() + " " + name + " \"" + value + "\"");
    }
 }
