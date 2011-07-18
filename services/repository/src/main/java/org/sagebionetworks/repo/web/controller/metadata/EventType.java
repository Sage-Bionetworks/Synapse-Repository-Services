package org.sagebionetworks.repo.web.controller.metadata;

/**
 * The event type that triggered this call.
 * @author jmhill
 *
 */
public enum EventType{
	CREATE,
	UPDATE,
	GET,
	DELETE,
	NEW_VERSION
}