package org.sagebionetworks.utils;

import java.nio.charset.Charset;

import org.apache.http.entity.ContentType;

import com.amazonaws.services.s3.model.S3Object;

public class ContentTypeUtil {
	/**
	 * 
	 * @param contentTypeString
	 * @return the Charset indicated by the given string or null if no Charset is indicated
	 */
	public static Charset getCharsetFromContentTypeString(String contentTypeString) {
		ContentType contentType = contentTypeString==null ? null : ContentType.parse(contentTypeString);
		return contentType==null ? null : contentType.getCharset();
	}
	
	public static Charset getCharsetFromS3Object(S3Object s3Object) {
		String contentTypeString = s3Object.getObjectMetadata().getContentType();
		return getCharsetFromContentTypeString(contentTypeString);
	}

}
