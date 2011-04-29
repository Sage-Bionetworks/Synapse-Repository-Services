package org.sagebionetworks.repo.model.jdo.temp;

import java.util.Set;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.JDOAnnotatable;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDOInputDataLayer;
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
	public String create(JDODataset toCreate) throws DataAccessException, InvalidModelException;
	
	/**
	 * Get a dataset using its id.
	 * @param id
	 * @return
	 */
	public JDODataset get(String id);
	
	/**
	 * Get the annotations for a class
	 * @param ownerClass
	 * @param id
	 * @return
	 */
	public JDOAnnotations getAnnotations(Class<? extends JDOAnnotatable> ownerClass, String id);
	
	/**
	 * Delete a dataset using its id.
	 * @param id
	 * @return 
	 */
	public boolean delete(String id);
	
	/**
	 * Update the annotations on a dataset.
	 * @param datasetid
	 * @param newAnnoations
	 */
	public void updateAnnotations(Class<? extends JDOAnnotatable> ownerClass, String id, JDOAnnotations newAnnoations);
	

}
