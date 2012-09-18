package org.sagebionetworks.tool.migration.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
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
	
	/**
	 * Build a map from the list. The key will be entity MigratableObjectDescriptors.
	 * @param list
	 * @return
	 */
	public static Map<MigratableObjectDescriptor, MigratableObjectData> buildMigratableMapFromList(List<MigratableObjectData> list){
		Map<MigratableObjectDescriptor, MigratableObjectData> map = new HashMap<MigratableObjectDescriptor, MigratableObjectData>();
		Map<MigratableObjectType,Integer> mapStats = new HashMap<MigratableObjectType,Integer>();
		for(MigratableObjectData entity: list) {
			if (!map.containsKey(entity.getId())) { // only count entities once
				Integer typeCount = mapStats.get(entity.getId().getType());
				if (typeCount==null) typeCount=1; else typeCount++;
				mapStats.put(entity.getId().getType(), typeCount);
			}
			map.put(entity.getId(), entity);
		}
		System.out.println("Migratable list count by type: "+mapStats);
		return map;
	}
	
	
	/**
	 * Returns true iff the dependencies of 'objectToMigrate' already exist in 'destContents'
	 * @param objectToMigrate  the object to migrate and its dependencies
	 * @param destContents the contents of the desintation repository
	 * @return
	 */
	public static boolean dependenciesFulfilled(MigratableObjectData objectToMigrate, Collection<MigratableObjectDescriptor> destContents) {
		boolean result = true;
		for (MigratableObjectDescriptor dependency : objectToMigrate.getDependencies()) {
			if (!destContents.contains(dependency)) {
				result = false;
				break;
			}
		}
		return result;
	}


}
