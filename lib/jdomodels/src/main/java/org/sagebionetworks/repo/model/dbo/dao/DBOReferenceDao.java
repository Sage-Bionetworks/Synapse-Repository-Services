package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;

public interface DBOReferenceDao {
	
	/**
	 * Replace all references for a given owner.
	 * @param toReplace
	 * @return
	 * @throws DatastoreException 
	 */
	public Map<String, Set<Reference>> replaceReferences(Long ownerId, Map<String, Set<Reference>> references) throws DatastoreException;
	
	/**
	 * Get all references for a given owner.
	 * @param ownerId
	 * @return
	 */
	public Map<String, Set<Reference>> getReferences(Long ownerId);

}
