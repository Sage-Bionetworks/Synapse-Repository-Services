package org.sagebionetworks.repo.model;

import com.google.appengine.api.datastore.Key;


/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * @author bhoff
 *
 */
public interface ProjectAccessor {
	public Project getProject(Key id);
	
	public void makePersistent(Project project);
	
	public void delete(Project project);
}
