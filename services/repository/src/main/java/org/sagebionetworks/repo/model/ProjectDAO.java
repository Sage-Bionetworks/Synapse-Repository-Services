package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOProject;

import com.google.appengine.api.datastore.Key;

/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * 
 * @author bhoff
 * 
 */
public interface ProjectDAO extends BaseDAO {
	// public GAEJDOProject getProject(Key id);
	//
	// public void makePersistent(GAEJDOProject project);
	//
	// //public void delete(Project project);
	//
	// public void delete(Key id);
}
