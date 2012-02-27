package org.sagebionetworks.repo.manager.backup.migration;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.registry.MigrationSpecData.FieldMigrationSpecData;

public class MigrationHelper {
	public static void migrateBucket(Annotations srcAnnots, Annotations dstAnnots, FieldMigrationSpecData fmsd) {
		if (null != srcAnnots.getAllValues(fmsd.getSrcFieldName())) {
			Object srcData = srcAnnots.deleteAnnotation(fmsd.getSrcFieldName());
			dstAnnots.addAnnotation(fmsd.getDestFieldName(), srcData);
		}
		
		return;
	}
	
	public static void deleteFromAnnotations(Annotations srcAnnots, FieldMigrationSpecData fmsd) {
		if (null != srcAnnots.getAllValues(fmsd.getSrcFieldName())) {
			Object srcData = srcAnnots.deleteAnnotation(fmsd.getSrcFieldName());
		}
		
		return;
	}
	
}
