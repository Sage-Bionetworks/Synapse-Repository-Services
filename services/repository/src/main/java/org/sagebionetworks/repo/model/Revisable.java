package org.sagebionetworks.repo.model;


/**
 * Interfaced implemented by all classes which have versions.
 * @author bhoff
 * <T> the type of the Object 'owning' the revision
 */
public interface Revisable<T extends Revisable<T>> {
	Revision<T> getRevision();
}
