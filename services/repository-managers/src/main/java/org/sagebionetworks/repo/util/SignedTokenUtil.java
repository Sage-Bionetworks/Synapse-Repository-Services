package org.sagebionetworks.repo.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;

public class SignedTokenUtil {
		
	private static final String ENCRYPTION_CHARSET = Charset.forName("utf-8").name();

	/**
	 * Add the HMAC
	 * @param token
	 * @return the signed, serialized token
	 */
	public static void signToken(SignedTokenInterface token) {
		token.setHmac(generateSignature(token));
	}
	
	private static String generateSignature(SignedTokenInterface token) {
		if (token.getHmac()!=null) throw new IllegalArgumentException("HMAC is added only after generating signature.");
		try {
			String jsonString = EntityFactory.createJSONStringForEntity(token);
			byte[] secretKey = StackConfiguration.getEncryptionKey().getBytes(ENCRYPTION_CHARSET);
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
		String regeneratedHmac = generateSignature(token);
		if (!regeneratedHmac.equals(hmac)) 
			throw new IllegalArgumentException("Invalid digital signature.");
		token.setHmac(hmac);
		return token;
	}


}
