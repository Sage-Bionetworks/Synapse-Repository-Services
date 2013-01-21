package org.sagebionetworks.dynamo.dao;

/**
 * When the path to the root is incomplete.
 *
 * @author Eric Wu
 */
public class IncompletePathException extends RuntimeException {

	private static final long serialVersionUID = 6869337894039357790L;

	public IncompletePathException(String msg) {
		super(msg);
	}
}
