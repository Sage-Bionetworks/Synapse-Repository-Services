package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

abstract public class GAEJDORevisableAnnotatableDAOHelper<S, T extends GAEJDORevisable<T> & GAEJDOAnnotatable> extends
	GAEJDOAnnotatableDAOHelper<S,T> {

	protected List<T> getHavingAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			Class valueClass, 
			Object value,
			int start,
			int end) {
		Query query = pm.newQuery(getJdoClass());
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
		Query query = pm.newQuery(getJdoClass());
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
	
	public  Collection getAnnotationValues(T owner, String attribute, Class annotationClass) {
		if (annotationClass.equals(String.class)) {
			Collection<String> c = new HashSet<String>();
			for (GAEJDOStringAnnotation annot: owner.getAnnotations().getStringAnnotations()) {
				if (annot.getAttribute().equals(attribute)) c.add(annot.getValue());
			}
			return c;
		} else {
			throw new RuntimeException("Unexpected type "+annotationClass);
		}
	}

		
}
