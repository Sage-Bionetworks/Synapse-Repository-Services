package org.sagebionetworks.repo.model.ses;

import java.io.UnsupportedEncodingException;

public class SESNotificationUtils {
	
	public static byte[] encodeBody(String body) {
		try {
			return body.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String decodeBody(byte[] bytes) {
		try {
			return new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
