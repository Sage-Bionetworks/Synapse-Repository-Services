package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnalysisResultDAO;

import com.google.appengine.api.datastore.Key;

public class GAEJDOAnalysisResultDAOImpl implements AnalysisResultDAO {
	// PersistenceManager pm;
	//
	// public GAEJDOAnalysisResultDAOImpl() {
	// this.pm=pm;
	// }

	public GAEJDOAnalysisResult getAnalysisResult(Key id) {
		PersistenceManager pm = PMF.get();
		GAEJDOAnalysisResult ans = (GAEJDOAnalysisResult) pm.getObjectById(
				GAEJDOAnalysisResult.class, id);
		// pm.close();
		return ans;
	}

	public void makePersistent(GAEJDOAnalysisResult analysisResult) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(analysisResult);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	// public void delete(AnalysisResult analysisResult) {
	// PersistenceManager pm = PMF.get();
	// Transaction tx=null;
	// try {
	// tx=pm.currentTransaction();
	// tx.begin();
	// pm.deletePersistent(analysisResult);
	// tx.commit();
	// } finally {
	// if(tx.isActive()) {
	// tx.rollback();
	// }
	// pm.close();
	// }
	// }

	public void delete(Key id) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query q = pm.newQuery(AnalysisResultDAO.class);
			q.setFilter("id==pId");
			q.declareParameters(Key.class.getName() + " pId");
			@SuppressWarnings("unchecked")
			Collection<GAEJDOAnalysisResult> ars = (Collection<GAEJDOAnalysisResult>) q
					.execute(id);
			if (ars.size() != 1)
				throw new IllegalStateException("Expected 1 but got "
						+ ars.size());
			pm.deletePersistent(ars.iterator().next());
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void makeTransient(GAEJDOAnalysisResult analysisResult) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makeTransient(analysisResult);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}
}
