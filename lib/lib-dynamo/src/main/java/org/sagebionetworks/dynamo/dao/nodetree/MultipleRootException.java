package org.sagebionetworks.dynamo.dao.nodetree;

/**
 * When there exists more than one root.
 */
public class MultipleRootException extends RuntimeException {

	private static final long serialVersionUID = 7580596365261968863L;

	public MultipleRootException(String msg) {
		super(msg);
	}
}
