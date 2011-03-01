package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ProjectDAO;



/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * 
 * @author bhoff
 * 
 */
public class GAEJDOProjectDAOImpl /*implements ProjectDAO*/ {


	public GAEJDOProject getProject(Long id) {
		PersistenceManager pm = PMF.get();
		GAEJDOProject ans = (GAEJDOProject) pm.getObjectById(
				GAEJDOProject.class, id);
		// pm.close();
		return ans;
	}

	public void makePersistent(GAEJDOProject project) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(project);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void delete(Long id) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query q = pm.newQuery(GAEJDOProject.class);
			q.setFilter("id==pId");
			q.declareParameters(Long.class.getName() + " pId");
			long n = q.deletePersistentAll(id);
			if (n != 1)
				throw new IllegalStateException("Expected 1 but got " + n);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}


}
