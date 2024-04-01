package org.sagebionetworks.upload.multipart;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.google.cloud.storage.Blob;

public class GoogleUtils {

	public static final String UNSUPPORTED_COPY_MSG = "Copying from a Google Cloud Bucket is not supported yet.";

	// 15 minutes
	public static final int PRE_SIGNED_URL_EXPIRATION_MS = 15 * 1000 * 60;
	
	public static void validatePartMd5(Blob uploadedPart, String partMD5Hex) {
		if (uploadedPart == null) {
			throw new IllegalArgumentException("The uploaded part could not be found");
		}
		if (!Hex.encodeHexString(Base64.decodeBase64(uploadedPart.getMd5())).equals(partMD5Hex)) {
			throw new IllegalArgumentException("The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.");
		}
	}
}
