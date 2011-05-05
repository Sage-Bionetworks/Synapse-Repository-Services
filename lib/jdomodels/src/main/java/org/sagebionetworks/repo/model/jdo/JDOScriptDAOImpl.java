package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.ScriptDAO;



public class JDOScriptDAOImpl implements ScriptDAO {

//	public JDOScript getScript(Long id) {
//		PersistenceManager pm = PMF.get();
//		JDOScript ans = (JDOScript) pm.getObjectById(JDOScript.class,
//				id);
//		// pm.close();
//		return ans;
//	}
//
//	public void makePersistent(JDOScript script) {
//		PersistenceManager pm = PMF.get();
//
//		// the explicit transaction is important for the persistence of both
//		// objects in the
//		// owned relationship. see
//		// http://code.google.com/appengine/docs/java/datastore/relationships.html#Relationships_Entity_Groups_and_Transactions
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//			pm.makePersistent(script);
//			tx.commit();
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}
//
//	public void delete(Long id) {
//		PersistenceManager pm = PMF.get();
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//			Query q = pm.newQuery(JDOScript.class);
//			q.setFilter("id==pId");
//			q.declareParameters(Long.class.getName() + " pId");
//			long n = q.deletePersistentAll(id);
//			if (n != 1)
//				throw new IllegalStateException("Expected 1 but got " + n);
//			tx.commit();
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}

}
