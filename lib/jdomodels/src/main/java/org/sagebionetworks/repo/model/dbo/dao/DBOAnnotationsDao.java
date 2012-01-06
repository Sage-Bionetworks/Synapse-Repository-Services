package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;

public interface DBOAnnotationsDao {
	
	/**
	 * Get all of the annotations for an entity.
	 * @param owner
	 * @return
	 */
	public Annotations getAnnotations(Long owner);
	
	/**
	 * Replace all annotations.
	 * @param annotations
	 * @throws DatastoreException 
	 */
	public void replaceAnnotations(Annotations annotations) throws DatastoreException;

}
