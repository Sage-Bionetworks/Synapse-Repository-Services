package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * 
 * @author bhoff
 *
 * @param <S> the DTO class
 * @param <T> the JDO class
 */
abstract public class GAEJDOBaseDAOHelper<S,T extends GAEJDOBase> {
	
	abstract public S newDTO();
	
	abstract public T newJDO();
	
	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 */
	abstract public void copyToDto(T jdo, S dto);
	
	
	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 */
	abstract public void copyFromDto(S dto, T jdo);
	
	/**
	 * @param jdoClass the class parameterized by T
	 */
	abstract public Class<T> getJdoClass();

	
	public String create(S dto) {
		T jdo = newJDO();
		copyFromDto(dto, jdo);
		PersistenceManager pm = PMF.get();		
				Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				pm.makePersistent(jdo);
				tx.commit();
				return KeyFactory.keyToString(jdo.getId());
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	
	public S get(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Key key = KeyFactory.stringToKey(id);
			@SuppressWarnings("unchecked")
			T jdo = (T)pm.getObjectById(getJdoClass(), key);
			S dto = newDTO();
			copyToDto(jdo, dto);
			pm.close();
			return dto;
		} catch (Exception e) {
			throw new DatastoreException(e);
		}
	}
	
	public void delete(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();		
		Transaction tx=null;
		try {
			 	tx=pm.currentTransaction();
				tx.begin();
				Key key = KeyFactory.stringToKey(id);
				@SuppressWarnings("unchecked")
				T jdo = (T)pm.getObjectById(key);
				pm.deletePersistent(jdo);
				tx.commit();
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
				if(tx.isActive()) {
					tx.rollback();
				}
				pm.close();
		}	
	}
	

	/**
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 */
	public List<S> getInRange(int start, int end) {
		PersistenceManager pm = PMF.get();		
		try {
			Query query = pm.newQuery(getJdoClass());
			query.setRange(start, end);
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>)query.execute());
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
			}
			return ans;
		} finally {
			pm.close();
		}
	}
		
	/**
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 * and sorted by the given primary field
	 */
	public List<S> getInRangeSortedByPrimaryField(int start, int end, String sortBy, boolean asc) {
		PersistenceManager pm = null;		
		try {
			pm = PMF.get();		
			Query query = pm.newQuery(getJdoClass());
			query.setRange(start, end);
			query.setOrdering(sortBy+(asc?" ascending":" descending"));
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>)query.execute());
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
			}
			return ans;
		} finally {
			pm.close();
		}
		
	}
	
	public List<S> getInRangeHavingPrimaryField(int start, int end, String attribute, Object value)  {
		PersistenceManager pm = null;		
		try {
			pm = PMF.get();		
			Query query = pm.newQuery(getJdoClass());
			query.setRange(start, end);
			query.setFilter(attribute+"==pValue");
			query.declareParameters(value.getClass()+" pValue");
			@SuppressWarnings("unchecked")
			List<T> list = ((List<T>)query.execute(value));
			List<S> ans = new ArrayList<S>();
			for (T jdo : list) {
				S dto = newDTO();
				copyToDto(jdo, dto);
				ans.add(dto);
			}
			return ans;
		} finally {
			pm.close();
		}
	}
		
	

}
