package org.sagebionetworks.repo.model.dbo.file;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class MultipartRequestUtils {
	

	/**
	 * Get the JSON string of the request.
	 * 
	 * @param request
	 * @return
	 */
	public static String createRequestJSON(MultipartRequest request) {
		try {
			return EntityFactory.createJSONStringForEntity(request);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * De-serialize the request from the json string
	 * @param <T>
	 * @param requestJson
	 * @param clazz
	 * @return
	 */
	public static <T extends MultipartRequest> T getRequestFromJson(String requestJson, Class<T> clazz) {
		try {
			return EntityFactory.createEntityFromJSONString(requestJson, clazz);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Calculate the MD5 of the given multipart request
	 * 
	 * @param request
	 * @return
	 */
	public static String calculateMD5AsHex(MultipartRequest request) {
		try {
			String json = EntityFactory.createJSONStringForEntity(request);
			byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] md5Bytes = messageDigest.digest(jsonBytes);
			return new String(Hex.encodeHex(md5Bytes));
		} catch (JSONObjectAdapterException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
