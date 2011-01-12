package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.gaejdo.GAEJDODataset;

import com.google.appengine.api.datastore.Key;

public interface DatasetAccessor {
	public GAEJDODataset getDataset(Key id);
	
	public void makePersistent(GAEJDODataset dataset);
	
	//public void delete(GAEJDODataset dataset);

	public void delete(Key id);

	public void deleteDatasetAndContents(GAEJDODataset dataset);
	
	/**
	 * To revise an object: 
	 * 1) retrieve an existing object, 
	 * 2) call 'makeTransient'
	 * 2) edit members as desired, 
	 * 	(This could include updating a member DatasetLayer with a new 
	 * 	version of the same DatasetLayer.)
	 * 3) increment the version field in the revision, 
	 * 4) call 'makePersistent' to add it as a new object to the data store
	 * 
	 * @param dataset
	 */
	public void makeTransient(GAEJDODataset dataset);
}
