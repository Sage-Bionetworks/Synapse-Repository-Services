package org.sagebionetworks.repo.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.util.SerializationUtils;

public class SignedTokenUtil {
		
	private static final String ENCRYPTION_CHARSET = Charset.forName("utf-8").name();

	/**
	 * Add the HMAC and serialize
	 * @param token
	 * @return the signed, serialized token
	 */
	public static String signAndSerialized(SignedTokenInterface token) {
		token.setHmac(generateSignature(token));
		return SerializationUtils.serializeAndHexEncode(token);
	}
	
	public static String generateSignature(SignedTokenInterface token) {
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
	 * Deserialize and validate the HMAC
	 * 
	 * @param token
	 * @param type
	 * @return
	 */
	public static <T extends SignedTokenInterface> T deserializeAndValidateToken(String token, Class<T> type) {
		T result = SerializationUtils.hexDecodeAndDeserialize(token, type);
		String hmac = result.getHmac();
		result.setHmac(null);
		String regeneratedHmac = generateSignature(result);
		if (!regeneratedHmac.equals(hmac)) 
			throw new IllegalArgumentException("Invalid digital signature.");
		result.setHmac(hmac);
		return result;
	}


}
