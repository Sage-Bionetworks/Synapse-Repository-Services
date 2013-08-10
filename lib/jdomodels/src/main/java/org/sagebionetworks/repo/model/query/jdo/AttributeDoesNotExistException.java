package org.sagebionetworks.repo.model.query.jdo;

public class AttributeDoesNotExistException extends Exception {

	private static final long serialVersionUID = -8020668982415939359L;

	public AttributeDoesNotExistException(String message) {
		super(message);
	}
}
