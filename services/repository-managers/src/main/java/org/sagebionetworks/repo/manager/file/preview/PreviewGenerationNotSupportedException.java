package org.sagebionetworks.repo.manager.file.preview;

/**
 * RuntimeException that is thrown when the {@link PreviewGenerator} decides that generating a preview can not be supported.
 */
public class PreviewGenerationNotSupportedException extends RuntimeException{
	public PreviewGenerationNotSupportedException(String message, Throwable cause){
		super(message, cause);
	}

	public PreviewGenerationNotSupportedException(String message){
		super(message);
	}
}

