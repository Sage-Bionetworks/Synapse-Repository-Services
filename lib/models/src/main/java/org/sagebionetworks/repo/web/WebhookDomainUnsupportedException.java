package org.sagebionetworks.repo.web;

public class WebhookDomainUnsupportedException extends RuntimeException {

	public WebhookDomainUnsupportedException() {
		super("Unsupported invoke endpoint, please contact support for more information.");
	}

}
