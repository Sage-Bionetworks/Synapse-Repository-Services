package org.sagebionetworks.tool.migration.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.tool.migration.dao.EntityData;

public class JobUtil {
	/**
	 * Build a map from the list. The key will be entity ids.
	 * @param list
	 * @return
	 */
	public static Map<String, EntityData> buildMapFromList(List<EntityData> list){
		Map<String, EntityData> map = new HashMap<String, EntityData>();
		for(EntityData entity: list){
			map.put(entity.getEntityId(), entity);
		}
		return map;
	}
}
