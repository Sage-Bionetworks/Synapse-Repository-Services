package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.Date;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.sagebionetworks.repo.model.AnnotationsAccessor;

import com.google.appengine.api.datastore.Text;


public class AnnotationsAccessorImpl<T extends Annotatable> implements AnnotationsAccessor<T> {
	private Class<T> annotatableClass;
	private PersistenceManager pm = null;

	public AnnotationsAccessorImpl(Class<T> annotatableClass) {
		this.annotatableClass = annotatableClass;
	}
	
	public Collection<T> getHavingStringAnnotation(String attrib, String value) {
		return getHavingAnnotation(attrib, "stringAnnotations", GAEJDOStringAnnotation.class, String.class, value);
	}
	
	public Collection<T> getHavingIntegerAnnotation(String attrib, Integer value) {
		return getHavingAnnotation(attrib, "integerAnnotations", GAEJDOIntegerAnnotation.class, Integer.class, value);
	}
	
	public Collection<T> getHavingTextAnnotation(String attrib, Text value) {
		return getHavingAnnotation(attrib, "textAnnotations", GAEJDOTextAnnotation.class, Text.class, value);
	}
	
	public Collection<T> getHavingBooleanAnnotation(String attrib, Boolean value) {
		return getHavingAnnotation(attrib, "booleanAnnotations",GAEJDOBooleanAnnotation.class, Boolean.class, value);
	}
	
	public Collection<T> getHavingFloatAnnotation(String attrib, Float value) {
		return getHavingAnnotation(attrib, "floatAnnotations", GAEJDOFloatAnnotation.class, Float.class, value);
	}
	
	public Collection<T> getHavingDateAnnotation(String attrib, Date value) {
		return getHavingAnnotation(attrib, "dateAnnotations", GAEJDODateAnnotation.class, Date.class, value);
	}
	
	private Collection<T> getHavingAnnotation(
			String attrib,
			String collectionName,
			Class annotationClass,
			Class valueClass, 
			Object value) {
		if (pm==null) pm = PMF.get();
		Query query = pm.newQuery(annotatableClass);
//		String filterString = ("annotations==vAnnotations && vAnnotations."+
//				collectionName+".contains(vAnnotation) && "+
//				"vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
//		System.out.println(filterString);
//		query.setFilter(filterString);
//		String variableString = (Annotations.class.getName()+" vAnnotations; "+
//				annotationClass.getName()+" vAnnotation");
//		System.out.println(variableString);
//		query.declareVariables(variableString);
		query.declareParameters(String.class.getName()+" pAttrib, "
				+valueClass.getName()+" pValue");
		@SuppressWarnings("unchecked")
		Collection<T> ans = (Collection<T>)query.execute(attrib, value);	
		//pm.close();
		return ans;
	}

	public void close() {
		if (pm!=null) {
			pm.close();
			pm=null;
		}
	}
}
