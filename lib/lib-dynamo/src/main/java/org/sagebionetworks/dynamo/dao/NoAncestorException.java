package org.sagebionetworks.dynamo.dao;

/**
 * When no ancestor exists for the node. Every node has at least one ancestor.
 * The root node should have the dummy ROOT as its ancestor.
 *
 * @author Eric Wu
 */
public class NoAncestorException extends IncompletePathException {

	private static final long serialVersionUID = 6390029699578549939L;

	public NoAncestorException(String msg) {
		super(msg);
	}

	public NoAncestorException(String msg, Throwable e) {
		super(msg, e);
	}

	public NoAncestorException(Throwable e) {
		super(e);
	}
}
