package org.sagebionetworks.repo.manager.backup.migration;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.EntityMigrationData;
import org.sagebionetworks.repo.model.registry.EntityMigrationData.RenameFieldData;
import org.sagebionetworks.schema.ENCODING;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.TYPE;

/**
 * The purpose of this step is to apply any migration data.
 * @author jmhill
 *
 */
public class ApplyMigrationData implements RevisionMigrationStep {
	
	private EntityMigrationData migrationData;
	
	/**
	 * Passed the data used to used to drive migration.
	 * @param migrationData
	 */
	public ApplyMigrationData(EntityMigrationData migrationData){
		this.migrationData = migrationData;
	}

	@Override
	public EntityType migrateOneStep(NodeRevisionBackup toMigrate,	EntityType type) {
		// If we have any fields to rename then now is the time to do it.
		List<RenameFieldData> renameList = migrationData.getRenameDataForEntity(type);
		if(renameList != null){
			renameFields(renameList, toMigrate, type);
		}
		return type;
	}

	/**
	 * Rename a 
	 * @param toRename
	 * @param toMigrate
	 * @param type
	 */
	private void renameFields(List<RenameFieldData> toRename, NodeRevisionBackup toMigrate, EntityType type) {
		// Find each field in the data
		for(RenameFieldData rename: toRename){
			// First determine if the
			if(toMigrate.getNamedAnnotations() != null && toMigrate.getNamedAnnotations().getPrimaryAnnotations() != null){
				Annotations primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
				TYPE fieldType = rename.getFieldSchema().getType();
				// First we must check the encoding
				ENCODING encoding = rename.getFieldSchema().getContentEncoding();
				if(encoding != null){
					if(ENCODING.BINARY == encoding){
						Map<String, List<byte[]>> annos = primaryAnnotations.getBlobAnnotations();
						if(annos != null){
							List<byte[]> valueToMigrate = annos.remove(rename.getOldFieldName());
							if(valueToMigrate != null){
								annos.put(rename.getNewFieldName(), valueToMigrate);
							}
						}
					}else{
						throw new IllegalArgumentException("Support has not been added to migrate field TYPE="+fieldType.name()+" for ENCODING: "+encoding+", toRename: "+rename.toString());						
					}
				}else if(TYPE.STRING == fieldType){
					if(rename.getFieldSchema().getFormat() != null){
						// This might be a date
						if(FORMAT.DATE_TIME == rename.getFieldSchema().getFormat()){
							// Migrate the Date annotations.
							Map<String, List<Date>> annos = primaryAnnotations.getDateAnnotations();
							if(annos != null){
								List<Date> valueToMigrate = annos.remove(rename.getOldFieldName());
								if(valueToMigrate != null){
									annos.put(rename.getNewFieldName(), valueToMigrate);
								}
							}
						}else{
							throw new IllegalArgumentException("Support has not been added to migrate field TYPE="+fieldType.name()+" for FORMAT: "+rename.getFieldSchema().getFormat()+", toRename: "+rename.toString());
						}
					}else{
						// Migrate the string annotations.
						Map<String, List<String>> stringAnnos = primaryAnnotations.getStringAnnotations();
						if(stringAnnos != null){
							List<String> valueToMigrate = stringAnnos.remove(rename.getOldFieldName());
							if(valueToMigrate != null){
								stringAnnos.put(rename.getNewFieldName(), valueToMigrate);
							}
						}
					}
				}else if(TYPE.INTEGER == fieldType){
					// Migrate the string annotations.
					Map<String, List<Long>> longAnnos = primaryAnnotations.getLongAnnotations();
					if(longAnnos != null){
						List<Long> valueToMigrate = longAnnos.remove(rename.getOldFieldName());
						if(valueToMigrate != null){
							longAnnos.put(rename.getNewFieldName(), valueToMigrate);
						}
					}
				}else if(TYPE.NUMBER == fieldType){
					// Migrate the string annotations.
					Map<String, List<Double>> annos = primaryAnnotations.getDoubleAnnotations();
					if(annos != null){
						List<Double> valueToMigrate = annos.remove(rename.getOldFieldName());
						if(valueToMigrate != null){
							annos.put(rename.getNewFieldName(), valueToMigrate);
						}
					}
				}else{
					throw new IllegalArgumentException("Support has not been added to migrate field TYPE="+fieldType.name()+" for : "+rename.toString());
				}
			}
		}
	}
}
