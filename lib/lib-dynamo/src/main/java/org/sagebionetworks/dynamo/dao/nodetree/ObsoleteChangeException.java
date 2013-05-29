package org.sagebionetworks.dynamo.dao.nodetree;

/**
 * When the hierarchical change is out-of-date.
 */
public class ObsoleteChangeException extends RuntimeException {

	private static final long serialVersionUID = 6051513401912133186L;

	public ObsoleteChangeException(String msg) {
		super(msg);
	}
}
