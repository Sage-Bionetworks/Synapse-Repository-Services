package org.sagebionetworks.client.upload;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Binds MD5 MessageDigest in a ThreadLocal. This allows each digest to be
 * reused by each thread.
 *
 */
public class ThreadLocalMD5Digest {

	private static final ThreadLocal<MessageDigest> MD5_DIGEST_LOCAL;
	static {
		MD5_DIGEST_LOCAL = new ThreadLocal<MessageDigest>() {
			@Override
			protected MessageDigest initialValue() {
				try {
					return MessageDigest.getInstance("MD5");
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	/**
	 * Get the current thread's MD5 MessageDigest.
	 * 
	 * @return
	 */
	public static MessageDigest getThreadDigest() {
		MessageDigest digest = MD5_DIGEST_LOCAL.get();
		digest.reset();
		return digest;
	}

}
