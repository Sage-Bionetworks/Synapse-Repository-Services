package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.apache.commons.fileupload.FileItemStream;

public class ByteArrayFileItemStream implements FileItemStream {
	private byte[] bytes;
	private String contentType;
	private String name;
	
	ByteArrayFileItemStream(byte[] bytes, String contentType, String name) {
		this.bytes = bytes;
		this.contentType = contentType;
		this.name=name;
	}
	
	@Override
	public InputStream openStream() throws IOException {
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFieldName() {
		return "";
	}

	@Override
	public boolean isFormField() {
		return false;
	}

}
