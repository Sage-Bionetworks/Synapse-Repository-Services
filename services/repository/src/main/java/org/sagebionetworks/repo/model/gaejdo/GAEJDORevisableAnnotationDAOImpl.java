package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Base;

import com.google.appengine.api.datastore.Key;

abstract public class GAEJDORevisableAnnotationDAOImpl<S extends Base, T extends GAEJDOAnnotatable & GAEJDOBase, A>
	extends GAEJDOAnnotationDAOImpl<S, T, A> {

	 protected boolean hasAnnotation(
				PersistenceManager pm,
				T owner,
				String attrib,
				String collectionName,
				Class annotationClass,
				Class valueClass, 
				Object value) {
			Query query = pm.newQuery(getOwnerClass());
			String filterString = ("this.id==pId && annotations==vAnnotations && vAnnotations."+
					collectionName+".contains(vAnnotation) && "+
					"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue &&"+
					" revision==r && r.latest==true");
//			System.out.println(filterString);
			query.setFilter(filterString);
			String variableString = (GAEJDOAnnotations.class.getName()+" vAnnotations; "+
					annotationClass.getName()+" vAnnotation; "+
					GAEJDORevision.class.getName()+" r");
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
		Query query = pm.newQuery(getOwnerClass());
		String filterString = ("annotations==vAnnotations && vAnnotations."+
				collectionName+".contains(vAnnotation) && "+
				"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue"+
				" revision==r && r.latest==true");
		query.setFilter(filterString);
		String variableString = (GAEJDOAnnotations.class.getName()+" vAnnotations; "+
				annotationClass.getName()+" vAnnotation; "+
				GAEJDORevision.class.getName()+" r");
//		System.out.println(variableString);
		query.declareVariables(variableString);
		query.declareParameters(String.class.getName()+" pAttrib, "
				+valueClass.getName()+" pValue");
		query.setRange(start, end);
		@SuppressWarnings("unchecked")
		List<T> ans = (List<T>)query.execute(attrib, value);	
		return ans;
	}

	protected List<T> getSortedByAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			int start,
			int end) {
		Query query = pm.newQuery(getOwnerClass());
		String filterString = ("annotations==vAnnotations && vAnnotations."+
				collectionName+".contains(vAnnotation) && "+
				"vAnnotation.attribute==pAttrib"+
				" revision==vRevision && vRevision.latest==true");
		query.setFilter(filterString);
		String variableString = (GAEJDOAnnotations.class.getName()+" vAnnotations; "+
				annotationClass.getName()+" vAnnotation; "+
				GAEJDORevision.class.getName()+" vRevision");
		query.declareVariables(variableString);
		query.declareParameters(String.class.getName()+" pAttrib");
		@SuppressWarnings("unchecked")
		// since we can't sort by attribute value, we can't limit the query to a range
		// therefore we must retrieve everything, then get the sub-list [start, end)
		List<T> all = (List<T>)query.execute(attrib);
		List<T> range = new ArrayList<T>();
		return range;
	}
	
}
