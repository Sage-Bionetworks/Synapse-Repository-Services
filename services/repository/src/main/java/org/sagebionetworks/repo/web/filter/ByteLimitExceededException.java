package org.sagebionetworks.repo.web.filter;

/**
 * Exception thrown when the number of bytes read from an InputStream exceeds the limit.
 *
 */
public class ByteLimitExceededException extends IllegalArgumentException {
	
	private static final long serialVersionUID = 1L;

	public ByteLimitExceededException(String s) {
		super(s);
	}

}
