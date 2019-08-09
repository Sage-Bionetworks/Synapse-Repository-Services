package org.sagebionetworks.securitytools;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Base64;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DecryptResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.google.inject.Inject;

public class EncryptionUtilsImpl implements EncryptionUtils {
	public static final String UTF_8 = "UTF-8";
	private AWSKMS awsKeyManagerClient;
	
	@Inject
	public EncryptionUtilsImpl(AWSKMS awsKeyManagerClient) {
		this.awsKeyManagerClient=awsKeyManagerClient;
	}
	

	@Override
	public String encryptStringWithStackKey(String s) throws UnsupportedEncodingException {
		byte[] plaintext = s.getBytes( UTF_8 );
		EncryptResult encryptResult = this.awsKeyManagerClient.encrypt(
				new EncryptRequest().withPlaintext(ByteBuffer.wrap(plaintext)));
		return new String(Base64.getEncoder().encode(encryptResult.getCiphertextBlob()).array(), UTF_8);
	}

	@Override
	public String decryptStackEncryptedString(String s) throws UnsupportedEncodingException {
		byte[] rawEncrypted = Base64.getDecoder().decode(s.getBytes(UTF_8));
		// KMS can decrypt the value without providing the encryption key.
		DecryptResult decryptResult = this.awsKeyManagerClient.decrypt(new DecryptRequest().withCiphertextBlob(ByteBuffer.wrap(rawEncrypted)));
		return byteBufferToString(decryptResult.getPlaintext());
	}

	/**
	 * Convert the given ByteBuffer to a UTF-8 string.
	 * @param buffer
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private static String byteBufferToString(ByteBuffer buffer) throws UnsupportedEncodingException {
		byte[] rawBytes = new byte[buffer.remaining()];
		buffer.get(rawBytes);
		return new String(rawBytes, UTF_8);
	}
	

}
