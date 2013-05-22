package org.sagebionetworks.dynamo.dao.nodetree;

/**
 * When a node has more than one ancestor of a particular generation. One special
 * case is a child that has more than one parent.
 */
public class MultipleInheritanceException extends RuntimeException {

	private static final long serialVersionUID = -74309534033842734L;

	public MultipleInheritanceException(String msg) {
		super(msg);
	}
}
