package org.sagebionetworks.markdown;

public class MarkdownClientException extends Exception {

	private static final long serialVersionUID = -6380805167797397239L;
	private final int statusCode;

	public MarkdownClientException(int statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
