package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.sage.datamodel.Project;
import org.sage.datamodel.ProjectAccessor;

import com.google.appengine.api.datastore.Key;


/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * @author bhoff
 *
 */
public class ProjectAccessorImpl implements ProjectAccessor {
	PersistenceManager pm;
	
	public ProjectAccessorImpl(PersistenceManager pm) {
		this.pm=pm;
	}
	
	public Project getProject(Key id) {
		return (Project)pm.getObjectById(Project.class, id);		
	}
	
	public void makePersistent(Project project) {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(project);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}
	
	public void delete(Project project) {
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.deletePersistent(project);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
		}	
	}

}
