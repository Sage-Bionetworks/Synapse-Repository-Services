package org.sagebionetworks.repo.model.dbo.schema;

import java.util.Optional;

import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.Keys;

/**
 * Abstraction of a DAO to read/write derived annotations to/from the database.
 *
 */
public interface DerivedAnnotationDao {

	/**
	 * Save the provided annotations to the database.
	 * 
	 * @param entityId
	 * @param a
	 */
	void saveDerivedAnnotations(String entityId, Annotations a);

	/**
	 * Get the derived annotations for the given entity from the Database.
	 * 
	 * @param entityId
	 * @return {@link Optional#empty()} if there are no annotations saved for the
	 *         given entity.
	 */
	Optional<Annotations> getDerivedAnnotations(String entityId);
	
	/**
	 * Get the derived annotation keys for the given entity from the Database.
	 * 
	 * @param entityId
	 * @return {@link Optional#empty()} if there are no annotations saved for the
	 *         given entity.
	 */
	Optional<Keys> getDerivedAnnotationKeys(String entityId);

	/**
	 * Clear any saved annotations for the given entity. If there are no annotations
	 * for the given entity then this call will do nothing.
	 * 
	 * @param entityId
	 */
	void clearDerivedAnnotations(String entityId);

	/**
	 * Clear all data from the table.
	 */
	void clearAll();

}
