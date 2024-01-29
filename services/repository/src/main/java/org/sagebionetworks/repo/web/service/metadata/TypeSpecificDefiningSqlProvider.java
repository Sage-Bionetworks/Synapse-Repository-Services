package org.sagebionetworks.repo.web.service.metadata;

import org.sagebionetworks.repo.model.Entity;

/**
 * Allows entity specific definingSQL actions
 * 
 * @param <T>
 */
public interface TypeSpecificDefiningSqlProvider<T extends Entity> extends EntityProvider<T> {
	
	/**
	 * Validate the defining SQL for the given entity type
	 * 
	 * @param definingSql
	 * @param entityType
	 */
	public void validateDefiningSql(String definingSql);
}
