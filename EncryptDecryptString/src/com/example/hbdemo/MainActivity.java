package com.example.hbdemo;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

/**
 * This Activity shows encryption and decryption of a string.
 * @author <b>Harshal Benake</b>
 *
 */
public class MainActivity extends Activity {
	 static final String TAG = "SymmetricAlgorithmAES";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Original text
        String originalText = "This is my text - Harshal Benake";
        System.out.println("[ORIGINAL]:" + originalText);
        // Set up secret key spec for 128-bit AES encryption and decryption
        SecretKeySpec secretKeySpec = null;
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed("any data used as random seed".getBytes());
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128, secureRandom);
            secretKeySpec = new SecretKeySpec((keyGenerator.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {
            Log.e(TAG, "AES secret key spec error");
        }
        // Encode the original data with AES
        byte[] encodedBytes = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            encodedBytes = cipher.doFinal(originalText.getBytes());
        } catch (Exception e) {
            Log.e(TAG, "AES encryption error");
        }
        System.out.println("[ENCODED]:" + Base64.encodeToString(encodedBytes, Base64.DEFAULT));
        // Decode the encoded data with AES
        byte[] decodedBytes = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            decodedBytes = cipher.doFinal(encodedBytes);
        } catch (Exception e) {
            Log.e(TAG, "AES decryption error");
        }
        System.out.println("[DECODED]:" + new String(decodedBytes));
    }
}
