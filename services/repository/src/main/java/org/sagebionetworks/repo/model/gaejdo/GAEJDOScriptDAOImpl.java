package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ScriptDAO;

import com.google.appengine.api.datastore.Key;

public class GAEJDOScriptDAOImpl implements ScriptDAO {

	public GAEJDOScript getScript(Key id) {
		PersistenceManager pm = PMF.get();
		GAEJDOScript ans = (GAEJDOScript) pm.getObjectById(GAEJDOScript.class,
				id);
		// pm.close();
		return ans;
	}

	public void makePersistent(GAEJDOScript script) {
		PersistenceManager pm = PMF.get();

		// the explicit transaction is important for the persistence of both
		// objects in the
		// owned relationship. see
		// http://code.google.com/appengine/docs/java/datastore/relationships.html#Relationships_Entity_Groups_and_Transactions
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(script);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void delete(Key id) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query q = pm.newQuery(GAEJDOScript.class);
			q.setFilter("id==pId");
			q.declareParameters(Key.class.getName() + " pId");
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
