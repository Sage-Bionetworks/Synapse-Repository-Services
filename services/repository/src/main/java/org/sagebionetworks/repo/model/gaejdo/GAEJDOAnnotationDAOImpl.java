package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * 
 * This class manages annotations of type T.
 * @author bhoff
 *
 * @param <T> the annotatable object type
 * @param <A> the annotation value type (String, Boolean, Float, Date, Integer)
 */
abstract public class GAEJDOAnnotationDAOImpl<S extends Base, T extends GAEJDOAnnotatable & GAEJDOBase, A>
	implements AnnotationDAO<S, A> {
	
	// These methods are to be made concrete for particular types of annotations
	abstract protected Class<? extends GAEJDOAnnotation<A>> getAnnotationClass();
	abstract protected Class<A> getValueClass();
	abstract protected GAEJDOAnnotation<A> newAnnotation(String attribute, A value);
	// this is the name of the Set field in the GAEJDOAnnotations object
	abstract protected String getCollectionName();
	abstract protected Set<GAEJDOAnnotation<A>> getAnnotationSet(GAEJDOAnnotations annots);
	
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
	

	
	// these methods are to be made concrete for particular owner classes
	// in particular, the JDO queries are different for revisable vs. non-revisable classes
	abstract protected Class<T> getOwnerClass();
	/**
	 * Check whether an object has a given annotation
	 * @param pm
	 * @param owner
	 * @param attrib
	 * @param collectionName
	 * @param annotationClass
	 * @param valueClass
	 * @param value
	 * @return
	 */
	 protected boolean hasAnnotation(
			PersistenceManager pm,
			T owner,
			String attrib,
			String collectionName,
			Class annotationClass,
			Class valueClass, 
			Object value) {
		Query query = pm.newQuery(getOwnerClass());
		String filterString = ("id==pId && annotations==vAnnotations && vAnnotations."+
				collectionName+".contains(vAnnotation) && "+
				"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
		query.setFilter(filterString);
		String variableString = (GAEJDOAnnotations.class.getName()+" vAnnotations; "+
				annotationClass.getName()+" vAnnotation");
		query.declareVariables(variableString);
		query.declareParameters(
				Key.class.getName()+" pId, "
				+String.class.getName()+" pAttrib, "
				+valueClass.getName()+" pValue");
		@SuppressWarnings("unchecked")
		List<T> ans = (List<T>)query.execute(owner.getId(), attrib, value);	
		if (ans.size()>1) throw new IllegalStateException("Expected 0 or 1 result but got "+ans.size());
		return ans.size()>0;
	}
	
	protected List<T> getHavingAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			Class valueClass, 
			Object value,
			int start,
			int end) {
		// TODO
		throw new RuntimeException("Not yet implemented");
		
	}
	
	protected List<T> getSortedByAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			int start,
			int end) {
		// TODO
		throw new RuntimeException("Not yet implemented");
		
	}
	

	
	public void addAnnotation(String id, String attribute, A value) throws DatastoreException  {
		PersistenceManager pm = PMF.get();	
		Transaction tx=null;
		try {
		 	tx=pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T)pm.getObjectById(getOwnerClass(), key);
			// check whether already have this annotation
			if (hasAnnotation(pm, jdo, attribute, value)) return;
			GAEJDOAnnotations annots = jdo.getAnnotations();
			getAnnotationSet(annots).add(newAnnotation(attribute, value));
			tx.commit();
		} finally {
			if(tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}	
	}
	
	public boolean hasAnnotation(PersistenceManager pm, T jdo, String attribute, A value) {
		return hasAnnotation(pm, jdo, attribute, getCollectionName(), getAnnotationClass(), getValueClass(), value);
	}
	
	public void removeAnnotation(String id, String attribute, A value) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		Transaction tx=null;
		try {
		 	tx=pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T)pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			getAnnotationSet(annots).remove(newAnnotation(attribute, value));
			tx.commit();
		} finally {
			if(tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}	
	}
	
	/**
	 * @param id the id of the 'Attributable' owner object
	 * @return all the annotations of 
	 */
	public Map<String,Collection<A>> getAnnotations(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T)pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			Map<String,Collection<A>> ans = new HashMap<String,Collection<A>>();
			for (GAEJDOAnnotation<A> annot : getAnnotationSet(annots)) {
				Collection<A> values = ans.get(annot.getAttribute());
				if (values==null) {
					values=new HashSet<A>();
					ans.put(annot.getAttribute(),values);
				}
				values.add(annot.getValue());
			}
			return ans;
		} finally {
			pm.close();
		}					
	}

	
	public Collection<A> getAnnotations(String id, String attribute) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T)pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			Collection<A> ans = new HashSet<A>();
			for (GAEJDOAnnotation<A> annot : getAnnotationSet(annots)) {
				if (annot.getAttribute().equals(attribute)) ans.add(annot.getValue());
			}
			return ans;
		} finally {
			pm.close();
		}			
	}
	
	/**
	 * 
	 * @return a set of all attribute names for annotations of type 'A' used in objects of type 'T'
	 * @throws DatastoreException
	 */
	public Collection<String> getAttributes() throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Collection<String> ans = new HashSet<String>();
			Extent<T> extent = pm.getExtent(getOwnerClass());
			for (T jdo : extent) {
				GAEJDOAnnotations annots = jdo.getAnnotations();
				for (GAEJDOAnnotation<A> annot : getAnnotationSet(annots)) {
					ans.add(annot.getAttribute());
				}
			}
			return ans;
		} finally {
			pm.close();
		}			
	}
	
	public List<S> copyToDtoList(List<T> jdoList) {
		List<S> ans = new ArrayList<S>();
		for (T jdo: jdoList) {
			S dto = newDTO();
			copyToDto(jdo, dto);
			ans.add(dto);
		}
		return ans;
	}

	

	public List<S> getHavingAnnotation(PersistenceManager pm, String attrib, String value, int start, int end) {
		return copyToDtoList(
				getHavingAnnotation(
						pm, 
						attrib, 
						getCollectionName(), 
						getAnnotationClass(), 
						getValueClass(), 
						value, 
						start, 
						end));
	}
	

	
	public List<S> getInRangeHaving(int start, int end, String attribute, A value) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			return copyToDtoList(getHavingAnnotation(
					pm,
					attribute,
					getCollectionName(),
					getAnnotationClass(),
					getValueClass(), 
					value,
					start,
					end));
		} finally {
			pm.close();
		}			
	}
	
	public List<S> getInRangeSortedBy(int start, int end, String sortByAttr) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			return copyToDtoList(getSortedByAnnotation(
					pm,
					sortByAttr,
					getCollectionName(),
					getAnnotationClass(),
					start,
					end));
		} finally {
			pm.close();
		}			
		
	}
	

}
