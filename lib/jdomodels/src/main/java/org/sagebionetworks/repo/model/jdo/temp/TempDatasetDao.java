package org.sagebionetworks.repo.model.jdo.temp;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.springframework.dao.DataAccessException;

/**
 * A temporary DAO for loading datasets
 * @author jmhill
 *
 */
public interface TempDatasetDao {
	
	/**
	 * Create a new Dataset
	 * @param toCreate
	 * @return
	 * @throws InvalidModelException 
	 * @throws DataAccessException 
	 */
	public String create(Dataset toCreate) throws DataAccessException, InvalidModelException;
	
	/**
	 * Get a dataset using its id.
	 * @param id
	 * @return
	 */
	public Dataset get(String id);
	
	/**
	 * Delete a dataset using its id.
	 * @param id
	 * @return 
	 */
	public boolean delete(String id);

}
