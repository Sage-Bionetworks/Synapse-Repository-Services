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

public class MigrationSpecData {
	
	private Map<EntityType, List<FieldMigrationSpecData>> migrationSpecData;
	
	public MigrationSpecData() {
		migrationSpecData = new HashMap<EntityType, List<FieldMigrationSpecData>>();
	}
	
//	public MigrationSpecData(MigrationSpec data) {
//		migrationSpecData = new HashMap<EntityType, List<FieldMigrationSpecData>>();
//		this.setData(data);
//	}
//	
	public void setData(MigrationSpec data) {
		
		if (data.getMigrationMetadata() != null) {
			for (EntityTypeMigrationSpec ems: data.getMigrationMetadata()) {
				if ((ems.getEntityType() == null) || (ems.getFields() == null))
					throw new IllegalArgumentException("entityType and fields must be defined in EntityTypeMigrationSpec."); 
				String strEntityType = ems.getEntityType();
				EntityType type = EntityType.valueOf(strEntityType);
				List <FieldMigrationSpecData> lfmd = new ArrayList<FieldMigrationSpecData>();
				List <FieldMigrationSpec> lfms = ems.getFields();
				for (FieldMigrationSpec fms: lfms) {
					FieldDescription src = fms.getSource();
					FieldDescription dst = fms.getDestination();
					if ((! dst.getType().equals("")) && (! src.getType().equals(dst.getType()))) {
						// Only allow from String to Int
						if (! ((dst.getType().equals("integer")) && (src.getType().equals("string"))))
							throw new IllegalArgumentException("Type transformation supported only from string to integer.");
					}
					// Get field schemas
//					ObjectSchema srcSchema = null;
					ObjectSchema dstSchema = null;
					if (dst.getBucket().equals("primary")) {
					// If dest is primary, schema derived from actual schema
						try {
							String jsonString = (String) EntityTypeUtils.getClassForType(type).getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
							ObjectSchema schema = EntityFactory.createEntityFromJSONString(jsonString, ObjectSchema.class);
							// Lookup the schema for the field
							dstSchema = schema.getProperties().get(dst.getName());
							if (dstSchema == null)
								throw new IllegalArgumentException("Could not find schema for the property to rename using the new name: "+dst.getName()+" for type: "+type.name());
							// TODO: Add consistency check for specified type vs schema type
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					lfmd.add(new FieldMigrationSpecData(src.getName(), src.getBucket(), src.getType(), dst.getName(), dst.getBucket(), dst.getType(), dstSchema));
					
				}
				migrationSpecData.put(type, lfmd);
			}
		} else {
			throw new IllegalArgumentException("migrationMetadata cannot be null.");
		}
		return;
	}
	
	public List<FieldMigrationSpecData> getData(EntityType type) {
		return migrationSpecData.get(type);
	}
	
	//	Hack to return the primary field names that are going to be deleted and
	//	therefore are not in the schema for the type anymore.
	//	Needed to help with V0 to V1 conversion.
	public List<String> getPrimaryFieldsToDelete(EntityType type) {
		List<String> pfToDelete = new ArrayList<String>();
		List<FieldMigrationSpecData> l = this.getData(type);
		for (FieldMigrationSpecData f: l) {
			if ((f.getSrcBucket().equals("primary")) && (f.getDestBucket().equals("deleted")))
				pfToDelete.add(f.getSrcFieldName());
		}
		return pfToDelete;
	}
	
	// Same as John, allows to move to consistent format for primary and additional fields
	public static class FieldMigrationSpecData {
		private String srcFieldName;
		private String destFieldName;
		private String srcBucket;
		private String destBucket;
//		private ObjectSchema srcSchema;
		private String srcType;
		private String destType;
		private ObjectSchema destSchema;
		
		public String getSrcFieldName() {
			return srcFieldName;
		}
		
		public String getDestFieldName() {
			return destFieldName;
		}
		
		public String getSrcBucket() {
			return srcBucket;
		}
		
		public String getDestBucket() {
			return destBucket;
		}
		
		public String getSrcType() {
			return this.srcType;
		}
		
		public String getDestType() {
			return this.destType;
		}
//		public ObjectSchema getSrcSchema() {
//			return srcSchema;
//		}
//
		public ObjectSchema getDestSchema() {
			return destSchema;
		}
		
		public FieldMigrationSpecData(String srcName, String srcBucket, String srcType, String destName, String destBucket, String destType, ObjectSchema destSchema) {
			this.srcFieldName = srcName;
			this.destFieldName = destName;
			this.srcBucket = srcBucket;
			this.destBucket = destBucket;
//			this.srcSchema = srcSchema;
			this.srcType = srcType;
			this.destType = destType;
			this.destSchema = destSchema;
		}
	}
}
