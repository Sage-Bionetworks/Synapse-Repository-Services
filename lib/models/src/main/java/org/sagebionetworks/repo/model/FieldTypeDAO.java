package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.query.FieldType;

/**
 * A cache used to lookup the field type associated with a given annotation name.
 * 
 * @author jmhill
 *
 */
public interface FieldTypeDAO {
	
	/**
	 * Add a new 
	 * @param name
	 * @param type
	 * @throws DatastoreException 
	 * @return true if the definitions already exists, else false.
	 */
	public boolean addNewType(String name, FieldType type) throws DatastoreException;
	
	/**
	 * Get the type for a given attribute name.
	 * @param name
	 * @return
	 */
	public FieldType getTypeForName(String name);
	
	
	/**
	 * Remove a mapping
	 * @param name
	 */
	public void delete(String name);

}
