package org.sagebionetworks.repo.model.query.entity;

/**
 * 
 * Abstraction for a converter that transforms input values
 * into 
 *
 */
public interface ValueTransformer {

	/**
	 * Transform the given input into a value that can be used for the database.
	 * 
	 * @param input
	 * @return
	 */
	public Object transform(Object input);
}
