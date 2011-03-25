package org.sagebionetworks.repo.model.jdo;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.jdo.persistence.JDOProject;



/**
 * This is the Data Access Object class for the Project class, using JDO
 * 
 * @author bhoff
 * 
 */
public class JDOProjectDAOImpl {


	public JDOProject getProject(Long id) {
		PersistenceManager pm = PMF.get();
		JDOProject ans = (JDOProject) pm.getObjectById(
				JDOProject.class, id);
		// pm.close();
		return ans;
	}

	public void makePersistent(JDOProject project) {
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
			Query q = pm.newQuery(JDOProject.class);
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
