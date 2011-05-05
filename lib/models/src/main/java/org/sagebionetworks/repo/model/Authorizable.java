package org.sagebionetworks.repo.model;

/*
 * interface for objects to which authorization can be granted
 */
public interface Authorizable {
	String getId();
	String getType();
}
