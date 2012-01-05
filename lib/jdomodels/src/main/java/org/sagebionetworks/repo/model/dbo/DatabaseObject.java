package org.sagebionetworks.repo.model.dbo;

/**
 * This is a place holder interface for Database Objects.
 * @author John
 *
 */
public interface DatabaseObject<T> {
	
	/**
	 * Get the database Mapping for this object.
	 * @return
	 */
	public TableMapping<T> getTableMapping();

}
