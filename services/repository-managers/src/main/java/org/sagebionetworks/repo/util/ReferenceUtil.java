package org.sagebionetworks.repo.util;

import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;

public interface ReferenceUtil {
	
	/**
	 * A helper method to replace any null version number found in the passed references with the current version of any reference.
	 * 
	 * @param references
	 * @return
	 * @throws DatastoreException
	 */
	public void replaceNullVersionNumbersWithCurrent(Map<String, Set<Reference>> references) throws DatastoreException;

	
	/**
	 * A helper method to replace any null version number found in the passed references with the current version of any reference.
	 * @param references
	 * @return
	 * @throws DatastoreException
	 */
	public void replaceNullVersionNumbersWithCurrent(Set<Reference> references) throws DatastoreException;
}
