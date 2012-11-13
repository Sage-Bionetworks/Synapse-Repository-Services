package org.sagebionetworks.dynamo.dao;

/**
 * Cannot find the parent. The parent should exist before any child.
 *
 * @author Eric Wu
 */
public class ParentNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -6103939002096733112L;

	public ParentNotFoundException(String msg) {
		super(msg);
	}

	public ParentNotFoundException(String msg, Throwable e) {
		super(msg, e);
	}

	public ParentNotFoundException(Throwable e) {
		super(e);
	}
}
