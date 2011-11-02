package org.sagebionetworks.repo.manager;

import java.util.Iterator;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;

/**
 * Converts an entity with annotations to a map
 * @author jmhill
 *
 */
public class EntityToMapUtil {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	/**
	 * Create a map using an entity and its annotations.
	 * @param <T>
	 * @param entity
	 * @return
	 */
	public static <T extends Entity> Map<String, Object> createMapFromEntity(EntityWithAnnotations<T> entity) {
		// Convert the object to the map
		@SuppressWarnings("unchecked")
		Map<String, Object> row = OBJECT_MAPPER.convertValue(entity.getEntity(),Map.class);
		Annotations annotations = entity.getAnnotations();
		addNewOnly(row, annotations.getStringAnnotations());
		addNewOnly(row, annotations.getDateAnnotations());
		addNewOnly(row, annotations.getLongAnnotations());
		addNewOnly(row, annotations.getDoubleAnnotations());
		return row;
	}
	
	/**
	 * Only add values that are not already in the map
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param toAdd
	 */
	public static <K> void addNewOnly(Map<String, Object> map, Map<String, ? extends K> toAdd){
		if(toAdd != null){
			Iterator<String> keyIt = toAdd.keySet().iterator();
			while(keyIt.hasNext()){
				String key = keyIt.next();
				if(!map.containsKey(key)){
					map.put(key, toAdd.get(key));
				}
			}
		}
	}
}
