package org.sagebionetworks.repo.manager.backup.migration;

import java.util.List;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.registry.MigrationSpecData;
import org.sagebionetworks.repo.model.registry.MigrationSpecData.FieldMigrationSpecData;

public class GenericMigrator implements RevisionMigrationStep {

	private MigrationSpecData migrationSpecData;
	
	public GenericMigrator() {
	}
	
	public GenericMigrator(MigrationSpecData m) {
		this.migrationSpecData = m;
	}
	
	public void setMigrationSpecData(MigrationSpecData m) {
		this.migrationSpecData = m;
	}
	
	public MigrationSpecData getMigrationSpecData() {
		return this.migrationSpecData;
	}

	@Override
	public NodeRevisionBackup migrateOneStep(NodeRevisionBackup toMigrate, EntityType type) {
		Annotations primaryAnnotations = null;
		Annotations additionalAnnotations = null;
		Annotations annots = null;
		List<FieldMigrationSpecData> lfmsd;
		
		lfmsd = migrationSpecData.getData(type);
		if (null != lfmsd) {
			if(toMigrate.getNamedAnnotations() != null && toMigrate.getNamedAnnotations().getPrimaryAnnotations() != null){
				primaryAnnotations = toMigrate.getNamedAnnotations().getPrimaryAnnotations();
			}
			if(toMigrate.getNamedAnnotations() != null && toMigrate.getNamedAnnotations().getAdditionalAnnotations() != null){
				additionalAnnotations = toMigrate.getNamedAnnotations().getAdditionalAnnotations();
			}
			for (FieldMigrationSpecData fmsd: lfmsd) {
				// Where to get field
				if (fmsd.getSrcBucket().equals("primary")) {
					annots = primaryAnnotations;

				} else {
					annots = additionalAnnotations;
				}
				// Where to send field
				if (fmsd.getDestBucket().equals("primary")) {
					MigrationHelper.migrateBucket(annots, primaryAnnotations, fmsd);
				} else
					if (fmsd.getDestBucket().equals("additional")){
						MigrationHelper.migrateBucket(annots, additionalAnnotations, fmsd);
					} else {
						MigrationHelper.deleteFromAnnotations(annots, fmsd);
					}
			}
		}
		return toMigrate;
	}
	
}
