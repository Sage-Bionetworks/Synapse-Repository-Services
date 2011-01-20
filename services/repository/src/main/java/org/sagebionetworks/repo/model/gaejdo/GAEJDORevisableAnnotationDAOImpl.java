package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.Base;

abstract public class GAEJDORevisableAnnotationDAOImpl<S extends Base, T extends GAEJDOAnnotatable & GAEJDOBase, A>
	extends GAEJDOAnnotationDAOImpl<S, T, A> {

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
//		System.out.println(filterString);
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
