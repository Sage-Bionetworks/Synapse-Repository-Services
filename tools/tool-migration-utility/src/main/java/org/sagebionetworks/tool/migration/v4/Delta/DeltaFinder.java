package org.sagebionetworks.tool.migration.v4.Delta;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;

public class DeltaFinder {
	
	private SynapseAdminClient sourceClient;
	private SynapseAdminClient destinationClient;
	TypeToMigrateMetadata typeToMigrateMeta;
	
	public DeltaFinder(TypeToMigrateMetadata tm,
			SynapseAdminClient srcClient,
			SynapseAdminClient destClient) {
		typeToMigrateMeta = tm;
		sourceClient = srcClient;
		destinationClient = destClient;
	}

	public DeltaRanges findDeltaRanges() {
		DeltaRanges deltas = new DeltaRanges();
		deltas.setMigrationType(typeToMigrateMeta.getType());
		List<IdRange> insRanges = new LinkedList<IdRange>();
		List<IdRange> updRanges = new LinkedList<IdRange>();
		List<IdRange> delRanges = new LinkedList<IdRange>();
		
		Long updatesMinId = Math.max(typeToMigrateMeta.getSrcMinId(), typeToMigrateMeta.getDestMinId());
		Long updatesMaxId = Math.min(typeToMigrateMeta.getSrcMaxId(), typeToMigrateMeta.getDestMaxId());

		// Inserts and deletes ranges (these are ranges that do not overlap between source and destination
		// so either insert or delete depending where they occur
		if (typeToMigrateMeta.getSrcMinId() < updatesMinId) {
			IdRange r = new IdRange(typeToMigrateMeta.getSrcMinId(), updatesMinId-1);
			insRanges.add(r);
		}
		if (typeToMigrateMeta.getSrcMaxId() > updatesMaxId) {
			IdRange r = new IdRange(updatesMaxId+1, typeToMigrateMeta.getSrcMaxId());
			insRanges.add(r);
		}
		if (typeToMigrateMeta.getDestMinId() < updatesMinId) {
			IdRange r = new IdRange(typeToMigrateMeta.getDestMinId(), updatesMinId-1);
			delRanges.add(r);
		}
		if (typeToMigrateMeta.getDestMaxId() > updatesMaxId) {
			IdRange r = new IdRange(updatesMaxId+1, typeToMigrateMeta.getDestMaxId());
			delRanges.add(r);
		}
		
		// Update ranges
		
		deltas.setInsRanges(insRanges);
		deltas.setUpdRanges(updRanges);
		deltas.setDelRanges(delRanges);
		return deltas;
	}
	
}
