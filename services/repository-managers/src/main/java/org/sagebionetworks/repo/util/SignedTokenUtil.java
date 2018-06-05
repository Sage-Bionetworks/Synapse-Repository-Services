package org.sagebionetworks.repo.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Date;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;

public class SignedTokenUtil {
		
	private static final String ENCRYPTION_CHARSET = Charset.forName("utf-8").name();
	
	public static final long TOKEN_EXPIRATION_MS = 1000*60*60*24*31;
	public static final int DEFAULT_KEY_VERSION = 0;

	/**
	 * Add the HMAC
	 * @param token
	 * @return the signed, serialized token
	 */
	public static void signToken(SignedTokenInterface token) {
		int keyVersion = StackConfiguration.getCurrentHmacSigningKeyVersion();
		String hmac = generateSignature(token, keyVersion);
		token.setHmac(hmac);
		token.setVersion((long) keyVersion);
		long expires = System.currentTimeMillis()+TOKEN_EXPIRATION_MS;
		token.setExpiresOn(new Date(expires));
	}
	
	static String generateSignature(SignedTokenInterface token, int keyVersion) {
		if (token.getHmac()!=null) throw new IllegalArgumentException("HMAC is added only after generating signature.");
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(token);
			byte[] secretKey = StackConfiguration.getHmacSigningKeyForVersion(keyVersion).getBytes(ENCRYPTION_CHARSET);
			byte[] signatureAsBytes = HMACUtils.generateHMACSHA1SignatureFromRawKey(jsonString, secretKey);
			return new String(signatureAsBytes, ENCRYPTION_CHARSET);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * validate the HMAC
	 * 
	 * @param token
	 * @return
	 */
	public static <T extends SignedTokenInterface> T validateToken(T token) {
		String hmac = token.getHmac();
		token.setHmac(null);
		int keyVersion = DEFAULT_KEY_VERSION;
		if(token.getVersion() != null) {
			keyVersion = token.getVersion().intValue();
		}
		String regeneratedHmac = generateSignature(token, keyVersion);
		if (!regeneratedHmac.equals(hmac)) 
			throw new IllegalArgumentException("Invalid digital signature.");
		token.setHmac(hmac);
		return token;
	}


}
