package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ProjectDAO;

import com.google.appengine.api.datastore.Key;

/**
 * This is the Data Access Object class for the Project class, using GAE-JDO
 * 
 * @author bhoff
 * 
 */
public class GAEJDOProjectDAOImpl implements ProjectDAO {

	public GAEJDOProject getProject(Key id) {
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

	public void delete(Key id) {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Query q = pm.newQuery(GAEJDOProject.class);
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

	@Override
	public String create(Object dto) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(String id) throws DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Object r) throws DatastoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(String id) throws DatastoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getCount() throws DatastoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List getInRange(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection getPrimaryFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

}
