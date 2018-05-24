package org.sagebionetworks.aws;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.StringJoiner;

import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;

public class ParameterProviderImpl implements ParameterProvider {

	public static final String UTF_8 = "UTF-8";

	@Autowired
	AWSSimpleSystemsManagement ssmClient;

	@Autowired
	AWSKMS keyManagementClient;

	@Autowired
	StackConfiguration config;

	@Override
	public String getDecryptedValue(String key) {
		// get the raw value
		String encryptedValue = getValue(key);
		DecryptResult result = keyManagementClient
				.decrypt(new DecryptRequest().withCiphertextBlob(stringToByteBuffer(encryptedValue)));
		return byteBufferToString(result.getPlaintext());
	}

	@Override
	public String getValue(String key) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		String fullKey = createFullKeyName(key);
		GetParameterResult result = ssmClient.getParameter(new GetParameterRequest().withName(fullKey));
		return result.getParameter().getValue();
	}

	/**
	 * The full key includes <stack>.<instance>.key
	 * 
	 * @param key
	 * @return
	 */
	String createFullKeyName(String key) {
		StringJoiner joiner = new StringJoiner(".");
		joiner.add(config.getStack());
		joiner.add(config.getStackInstance());
		joiner.add(key);
		return joiner.toString();
	}

	/**
	 * Convert a string to ByteBuffer.
	 * 
	 * @param source
	 * @return
	 */
	public static ByteBuffer stringToByteBuffer(String source) {
		try {
			return ByteBuffer.wrap(source.getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert a ByteBuffer to a string.
	 * 
	 * @param buffer
	 * @return
	 */
	public static String byteBufferToString(ByteBuffer buffer) {
		try {
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			return new String(bytes, UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
