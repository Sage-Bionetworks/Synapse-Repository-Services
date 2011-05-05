package org.sagebionetworks.repo.model.jdo;

import org.sagebionetworks.repo.model.DatasetAnalysisDAO;



public class JDODatasetAnalysisDAOImpl implements DatasetAnalysisDAO {
//	public JDODatasetAnalysis getDatasetAnalysis(Long id) {
//		PersistenceManager pm = PMF.get();
//		JDODatasetAnalysis ans = (JDODatasetAnalysis) pm.getObjectById(
//				JDODatasetAnalysis.class, id);
//		// pm.close();
//		return ans;
//	}
//
//	public void makePersistent(JDODatasetAnalysis datasetAnalysis) {
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
//			pm.makePersistent(datasetAnalysis);
//			tx.commit();
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}
//
//	public void delete(JDODatasetAnalysis datasetAnalysis) {
//		PersistenceManager pm = PMF.get();
//		Transaction tx = null;
//		try {
//			tx = pm.currentTransaction();
//			tx.begin();
//			pm.deletePersistent(datasetAnalysis);
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
//			Query q = pm.newQuery(JDODatasetAnalysis.class);
//			q.setFilter("id==pId");
//			q.declareParameters(Long.class.getName() + " pId");
//			@SuppressWarnings("unchecked")
//			Collection<JDODatasetAnalysis> analyses = (Collection<JDODatasetAnalysis>) q
//					.execute(id);
//			if (analyses.size() != 1)
//				throw new IllegalStateException("Expected 1 but got "
//						+ analyses.size());
//			pm.deletePersistent(analyses.iterator().next());
//			tx.commit();
//		} finally {
//			if (tx.isActive()) {
//				tx.rollback();
//			}
//			pm.close();
//		}
//	}

}
