package org.sagebionetworks.dynamo.dao;

/**
 * When the hierarchical change is out-of-date.
 *
 * @author Eric Wu
 */
public class ObsoleteChangeException extends RuntimeException {

	private static final long serialVersionUID = 6051513401912133186L;

	public ObsoleteChangeException(String msg) {
		super(msg);
	}

	public ObsoleteChangeException(String msg, Throwable e) {
		super(msg, e);
	}

	public ObsoleteChangeException(Throwable e) {
		super(e);
	}
}
