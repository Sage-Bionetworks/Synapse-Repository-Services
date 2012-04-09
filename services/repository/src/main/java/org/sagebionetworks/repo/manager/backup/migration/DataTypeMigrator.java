package org.sagebionetworks.repo.manager.backup.migration;

import java.util.List;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.MigrationSpecData;
import org.sagebionetworks.repo.model.registry.MigrationSpecData.FieldMigrationSpecData;

// A one-time migrator for layer data into specialized layers
// Note: This must be executed as last step in migration, the type
public class DataTypeMigrator implements RevisionMigrationStep {

	private final String typeKey = "type";
	private MigrationSpecData migrationSpecData;
	
	public DataTypeMigrator() {
	}
	
	@Override
	public EntityType migrateOneStep(NodeRevisionBackup toMigrate, EntityType type) {
		Annotations primaryAnnotations = null;
		Annotations annots = null;
		List<FieldMigrationSpecData> lfmsd;
		
		// We only need to migrate layer entities
		if (EntityType.layer == type) {
			// We only look at primary annotations
			if(toMigrate.getNamedAnnotations() != null && toMigrate.getNamedAnnotations().getPrimaryAnnotations() != null){
				primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			}
			// Get type of layer
			String layerType = (String)primaryAnnotations.getSingleValue(typeKey);
			if (null == layerType)
				return type;
			// Only migrate C, E, G
			if (layerType.equals("C")) {
				// Remove tissueType, platform, releaseNotes, type
				primaryAnnotations.deleteAnnotation("tissueType");
				primaryAnnotations.deleteAnnotation("platform");
				primaryAnnotations.deleteAnnotation("type");
				type = EntityType.phenotypedata;
			} else if (layerType.equals("E")) {
				// Remove type
				primaryAnnotations.deleteAnnotation("type");
				type = EntityType.expressiondata;
			} else  if (layerType.equals("G")) {
				// Remove tissueType, type
				primaryAnnotations.deleteAnnotation("type");
				primaryAnnotations.deleteAnnotation("tissueType");
				type = EntityType.genotypedata;
			}
		}

		return type;
	}
	
}
