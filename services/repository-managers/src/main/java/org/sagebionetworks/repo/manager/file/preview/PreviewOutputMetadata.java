package org.sagebionetworks.repo.manager.file.preview;

public class PreviewOutputMetadata {
	private String contentType, extension;

	public PreviewOutputMetadata(String contentType, String extension) {
		super();
		this.contentType = contentType;
		this.extension = extension;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public String getExtension() {
		return extension;
	}
}
