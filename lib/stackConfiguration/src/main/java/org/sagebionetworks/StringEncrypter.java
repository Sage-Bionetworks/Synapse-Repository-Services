package org.sagebionetworks;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.apache.commons.codec.binary.Base64;

public class StringEncrypter {
	
	public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
	
	private KeySpec				keySpec;
	private SecretKeyFactory	keyFactory;
	private Cipher				cipher;
	
	private static final String	UNICODE_FORMAT			= "UTF8";
	
	// args[0] string to encode
	// args[1] encoding key
	// prints out encoded string
	public static void main(String[] args) {
		if (args.length<2) {
			System.out.println("Usage: StringEncrypter <plaintext> <encodingkey(s)>");
			System.exit(0);
		}
		for (int i = 1; i < args.length; i++) {
			StringEncrypter se = new StringEncrypter(args[i]);	
			System.out.println(args[0]+" -> "+se.encrypt(args[0]) + " (for encryption key " + args[i] + ")");
		}

	}

	public StringEncrypter(String encryptionKey ) {
		this(DESEDE_ENCRYPTION_SCHEME, encryptionKey);
	}

	public StringEncrypter( String encryptionScheme, String encryptionKey ) {

		if ( encryptionKey == null )
				throw new IllegalArgumentException( "encryption key was null" );
		if ( encryptionKey.trim().length() < 24 )
				throw new IllegalArgumentException(
						"encryption key was less than 24 characters" );

		try {
			byte[] keyAsBytes = encryptionKey.getBytes( UNICODE_FORMAT );

			if ( encryptionScheme.equals( DESEDE_ENCRYPTION_SCHEME) ) {
				keySpec = new DESedeKeySpec( keyAsBytes );
			}
			else {
				throw new IllegalArgumentException( "Encryption scheme not supported: "
													+ encryptionScheme );
			}

			keyFactory = SecretKeyFactory.getInstance( encryptionScheme );
			cipher = Cipher.getInstance( encryptionScheme );

		} catch (InvalidKeyException e)	{
			throw new RuntimeException( e );
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException( e );
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException( e );
		}

	}

	public String encrypt( String unencryptedString ) {
		if ( unencryptedString == null || unencryptedString.trim().length() == 0 )
				throw new IllegalArgumentException(
						"unencrypted string was null or empty" );

		try {
			SecretKey key = keyFactory.generateSecret( keySpec );
			cipher.init( Cipher.ENCRYPT_MODE, key );
			byte[] cleartext = unencryptedString.getBytes( UNICODE_FORMAT );
			byte[] ciphertext = cipher.doFinal( cleartext );

			return new String(Base64.encodeBase64(ciphertext));
		} catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	public String decrypt( String encryptedString ) {
		if ( encryptedString == null || encryptedString.trim().length() <= 0 )
				throw new IllegalArgumentException( "encrypted string was null or empty" );

		try {
			SecretKey key = keyFactory.generateSecret( keySpec );
			cipher.init( Cipher.DECRYPT_MODE, key );
			byte[] cleartext = Base64.decodeBase64(encryptedString.getBytes());

			byte[] ciphertext = cipher.doFinal( cleartext );

			return bytes2String( ciphertext );
		} 
		catch(BadPaddingException e) {
			throw new RuntimeException("The encryption key does not match the one used to encrypt the property", e);
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static String bytes2String( byte[] bytes ) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			stringBuffer.append( (char) bytes[i] );
		}
		return stringBuffer.toString();
	}

	public static class EncryptionException extends Exception {
		public EncryptionException( Throwable t ) {
			super( t );
		}
	}
}
