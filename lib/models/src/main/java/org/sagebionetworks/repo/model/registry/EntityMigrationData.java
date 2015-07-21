package org.sagebionetworks.repo.model.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Provides data for migrating data.
 * 
 * @author jmhill
 *
 */
public class EntityMigrationData {
	
	private Map<EntityType, List<RenameFieldData>> renameMap;
	
	/**
	 * 
	 * this is a test, to support autowiring
	 */
	public EntityMigrationData() {
		
	}
	
	/**
	 * Build up this object using data
	 * @param data
	 */
	public EntityMigrationData(EntityMigration data){
		// Build up the rename map
		renameMap = new HashMap<EntityType, List<RenameFieldData>>();
		if(data.getToRename() != null){
			for(RenameData rename: data.getToRename()){
				if(rename.getEntityTypeName() == null) throw new IllegalArgumentException("'entityTypeName' cannot be null");
				// First look up the entity
				EntityType type = EntityType.valueOf(rename.getEntityTypeName());
				if(rename.getOldFieldName() == null) throw new IllegalArgumentException("'oldFieldName' must be provided for type: "+type.name());
				if(rename.getNewFieldName() == null) throw new IllegalArgumentException("'newFieldName' must be provided for type: "+type.name());
				List<RenameFieldData> list = renameMap.get(type);
				if(list == null){
					list = new ArrayList<RenameFieldData>();
					renameMap.put(type, list);
				}
				// Lookup the schema for this type
				try {
					String jsonString = (String) EntityTypeUtils.getClassForType(type).getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
					ObjectSchema schema = EntityFactory.createEntityFromJSONString(jsonString, ObjectSchema.class);
					// Lookup the schema for the field
					ObjectSchema fieldSchema = schema.getProperties().get(rename.getNewFieldName());
					if(fieldSchema == null) throw new IllegalArgumentException("Could not find schema for the property to rename using the new name: "+rename.getNewFieldName()+" for type: "+type.name());
					list.add(new RenameFieldData(rename.getOldFieldName(), rename.getNewFieldName(), fieldSchema));
				} catch (Exception e) {
					throw new RuntimeException(e);
				} 
			}
		}
	}
	
	/**
	 * Get the rename data for a given type.
	 * @param type
	 * @return
	 */
	public List<RenameFieldData> getRenameDataForEntity(EntityType type){
		return renameMap.get(type);
	}
	/**
	 * Data need to rename an Entity field.
	 *
	 */
	public static class RenameFieldData {
		String oldFieldName;
		String newFieldName;
		ObjectSchema fieldSchema;
		public String getOldFieldName() {
			return oldFieldName;
		}
		public String getNewFieldName() {
			return newFieldName;
		}
		public ObjectSchema getFieldSchema() {
			return fieldSchema;
		}
		public RenameFieldData(String oldFieldName, String newFieldName,
				ObjectSchema fieldSchema) {
			super();
			this.oldFieldName = oldFieldName;
			this.newFieldName = newFieldName;
			this.fieldSchema = fieldSchema;
		}
		@Override
		public String toString() {
			return "RenameFieldData [oldFieldName=" + oldFieldName
					+ ", newFieldName=" + newFieldName + ", fieldSchema="
					+ fieldSchema.getId() + "]";
		}
	}

}
