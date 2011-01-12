package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOScript;

import com.google.appengine.api.datastore.Key;

public interface ScriptAccessor {
	public GAEJDOScript getScript(Key id);
	
	public GAEJDOScript getLatest(GAEJDOScript original);
	
	public void makePersistent(GAEJDOScript script);
	
//	public void delete(Script script);
	
	public void delete(Key id);

	/**
	 * To revise an object: 
	 * 1) retrieve an existing object, 
	 * 2) call 'makeTransient'
	 * 2) edit members as desired, 
	 * 3) increment the version field in the revision, 
	 * 4) call 'makePersistent' to add it as a new object to the data store
	 * 
	 * @param dataset
	 */
	public void makeTransient(GAEJDOScript script);
}
