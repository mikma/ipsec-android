package org.za.hem.ipsec_tools;

import java.io.BufferedReader;
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
	
	public NativeCommand(Context context) {
		mContext = context;
	    mBinDir = context.getDir("bin", Context.MODE_PRIVATE);
	    mSystemBin = new File(Environment.getRootDirectory(), "bin");
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
     * Run system command wait for and return result
     * @param cmd System command to run
     * @return stdout
     */
    public static String system(String cmd) {
    	try {
    		// Executes the command.
    		Process process = Runtime.getRuntime().exec(cmd);
        
    		// Reads stdout.
    		// NOTE: You can write to stdin of the command using
    		//       process.getOutputStream().
    		BufferedReader reader = new BufferedReader(
    				new InputStreamReader(process.getInputStream()));
    		int read;
    		char[] buffer = new char[4096];
    		StringBuffer output = new StringBuffer();
    		while ((read = reader.read(buffer)) > 0) {
    			output.append(buffer, 0, read);
    		}
    		reader.close();
        
    		// Waits for the command to finish.
    		process.waitFor();
        
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
 }
