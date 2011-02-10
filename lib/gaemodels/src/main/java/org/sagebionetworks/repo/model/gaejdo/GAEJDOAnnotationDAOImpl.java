package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jdo.Extent;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * This is the DAO for managing the annotations of annotatable objects. It is
 * made concrete for particular object types and particular annotation value
 * types. Further, it can be extended to add complexity, e.g. for
 * revisable-annotatable types.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            the DTO type
 * @param <T>
 *            the JDO (persisted) type
 * @param <A>the annotation value type (String, Boolean, Float, Date, Integer)
 */
abstract public class GAEJDOAnnotationDAOImpl<S extends Base, T extends GAEJDOAnnotatable & GAEJDOBase, A extends Comparable<A>>
		implements AnnotationDAO<S, A> {
	
	abstract protected String getOwner();

	// These methods are to be made concrete for particular types of annotations

	abstract protected Class<? extends GAEJDOAnnotation<A>> getAnnotationClass();

	// we need this since we can't otherwise get the type of 'A' at runtime
	abstract protected Class<A> getValueClass();

	// this is the name of the Set field in the GAEJDOAnnotations object
	abstract protected String getCollectionName();

	abstract protected void addAnnotation(GAEJDOAnnotations annots,
			String attr, A value);

	abstract protected void removeAnnotation(GAEJDOAnnotations annots,
			String attr, A value);

	abstract protected Iterable<GAEJDOAnnotation<A>> getIterable(
			GAEJDOAnnotations annots);

	// -------------------------------------------------------------------

	/**
	 * Create a new instance of the data transfer object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return
	 */
	abstract public S newDTO();

	/**
	 * Create a new instance of the persistable object. Introducing this
	 * abstract method helps us avoid making assumptions about constructors.
	 * 
	 * @return
	 */
	abstract public T newJDO();

	/**
	 * Do a shallow copy from the JDO object to the DTO object.
	 * 
	 * @param jdo
	 * @param dto
	 * @throws DatastoreException 
	 */
	abstract public void copyToDto(T jdo, S dto) throws DatastoreException;

	/**
	 * Do a shallow copy from the DTO object to the JDO object.
	 * 
	 * @param dto
	 * @param jdo
	 * @throws InvalidModelException
	 */
	abstract public void copyFromDto(S dto, T jdo) throws InvalidModelException;

	abstract protected Class<T> getOwnerClass();

	/**
	 * 
	 * @param pm
	 * @param attrib
	 * @param collectionName
	 * @param annotationClass
	 * @param valueClass
	 * @param value
	 * @return the GAEJDOAnnotations objects having the given attribute/value
	 *         pair
	 */
	protected Collection<GAEJDOAnnotations> getAnnotationsHaving(
			PersistenceManager pm, String attrib, String collectionName,
			Class annotationClass, Class valueClass, Object value) {
		Query query = pm.newQuery(GAEJDOAnnotations.class);
		query.setFilter("this." + collectionName + ".contains(vAnnotation) && "
				+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
		query.declareVariables(annotationClass.getName() + " vAnnotation");
		query.declareParameters(String.class.getName() + " pAttrib, "
				+ valueClass.getName() + " pValue");
		@SuppressWarnings("unchecked")
		Collection<GAEJDOAnnotations> ans = (Collection<GAEJDOAnnotations>) query
				.execute(attrib, value);
		return ans;
	}

	/**
	 * Find the owner objects having the given annotations, subselecting a range
	 * of results.
	 * 
	 * NOTE: a different implementation is needed for Revisable objects. The
	 * implementation is found in the subclass GAEJDORevisableAnnotationDAOImpl
	 * 
	 */
	protected List<T> getHavingAnnotation(PersistenceManager pm, String attrib,
			String collectionName, Class annotationClass, Class valueClass,
			Object value, int start, int end) {
		Collection<GAEJDOAnnotations> annots = getAnnotationsHaving(pm, attrib,
				collectionName, annotationClass, valueClass, value);
		List<T> ans = new ArrayList<T>();
		// Now do a query for JDOs that 'own' the GAEJDOAnnotations class(es)
		if (annots.size() > 0) {
			Query ownerQuery = pm.newQuery(getOwnerClass());
			ownerQuery
					.setFilter("this.annotations==vAnnotations && vAnnotations.id==pId");
			ownerQuery.declareVariables(GAEJDOAnnotations.class.getName()
					+ " vAnnotations");
			ownerQuery.declareParameters(Key.class.getName() + " pId");
			List<T> owners = new ArrayList<T>();
			for (GAEJDOAnnotations a : annots) {
				@SuppressWarnings("unchecked")
				Collection<T> c = (Collection<T>) ownerQuery.execute(a.getId());
				if (c.size() > 1)
					throw new IllegalStateException();
				if (c.size() == 1)
					owners.add(c.iterator().next());
			}
			for (int i = start; i < end && i < owners.size(); i++)
				ans.add(owners.get(i));
		}
		return ans;
	}

	private Collection<A> getAnnotationValues(GAEJDOAnnotations annots,
			String attr) {
		Collection<A> ans = new HashSet<A>();
		for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
			if (annot.getAttribute().equals(attr))
				ans.add(annot.getValue());
		}
		return ans;
	}

	/**
	 * 
	 * NOTE: a different implementation is needed for Revisable objects. The
	 * implementation is found in the subclass GAEJDORevisableAnnotationDAOImpl
	 * 
	 * @return a collection of all objects of type T
	 */
	protected Collection<T> getAllOwners(PersistenceManager pm) {
		Query ownerQuery = pm.newQuery(getOwnerClass());
		@SuppressWarnings("unchecked")
		List<T> owners = ((List<T>) ownerQuery.execute());
		return owners;
	}

	/**
	 * 
	 * @param pm
	 * @param attrib
	 * @param collectionName
	 * @param annotationClass
	 * @param start
	 * @param end
	 * @param asc
	 * @return all objects of type 'T', sorted by the given annotation. Nulls
	 *         (owners not having the annotation) go first in the returned
	 *         list).
	 */
	private List<T> getSortedByAnnotation(PersistenceManager pm, String attrib,
			String collectionName, Class annotationClass, int start, int end,
			final boolean asc) {
		// first, get all the owners
		List<T> owners = new ArrayList<T>(getAllOwners(pm));

		// now we map the owner objects to their attribute values
		final Map<Key, A> ownerValueMap = new HashMap<Key, A>();
		for (T owner : owners) {
			Collection<A> annotCollection = getAnnotationValues(
					owner.getAnnotations(), attrib);
			A extrValue = null;
			// find the smallest/largest (depending on whether we're sorting
			// ascending/descending
			for (A value : annotCollection) {
				if (extrValue == null
						|| ((asc && extrValue.compareTo(value) > 0) || (!asc && extrValue
								.compareTo(value) < 0)))
					extrValue = value;
			}
			ownerValueMap.put(owner.getId(), extrValue);
		}

		// System.out.println("GAEJDOAnnotationDAOImpl: ownerValueMap: "+ownerValueMap);
		// System.out.println("GAEJDOAnnotationDAOImpl: Before sorting: "+owners);

		// now sort owners
		// nulls always go first, otherwise ascending/descending depending on
		// input param
		Collections.sort(owners, new Comparator<T>() {
			public int compare(T o1, T o2) {
				A v1 = ownerValueMap.get(o1.getId());
				A v2 = ownerValueMap.get(o2.getId());

				if (v1 == null && v2 == null)
					return 0;
				if (v1 == null)
					return -1;
				if (v2 == null)
					return 1;
				return ((asc ? 1 : -1) * v1.compareTo(v2));
			}
		});

		// System.out.println("GAEJDOAnnotationDAOImpl: After sorting: "+owners);

		List<T> ans = new ArrayList<T>();
		for (int i = start; i < end && i < owners.size(); i++)
			ans.add(owners.get(i));
		return ans;
	}

	protected boolean hasAnnotation(T jdo, String attribute, A value) {
		return getAnnotation(jdo, attribute, value)!=null;
	}
		
	protected GAEJDOAnnotation<A> getAnnotation(T jdo, String attribute, A value) {
		for (GAEJDOAnnotation<A> annot : getIterable(jdo.getAnnotations())) {
			if (annot.getAttribute().equals(attribute)
					&& annot.getValue().equals(value))
				return annot;
		}
		return null;
	}

	public void addAnnotation(String attribute, A value)
	throws DatastoreException, NotFoundException {
		addAnnotation(getOwner(), attribute, value);
	}
		
	public void addAnnotation(String id, String attribute, A value)
		throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T) pm.getObjectById(getOwnerClass(), key);
			// check whether already have this annotation
			if (hasAnnotation(jdo, attribute, value))
				return;
			GAEJDOAnnotations annots = jdo.getAnnotations();
			annots.toString(); // hack to 'touch' all the fields
			addAnnotation(annots, attribute, value);
			pm.makePersistent(jdo);
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public void removeAnnotation(String attribute, A value)
	throws DatastoreException, NotFoundException {
		removeAnnotation(getOwner(), attribute, value);
	}
	
	public void removeAnnotation(String id, String attribute, A value)
		throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T) pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotation<A> matchingAnnot = getAnnotation(jdo, attribute, value);
			if (matchingAnnot!=null) {
				removeAnnotation(jdo.getAnnotations(), attribute, value);
				// note: removing from the collection is not sufficient,
				// we also must explicitly delete it from the datastore
				pm.deletePersistent(matchingAnnot);
				pm.makePersistent(jdo);
			}
			tx.commit();
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			pm.close();
		}
	}

	public Map<String, Collection<A>> getAnnotations()
	throws DatastoreException, NotFoundException {
		return getAnnotations(getOwner());
	}
	
	/**
	 * @param id
	 *            the id of the 'Annotatable' owner object
	 * @return all the annotations belonging to the given object
	 * @throws NotFoundException
	 */

	public Map<String, Collection<A>> getAnnotations(String id)
			throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		try {
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T) pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			Map<String, Collection<A>> ans = new HashMap<String, Collection<A>>();
			for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
				Collection<A> values = ans.get(annot.getAttribute());
				if (values == null) {
					values = new HashSet<A>();
					ans.put(annot.getAttribute(), values);
				}
				values.add(annot.getValue());
			}
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * 
	 * @param id
	 * @param attribute
	 * @return all the annotations of the given object having the given
	 *         attribute
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Collection<A> getAnnotations(String id, String attribute)
			throws DatastoreException, NotFoundException {
		PersistenceManager pm = PMF.get();
		try {
			Key key = KeyFactory.stringToKey(id);
			T jdo = (T) pm.getObjectById(getOwnerClass(), key);
			GAEJDOAnnotations annots = jdo.getAnnotations();
			Collection<A> ans = new HashSet<A>();
			for (GAEJDOAnnotation<A> annot : getIterable(annots)) {
				if (annot.getAttribute().equals(attribute))
					ans.add(annot.getValue());
			}
			return ans;
		} catch (JDOObjectNotFoundException e) {
			throw new NotFoundException(e);
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * 
	 * @return a set of all attribute names for annotations of type 'A' used in
	 *         objects of type 'T'
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
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	public List<S> copyToDtoList(List<T> jdoList) throws DatastoreException {
		List<S> ans = new ArrayList<S>();
		for (T jdo : jdoList) {
			S dto = newDTO();
			copyToDto(jdo, dto);
			ans.add(dto);
		}
		return ans;
	}

	/**
	 * 
	 * @param start
	 * @param end
	 * @param attrib
	 * @param value
	 * @return all objects having the given annotation, paginated by the given
	 *         start and end.
	 */
	public List<S> getInRangeHaving(int start, int end, String attribute,
			A value) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			return copyToDtoList(getHavingAnnotation(pm, attribute,
					getCollectionName(), getAnnotationClass(), getValueClass(),
					value, start, end));
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}
	}

	/**
	 * @param start
	 * @param end
	 * @param sortByAttr
	 * @param asc
	 *            if true sort ascending, else sort descending
	 * @return a List of all the objects in the system, paginated by the given
	 *         start and end and ordered by the given attribute.
	 */
	public List<S> getInRangeSortedBy(int start, int end, String sortByAttr,
			boolean asc) throws DatastoreException {
		PersistenceManager pm = PMF.get();
		try {
			return copyToDtoList(getSortedByAnnotation(pm, sortByAttr,
					getCollectionName(), getAnnotationClass(), start, end, asc));
		} catch (Exception e) {
			throw new DatastoreException(e);
		} finally {
			pm.close();
		}

	}

}
