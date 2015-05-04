package org.sagebionetworks.repo.manager.file;

import java.nio.charset.Charset;

import org.apache.http.entity.ContentType;

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

}
