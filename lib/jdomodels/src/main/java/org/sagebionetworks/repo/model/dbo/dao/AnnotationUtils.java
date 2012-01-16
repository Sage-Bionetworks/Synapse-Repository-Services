package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.persistence.DBODateAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoubleAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOLongAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStringAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AnnotationUtils {
	
	/**
	 * Create a list of DBOStringAnnotation from the given map.
	 * @param owner
	 * @param map
	 * @return
	 */
	public static List<DBOStringAnnotation> createStringAnnotations(Long owner, Map<String, Collection<String>> map){
		List<DBOStringAnnotation> results = new ArrayList<DBOStringAnnotation>();
		if(map != null){
			Iterator<String> keyIt = map.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<String> valueColection = map.get(key);
				Iterator<String> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					String value = valueIt.next();
					// Note: The annotation tables are not used for persistence, we only put data in them to support queries.
					// Annotations are persisted in in a blob on the revision table.
					// Therefore, it is safe to trim string data when it is too long.
					String stringValue = (String) value;
					if((null != stringValue) && stringValue.length() > SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH){
						stringValue = stringValue.substring(0, SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH-1);
						value = stringValue;
					}
					DBOStringAnnotation anno = new DBOStringAnnotation();
					anno.setOwner(owner);
					anno.setAttribute(key);
					anno.setValue(value);
					results.add(anno);
				}
			}
		}
		return results;
	}

	/**
	 * Create a list of DBOLongAnnotation from the given map.
	 * @param ownerId
	 * @param map
	 * @return
	 */
	public static List<DBOLongAnnotation> createLongAnnotations(Long ownerId, Map<String, Collection<Long>> map) {
		List<DBOLongAnnotation> results = new ArrayList<DBOLongAnnotation>();
		if(map != null){
			Iterator<String> keyIt = map.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<Long> valueColection = map.get(key);
				Iterator<Long> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					Long value = valueIt.next();
					DBOLongAnnotation anno = new DBOLongAnnotation();
					anno.setOwner(ownerId);
					anno.setAttribute(key);
					anno.setValue(value);
					results.add(anno);
				}
			}
		}
		return results;
	}

	/**
	 * Create the double annotations.
	 * @param ownerId
	 * @param map
	 * @return
	 */
	public static List<DBODoubleAnnotation> createDoubleAnnotations(Long ownerId, Map<String, Collection<Double>> map) {
		List<DBODoubleAnnotation> results = new ArrayList<DBODoubleAnnotation>();
		if(map != null){
			Iterator<String> keyIt = map.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<Double> valueColection = map.get(key);
				Iterator<Double> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					Double value = valueIt.next();
					DBODoubleAnnotation anno = new DBODoubleAnnotation();
					anno.setOwner(ownerId);
					anno.setAttribute(key);
					anno.setValue(value);
					results.add(anno);
				}
			}
		}
		return results;
	}

	/**
	 * create the date annotations.
	 * @param ownerId
	 * @param map
	 * @return
	 */
	public static List<DBODateAnnotation> createDateAnnotations(Long ownerId, Map<String, Collection<Date>> map) {
		List<DBODateAnnotation> results = new ArrayList<DBODateAnnotation>();
		if(map != null){
			Iterator<String> keyIt = map.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<Date> valueColection = map.get(key);
				Iterator<Date> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					Date value = valueIt.next();
					DBODateAnnotation anno = new DBODateAnnotation();
					anno.setOwner(ownerId);
					anno.setAttribute(key);
					anno.setValue(value);
					results.add(anno);
				}
			}
		}
		return results;
	}

}
