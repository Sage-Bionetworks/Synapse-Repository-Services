package org.sagebionetworks.tool.migration.v4.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;

public class ToolMigrationUtils {
	
	public static List<TypeToMigrateMetadata> buildTypeToMigrateMetadata(
		MigrationTypeCounts srcCounts, MigrationTypeCounts destCounts,
		List<MigrationType> typesToMigrate) {
		if (srcCounts == null) throw new IllegalArgumentException("srcCounts cannot be null.");
		if (destCounts == null) throw new IllegalArgumentException("destCounts cannot be null.");
		if (typesToMigrate == null) throw new IllegalArgumentException("typesToMigrate cannot be null.");
		
		List<TypeToMigrateMetadata> l = new LinkedList<TypeToMigrateMetadata>();
		for (MigrationType t: typesToMigrate) {
			MigrationTypeCount srcMtc = findMetadata(srcCounts, t);
			if (srcMtc == null) {
				throw new RuntimeException("Could not find type " + t.name() + " in source migrationTypeCounts");
			}
			MigrationTypeCount destMtc = findMetadata(destCounts, t);
			if (destMtc == null) {
				throw new RuntimeException("Could not find type " + t.name() + " in destination migrationTypeCounts");
			}
			TypeToMigrateMetadata data = new TypeToMigrateMetadata(t, srcMtc.getMinid(), srcMtc.getMaxid(), srcMtc.getCount(), destMtc.getMinid(), destMtc.getMaxid(), destMtc.getCount());
			l.add(data);
		}
		return l;
	}
	
	private static MigrationTypeCount findMetadata(MigrationTypeCounts tCounts, MigrationType t) {
		MigrationTypeCount tc = null;
		for (MigrationTypeCount c: tCounts.getList()) {
			if (c.getType().equals(t)) {
				tc = c;
				break;
			}
		}
		return tc;
	}

}
