package org.sagebionetworks.repo.model.dbo;

/**
 * 
 * @author John
 *
 */
public interface AutoIncrementDatabaseObject<T> extends DatabaseObject<T>{
	
	/**
	 * The ID the auto-generated ID.
	 * @return
	 */
	public Long getId();
	
	/**
	 * The ID the auto-generated ID.
	 * @param id
	 */
	public void setId(Long id);

}
