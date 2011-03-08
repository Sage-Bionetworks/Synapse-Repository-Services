package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Base;



/**
 * This extension to JDOAnnotationDAOImpl customizes the JDO queries for
 * retrieval of annotations when only the *latest* revision of each annotated
 * object is to be retrieved.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            the data transfer object type
 * @param <T>
 *            the persistent object type
 * @param <A>
 *            the annotation value type
 */
abstract public class JDORevisableAnnotationDAOImpl<S extends Base, T extends JDOAnnotatable & JDOBase & JDORevisable, A extends Comparable<A>>
		extends JDOAnnotationDAOImpl<S, T, A> {

	/**
	 * limit search to the latest revision
	 */
	protected List<T> getHavingAnnotation(PersistenceManager pm, String attrib,
			String collectionName, Class annotationClass, Class valueClass,
			Object value, int start, int end) {
		Collection<JDOAnnotations> annots = getAnnotationsHaving(pm, attrib,
				collectionName, annotationClass, valueClass, value);
		List<T> ans = new ArrayList<T>();
		// Now do a query for JDOs that 'own' the JDOAnnotations class(es)
		if (annots.size() > 0) {
			Query ownerQuery = pm.newQuery(getOwnerClass());
			ownerQuery
					.setFilter("this.annotations==vAnnotations && vAnnotations.id==pId");
			ownerQuery.declareVariables(JDOAnnotations.class.getName()
					+ " vAnnotations");
			ownerQuery.declareParameters(Long.class.getName() + " pId");
			List<T> owners = new ArrayList<T>();
			for (JDOAnnotations a : annots) {
				@SuppressWarnings("unchecked")
				Collection<T> c = (Collection<T>) ownerQuery.execute(a.getId());
				if (c.size() > 1)
					throw new IllegalStateException();
				if (c.size() == 0)
					continue;
				T owner = c.iterator().next();
				if (owner.getRevision().getLatest() == true) {
					owners.add(owner);
				}
			}
			for (int i = start; i < end && i < owners.size(); i++)
				ans.add(owners.get(i));
		}
		return ans;
	}

	/**
	 * get all attribute owner objects, but only consider the latest revision
	 */
	protected Collection<T> getAllOwners(PersistenceManager pm) {
		Query ownerQuery = pm.newQuery(getOwnerClass());
		ownerQuery.setFilter("revision==vRevision && vRevision.latest==true");
		ownerQuery.declareVariables(JDORevision.class.getName()
				+ " vRevision");
		@SuppressWarnings("unchecked")
		List<T> owners = ((List<T>) ownerQuery.execute());
		return owners;
	}

}
