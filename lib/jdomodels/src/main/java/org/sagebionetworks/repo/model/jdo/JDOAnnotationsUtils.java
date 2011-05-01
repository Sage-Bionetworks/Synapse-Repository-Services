package org.sagebionetworks.repo.model.jdo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;

/**
 * Helper utilities for converting between JDOAnnotations and Annotations (DTO).
 * 
 * @author jmhill
 *
 */
public class JDOAnnotationsUtils {
	
	/**
	 * Create a JDOAnnotations object, copying all values from the passed DTO.
	 * @param dto
	 * @return
	 * @throws InvalidModelException
	 */
	public static JDOAnnotations createFromDTO(Annotations dto){
		JDOAnnotations jdo = new JDOAnnotations();
		updateFromJdoFromDto(dto, jdo);
		return jdo;
	}

	/**
	 * Update the JDO from the DTO
	 * @param dto
	 * @param jdo
	 */
	@SuppressWarnings("unchecked")
	public static void updateFromJdoFromDto(Annotations dto, JDOAnnotations jdo) {
		if(dto.getId() != null){
			jdo.setId(Long.valueOf(dto.getId()));
		}
		jdo.setStringAnnotations((Set<JDOStringAnnotation>)createFromMap(jdo, dto.getStringAnnotations()));
		jdo.setDateAnnotations((Set<JDODateAnnotation>)createFromMap(jdo, dto.getDateAnnotations()));
		jdo.setLongAnnotations((Set<JDOLongAnnotation>)createFromMap(jdo, dto.getLongAnnotations()));
		jdo.setDoubleAnnotations((Set<JDODoubleAnnotation>)createFromMap(jdo, dto.getDoubleAnnotations()));
	}
	
	/**
	 * Create a new Annotations object from the JDO.
	 * @param jdo
	 * @return
	 */
	public static Annotations createFromJDO(JDOAnnotations jdo){
		if(jdo == null) throw new IllegalArgumentException("JDOAnnotations cannot be null");
		Annotations dto = new Annotations();
		if(jdo.getId() != null){
			dto.setId(jdo.getId().toString());
		}
		dto.setStringAnnotations(createFromSet(jdo.getStringAnnotations()));
		dto.setLongAnnotations(createFromSet(jdo.getLongAnnotations()));
		dto.setDateAnnotations(createFromSet(jdo.getDateAnnotations()));
		dto.setDoubleAnnotations(createFromSet(jdo.getDoubleAnnotations()));
		return dto;
	}
	
	/**
	 * Build up the map from the set.
	 * @param <A>
	 * @param set
	 * @return
	 */
	public static <A> Map<String, Collection<A>> createFromSet(Set<? extends JDOAnnotation<A>> set){
		Map<String, Collection<A>> map = new HashMap<String, Collection<A>>();
		if(set != null){
			Iterator<? extends JDOAnnotation<A>> it = set.iterator();
			while(it.hasNext()){
				JDOAnnotation<A> jdoAno = it.next();
				String key = jdoAno.getAttribute();
				A value = jdoAno.getValue();
				Collection<A> collection = map.get(key);
				if(collection == null){
					collection = new ArrayList<A>();
					map.put(key, collection);
				}
				collection.add(value);
			}
		}
		return map;
	}
	
	/**
	 * Create a set of JDOAnnoations from a map
	 * @param <T>
	 * @param owner
	 * @param annotation
	 * @return
	 */
	public static <T> Set<? extends JDOAnnotation<T>> createFromMap(JDOAnnotations owner, Map<String, Collection<T>> annotation){
		Set<JDOAnnotation<T>> set = new HashSet<JDOAnnotation<T>>();
		if(annotation != null){
			Iterator<String> keyIt = annotation.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				Collection<T> valueColection = annotation.get(key);
				Iterator<T> valueIt = valueColection.iterator();
				while(valueIt.hasNext()){
					T value = valueIt.next();
					JDOAnnotation<T> jdo = createAnnotaion(owner, key, value);
					set.add(jdo);
				}
			}
		}
		return set;
	}
	
	/**
	 * Create a single JDOAnnotation.
	 * @param <T>
	 * @param owner
	 * @param key
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> JDOAnnotation<T> createAnnotaion(JDOAnnotations owner, String key, T value){
		if(key == null) throw new IllegalArgumentException("Key cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		JDOAnnotation<T>  jdo = null;
		if(value instanceof String){
			JDOStringAnnotation temp =  new JDOStringAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Date){
			JDODateAnnotation temp =  new JDODateAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Long){
			JDOLongAnnotation temp =  new JDOLongAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else if(value instanceof Double){
			JDODoubleAnnotation temp =  new JDODoubleAnnotation();
			temp.setOwner(owner);
			jdo = (JDOAnnotation<T>) temp;
		}else{
			throw new IllegalArgumentException("Unknown annoation type: "+value.getClass());
		}
		jdo.setAttribute(key);
		jdo.setValue(value);
		return jdo;
	}

}
