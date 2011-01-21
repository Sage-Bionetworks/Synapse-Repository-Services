package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;

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
abstract public class GAEJDOAnnotationDAOImpl<S extends Base, T extends GAEJDOAnnotatable & GAEJDOBase, A extends Comparable<A>>
	implements AnnotationDAO<S, A> {
	
	// These methods are to be made concrete for particular types of annotations
	abstract protected Class<? extends GAEJDOAnnotation<A>> getAnnotationClass();
	abstract protected Class<A> getValueClass();
	//abstract protected GAEJDOAnnotation<A> newAnnotation(String attribute, A value);
	// this is the name of the Set field in the GAEJDOAnnotations object
	abstract protected String getCollectionName();
	abstract protected void addAnnotation(GAEJDOAnnotations annots, String attr, A value);
	abstract protected void removeAnnotation(GAEJDOAnnotations annots, String attr, A value);
	abstract protected Iterable<GAEJDOAnnotation<A>> getIterable(GAEJDOAnnotations annots);
	//abstract protected Collection<Key> getAnnotations(GAEJDOAnnotations annots);
	
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
	 * @throws InvalidModelException 
	 */
	abstract public void copyFromDto(S dto, T jdo) throws InvalidModelException;
	

	
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
		Query query = pm.newQuery(GAEJDOAnnotations.class);
		query.setFilter("this."+collectionName+".contains(vAnnotation) && "+
				"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
		query.declareVariables(annotationClass.getName()+" vAnnotation");
		query.declareParameters(String.class.getName()+" pAttrib, "+valueClass.getName()+" pValue");
		@SuppressWarnings("unchecked")
		List<GAEJDOAnnotations> annots = (List<GAEJDOAnnotations>)query.execute(attrib, value);	
		// if annots is empty then no one has the given annotation, so just return 'false'
		if (annots.isEmpty()) return false;
		// Now do a query for JDOs that 'own' the GAEJDOAnnotations class(es)
		Query ownerQuery = pm.newQuery(getOwnerClass());
		ownerQuery.setFilter("this.id==pId && pAnnots.contains(this.annotations)");
		ownerQuery.declareParameters(Key.class.getName()+" pId, "+List.class.getName()+" pAnnots");
		@SuppressWarnings("unchecked")
		List<T> ans = (List<T>)query.execute(owner.getId(), annots);	
		if (ans.size()>1) throw new IllegalStateException("Expected 0 or 1 result but got "+ans.size());
		return ans.size()>0;
	}
	 
	 protected Collection<GAEJDOAnnotations> getAnnotationsHaving(
				PersistenceManager pm,
				String attrib,
				String collectionName,
				Class annotationClass,
				Class valueClass, 
				Object value) {
			Query query = pm.newQuery(GAEJDOAnnotations.class);
			query.setFilter("this."+collectionName+".contains(vAnnotation) && "+
				"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(annotationClass.getName()+" vAnnotation");
			query.declareParameters(String.class.getName()+" pAttrib, "+valueClass.getName()+" pValue");
			@SuppressWarnings("unchecked")
			Collection<GAEJDOAnnotations> ans = (Collection<GAEJDOAnnotations>)query.execute(attrib, value);	
			return ans;	
	 }
	
	 // NOTE: a different implementation is needed for Revisable objects.  The implementation is found in the
	 // subclass GAEJDORevisableAnnotationDAOImpl
	 protected List<T> getHavingAnnotation(
				PersistenceManager pm,
				String attrib,
				String collectionName,
				Class annotationClass,
				Class valueClass, 
				Object value,
				int start,
				int end) {
			Collection<GAEJDOAnnotations> annots = getAnnotationsHaving(pm, attrib, collectionName, annotationClass, valueClass, value);
			List<T> ans = new ArrayList<T>();
			// Now do a query for JDOs that 'own' the GAEJDOAnnotations class(es)
			if (annots.size()>0) {
				Query ownerQuery = pm.newQuery(getOwnerClass());
				ownerQuery.setFilter("this.annotations==vAnnotations && vAnnotations.id==pId");
				ownerQuery.declareVariables(GAEJDOAnnotations.class.getName()+" vAnnotations");
				ownerQuery.declareParameters(Key.class.getName()+" pId");
				List<T> owners = new ArrayList<T>();
				for (GAEJDOAnnotations a: annots) {
					@SuppressWarnings("unchecked")
					Collection<T> c = (Collection<T>)ownerQuery.execute(a.getId());
					if (c.size()>1) throw new IllegalStateException();
					if (c.size()==1) owners.add(c.iterator().next());
				}
				for (int i=start; i<end && i<owners.size(); i++) ans.add(owners.get(i));
			}
			return ans;
		}
				
		private Collection<A> getAnnotationValues(GAEJDOAnnotations annots, String attr) {
			Collection<A> ans = new HashSet<A>();
			for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
				if (annot.getAttribute().equals(attr)) ans.add(annot.getValue());
			}
			return ans;
		}
		
		 // NOTE: a different implementation is needed for Revisable objects.  The implementation is found in the
		 // subclass GAEJDORevisableAnnotationDAOImpl
		protected Collection<T> getAllOwners(PersistenceManager pm) {
			Query ownerQuery = pm.newQuery(getOwnerClass());
			@SuppressWarnings("unchecked")
			List<T> owners = ((List<T>)ownerQuery.execute());
			return owners;
		}

		private List<T> getSortedByAnnotation(
				PersistenceManager pm,
				String attrib,
				String collectionName,
				Class annotationClass,
				int start,
				int end,
				final boolean asc) {
			// first, get all the owners
			List<T> owners = new ArrayList(getAllOwners(pm));
			
			// now we map the owner objects to their attribute values
			final Map<Key, A> ownerValueMap = new HashMap<Key, A>();
			for (T owner: owners) {
				Collection<A> annotCollection = getAnnotationValues(owner.getAnnotations(), attrib);
				A extrValue = null;
				// find the smallest/largest (depending on whether we're sorting ascending/descending
				for (A value : annotCollection) {
					if (extrValue==null || ((asc && extrValue.compareTo(value)>0) || (!asc && extrValue.compareTo(value)<0))) extrValue=value;
				}
				ownerValueMap.put(owner.getId(), extrValue);
			}
			
			// now sort owners
			// nulls always go first, otherwise ascending/descending depending on input param
			Collections.sort(owners, new Comparator<T>() {
				public int compare(T o1, T o2) {
					A v1 = ownerValueMap.get(o1);
					A v2 = ownerValueMap.get(o2);
					if (v1==null && v2==null) return 0;
					if (v1==null) return -1;
					if (v2==null) return 1;
					return ((asc ? 1 : -1)*v1.compareTo(v2));
				}
			});
			List<T> ans = new ArrayList<T>();
			for (int i=start; i<end && i<owners.size(); i++) ans.add(owners.get(i));
			return ans;
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
			addAnnotation(annots, attribute, value);
			pm.makePersistent(jdo);
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
			removeAnnotation(annots, attribute, value);
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
	 * @return all the annotations belonging to the given object
	 */

	public Map<String,Collection<A>> getAnnotations(String id) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T)pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			Map<String,Collection<A>> ans = new HashMap<String,Collection<A>>();
			for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
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
			for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
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
				for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
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
	
	public List<S> getInRangeSortedBy(int start, int end, String sortByAttr, boolean asc) throws DatastoreException {
		PersistenceManager pm = PMF.get();	
		try {
			return copyToDtoList(getSortedByAnnotation(
					pm,
					sortByAttr,
					getCollectionName(),
					getAnnotationClass(),
					start,
					end,
					asc));
		} finally {
			pm.close();
		}			
		
	}
	

}
