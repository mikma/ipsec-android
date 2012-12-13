package org.za.hem.ipsec_tools.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import android.content.Context;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

public class CertManager {
    public static final String CERT_PREFIX = "cert_";
    public static final String CERT_POSTFIX = ".pem";
    public static final String KEY_POSTFIX = ".key";
    public static final String P12_POSTFIX = ".p12";
    
    private KeyStore mKS;
    private File mCertDir;
    private File mKeyStoreFile;
    private static final String KEY_STORE_FILE = "ipsec.keystore";
    private static final String PEM_CERT_NAME = "CERTIFICATE";
    private static final String PEM_KEY_NAME = "PRIVATE KEY";
    private static final String KEYSTORE_PKCS12 = "PKCS12";
    private static final String KEYSTORE_BKS = "BKS";
    private static final char[] MASTER_PWD = "foobar".toCharArray();
    
    public CertManager(Context context) throws KeyStoreException, CertificateException, IOException {
        mCertDir = context.getDir("certs", Context.MODE_PRIVATE);
        mKeyStoreFile = new File(mCertDir, KEY_STORE_FILE);
        
        mKS = java.security.KeyStore.getInstance(KEYSTORE_BKS);
        if (mKeyStoreFile.canRead()) {
            try {
                mKS.load(new BufferedInputStream(new FileInputStream(mKeyStoreFile)),
                         MASTER_PWD);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (CertificateException e) {
                // Start with empty key store
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                // Start with empty key store
                e.printStackTrace();
            }
            Log.i("CertManager", "KeyStore loaded " + mKS.size() + " aliases");
        } else {
            try {
                mKS.load(null, null);
                Log.i("CertManager", "Empty KeyStore loaded");
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public CharSequence[] getAliases() throws KeyStoreException {
        String[] aliasArray = new String[mKS.size()];
        Enumeration<String> aliases = mKS.aliases();
        int i = 0;
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            aliasArray[i++] = alias;
        }
        return aliasArray;
    }
    
    public void load(InputStream p12Stream, char[] password) throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
        KeyStore ks = getPKCS12Instance();
        ks.load(p12Stream, password);
        
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            try {
                KeyStore.Entry entry = ks.getEntry(alias, null);
                mKS.setEntry(alias, entry, null);
            } catch (UnrecoverableEntryException e) {
                // Ignore cert
                e.printStackTrace();
            }
        }
        
        OutputStream os = new BufferedOutputStream(new FileOutputStream(mKeyStoreFile));
        mKS.store(os, MASTER_PWD);
        os.close();
    }
    
    static protected KeyStore getPKCS12Instance() throws KeyStoreException {
        return java.security.KeyStore.getInstance(KEYSTORE_PKCS12);
    }

    public void writeCert(File path, String filePrefix, String alias) {
        File certFile = new File(path, filePrefix + CERT_POSTFIX);
        File keyFile = new File(path, filePrefix + KEY_POSTFIX);
        try {
            if (!mKS.containsAlias(alias))
                // TODO handle error
                return;
            Certificate[] chain = mKS.getCertificateChain(alias);
            Key key = mKS.getKey(alias, null);
            
            OutputStream certOs = new BufferedOutputStream(new FileOutputStream(certFile));                        
            for(int i = 0; i < chain.length; i++) {
                Certificate cert = chain[i];
                    writeBase64(certOs, PEM_CERT_NAME, cert.getEncoded());
            }
            certOs.close();
        
            OutputStream keyOs = new BufferedOutputStream(new FileOutputStream(keyFile));                        
            writeBase64(keyOs, PEM_KEY_NAME, key.getEncoded());
            keyOs.close();
            return;
        } catch (IOException e) {
            // Ignore
            e.printStackTrace();
        } catch (CertificateEncodingException e) {
            // Ignore
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // Ignore
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // Ignore
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // Ignore
            e.printStackTrace();
        }

        // Failed
        if (certFile.exists())
            certFile.delete();
        if (keyFile.exists())
            keyFile.delete();
    }

    private void writeBase64(OutputStream fos, String type, byte[] encoded) throws IOException {
        OutputStreamWriter os = new OutputStreamWriter(fos);
        os.write("-----BEGIN " + type + "-----\n");
        os.flush();
        OutputStream base64 = new Base64OutputStream(fos,
                Base64.NO_CLOSE | Base64.NO_PADDING);
        base64.write(encoded);
        base64.close();

        switch(encoded.length % 3) {
        case 0: break;
        case 1: os.write("=="); break;
        case 2: os.write("="); break;
        }
        os.write("\n-----END " + type + "-----\n");
        os.flush();
    }
}
