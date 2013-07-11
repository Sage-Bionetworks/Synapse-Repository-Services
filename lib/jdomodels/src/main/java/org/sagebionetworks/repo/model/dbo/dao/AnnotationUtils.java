package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.evaluation.dbo.DoubleAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.LongAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.StringAnnotationDBO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
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
	public static List<DBOStringAnnotation> createStringAnnotations(Long owner, Map<String, List<String>> map){
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
	public static List<DBOLongAnnotation> createLongAnnotations(Long ownerId, Map<String, List<Long>> map) {
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
	public static List<DBODoubleAnnotation> createDoubleAnnotations(Long ownerId, Map<String, List<Double>> map) {
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
	public static List<DBODateAnnotation> createDateAnnotations(Long ownerId, Map<String, List<Date>> map) {
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

	public static LongAnnotationDBO createLongAnnotationDBO(
			Long ownerId, Long ownerParentId, LongAnnotation anno) {
		LongAnnotationDBO dbo = new LongAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setValue(anno.getValue());
		dbo.setSubmissionId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate());
		return dbo;
	}

	public static StringAnnotationDBO createStringAnnotationDBO(
			Long ownerId, Long ownerParentId, AnnotationBase anno) {
		StringAnnotationDBO dbo = new StringAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setSubmissionId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate());
		
		// we must manually handle different typed Annos, since the AnnotationBase interface
		// does not specify the getValue() method
		if (anno instanceof StringAnnotation) {
			StringAnnotation sa = (StringAnnotation) anno;
			dbo.setValue(sa.getValue());
		} else if (anno instanceof DoubleAnnotation) {
			DoubleAnnotation da = (DoubleAnnotation) anno;
			dbo.setValue(da.getValue().toString());
		} else if (anno instanceof LongAnnotation) {
			LongAnnotation la = (LongAnnotation) anno;
			dbo.setValue(la.getValue().toString());
		} else {
			throw new IllegalArgumentException(
					"Unable to determine annotation type for key: " + anno.getKey());
		}
		
		return dbo;
	}

	public static DoubleAnnotationDBO createDoubleAnnotationDBO(
			Long ownerId, Long ownerParentId, DoubleAnnotation anno) {
		DoubleAnnotationDBO dbo = new DoubleAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setValue(anno.getValue());
		dbo.setSubmissionId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate());
		return dbo;
	}
	
	/**
	 * Ensure that Annotation keys are unique.
	 * 
	 * @param annos
	 */
	public static void validateAnnotations(Annotations annos) {
		if (annos == null) {
			throw new IllegalArgumentException("Annotations cannot be null");
		}
		Set<String> keys = new HashSet<String>();
		if (annos.getDoubleAnnos() != null) {
			checkAnnos(keys, annos.getDoubleAnnos());
		}
		if (annos.getLongAnnos() != null) {
			checkAnnos(keys, annos.getLongAnnos());
		}
		if (annos.getStringAnnos() != null) {
			checkAnnos(keys, annos.getStringAnnos());
		}
	}
	
	private static void checkAnnos(Set<String> keys, Collection<? extends AnnotationBase> annoCollection) {
		for (AnnotationBase ba : annoCollection) {
			if (!keys.add(ba.getKey())) {
				throw new InvalidModelException("Duplicate annotations found for key: " + ba.getKey());
			}
		}
	}

}
