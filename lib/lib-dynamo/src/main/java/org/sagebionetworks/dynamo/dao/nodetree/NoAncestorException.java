package org.sagebionetworks.dynamo.dao.nodetree;

/**
 * When no ancestor exists for the node. Every node has at least one ancestor.
 * The root node should have the dummy ROOT as its ancestor.
 */
public class NoAncestorException extends IncompletePathException {

	private static final long serialVersionUID = 6390029699578549939L;

	public NoAncestorException(String msg) {
		super(msg);
	}
}
