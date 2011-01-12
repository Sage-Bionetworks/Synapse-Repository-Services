package org.sagebionetworks.repo.model.gaejdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.ProjectAccessor;

import com.google.appengine.api.datastore.Key;


/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * @author bhoff
 *
 */
public class ProjectAccessorImpl implements ProjectAccessor {
//	PersistenceManager pm;
//	
//	public ProjectAccessorImpl(PersistenceManager pm) {
//		this.pm=pm;
//	}
	
	public GAEJDOProject getProject(Key id) {
		PersistenceManager pm = PMF.get();		
		GAEJDOProject ans = (GAEJDOProject)pm.getObjectById(GAEJDOProject.class, id);	
		//pm.close();
		return ans;
	}
	
	public void makePersistent(GAEJDOProject project) {
		PersistenceManager pm = PMF.get();		
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
				pm.close();
		}	
	}
	
//	public void delete(Project project) {
//		PersistenceManager pm = PMF.get();		
//		Transaction tx=null;
//		try {
//			 	tx=pm.currentTransaction();
//				tx.begin();
//				pm.deletePersistent(project);
//				tx.commit();
//		} finally {
//				if(tx.isActive()) {
//					tx.rollback();
//				}
//				pm.close();
//		}	
//	}
	public void delete(Key id) {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				Query q = pm.newQuery(GAEJDOProject.class);
				q.setFilter("id==pId");
				q.declareParameters(Key.class.getName()+" pId");
				long n = q.deletePersistentAll(id);
				if (n!=1) throw new IllegalStateException("Expected 1 but got "+n);
				tx.commit();
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}

}
