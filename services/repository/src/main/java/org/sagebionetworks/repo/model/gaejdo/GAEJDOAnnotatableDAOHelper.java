package org.sagebionetworks.repo.model.gaejdo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;

import com.google.appengine.api.datastore.Text;

/**
 * 
 * @author bhoff
 *
 * @param <S> the DTO class
 * @param <T> the GAEJDO class
 */
abstract public class GAEJDOAnnotatableDAOHelper<S, T extends GAEJDOAnnotatable> {
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

	private static final int MAX_STRING_LENGTH = 500;

//	public List<T> getInRangeHaving(PersistenceManager pm, int start, int end, String attribute, Object value) {
//		Class objClass = value.getClass();
//		if (objClass.equals(String.class)) {
//			String sValue = (String)value;
//			if (sValue.length()<MAX_STRING_LENGTH) {
//				return getHavingStringAnnotation(pm, attribute, sValue, start, end);
//			} else {
//				return getHavingTextAnnotation(pm, attribute, new Text(sValue), start, end);
//				
//			}
//		} else if (objClass.equals(Integer.class)) {
//			return getHavingIntegerAnnotation(pm, attribute, (Integer)value, start, end);
//		} else if (objClass.equals(Float.class)) {
//			return getHavingFloatAnnotation(pm, attribute, (Float)value, start, end);
//		} else if (objClass.equals(Boolean.class)) {
//			return getHavingBooleanAnnotation(pm, attribute, (Boolean)value, start, end);
//		} else if (objClass.equals(Date.class)) {
//			return getHavingDateAnnotation(pm, attribute, (Date)value, start, end);
//		} else {
//			throw new RuntimeException("Unexpected value type "+objClass);
//		}
//	}
	
	public List<S> copyToDtoList(List<T> jdoList) {
		List<S> ans = new ArrayList<S>();
		for (T jdo: jdoList) {
			S dto = newDTO();
			copyToDto(jdo, dto);
			ans.add(dto);
		}
		return ans;
	}
	
	public List<S> getHavingStringAnnotation(PersistenceManager pm, String attrib, String value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "stringAnnotations", GAEJDOStringAnnotation.class, String.class, value, start, end));
	}
	
	public List<S> getHavingIntegerAnnotation(PersistenceManager pm, String attrib, Integer value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "integerAnnotations", GAEJDOIntegerAnnotation.class, Integer.class, value, start, end));
	}
	
	public List<S> getHavingTextAnnotation(PersistenceManager pm, String attrib, Text value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "textAnnotations", GAEJDOTextAnnotation.class, Text.class, value, start, end));
	}
	
	public List<S> getHavingBooleanAnnotation(PersistenceManager pm, String attrib, Boolean value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "booleanAnnotations",GAEJDOBooleanAnnotation.class, Boolean.class, value, start, end));
	}
	
	public List<S> getHavingFloatAnnotation(PersistenceManager pm, String attrib, Float value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "floatAnnotations", GAEJDOFloatAnnotation.class, Float.class, value, start, end));
	}
	
	public List<S> getHavingDateAnnotation(PersistenceManager pm, String attrib, Date value, int start, int end) {
		return copyToDtoList(getHavingAnnotation(pm, attrib, "dateAnnotations", GAEJDODateAnnotation.class, Date.class, value, start, end));
	}
	
	abstract protected List<T> getHavingAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			Class valueClass, 
			Object value,
			int start,
			int end);
	
	abstract protected List<T> getSortedByAnnotation(
			PersistenceManager pm,
			String attrib,
			String collectionName,
			Class annotationClass,
			int start,
			int end);	
}
