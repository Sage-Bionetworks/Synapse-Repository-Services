package org.sagebionetworks.repo.manager.trash;

public class TooBigForTrashcanException extends RuntimeException {

	private static final long serialVersionUID = 1536626763999777501L;

	public TooBigForTrashcanException(String msg) {super(msg);}
}
