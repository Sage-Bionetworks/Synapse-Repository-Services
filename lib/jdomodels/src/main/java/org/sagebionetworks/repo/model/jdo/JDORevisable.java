package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.jdo.persistence.JDORevision;

/**
 * Interfaced implemented by all classes which have versions.
 * 
 * @author bhoff <T> the type of the Object 'owning' the revision
 */
public interface JDORevisable<T extends JDORevisable<T>> extends
		JDOBase {
	void setRevision(JDORevision<T> r);

	JDORevision<T> getRevision();
	
	public T getNextVersion();

	public void setNextVersion(T nextVersion);


}
