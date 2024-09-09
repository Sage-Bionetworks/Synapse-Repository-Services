package org.sagebionetworks.repo.service.metadata;

import org.sagebionetworks.repo.model.Entity;

/**
 * Allows entity specific definingSQL actions
 * 
 * @param <T>
 */
public interface TypeSpecificDefiningSqlProvider<T extends Entity> extends EntityProvider<T> {
	
	/**
	 * Validate the defining SQL for the given entity type.
	 * This will throw an IllegalArgumentException if the SQl is invalid.
	 * 
	 * @param definingSql
	 */
	public void validateDefiningSql(String definingSql);
}
