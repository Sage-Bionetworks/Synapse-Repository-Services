package org.sagebionetworks.tool.migration.v4.delta;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;

public class DeltaFinder {

	static private Log logger = LogFactory.getLog(DeltaFinder.class);

	private SynapseAdminClient sourceClient;
	private SynapseAdminClient destinationClient;
	TypeToMigrateMetadata typeToMigrateMeta;
	String salt;
	Long batchSize;
	
	public DeltaFinder(TypeToMigrateMetadata tm,
			SynapseAdminClient srcClient,
			SynapseAdminClient destClient,
			String salt,
			Long bSize) {
		typeToMigrateMeta = tm;
		sourceClient = srcClient;
		destinationClient = destClient;
		this.salt = salt;
		batchSize = bSize;
	}

	public DeltaRanges findDeltaRanges() throws SynapseException, JSONObjectAdapterException {
		DeltaRanges deltas = new DeltaRanges();
		deltas.setMigrationType(typeToMigrateMeta.getType());
		List<IdRange> insRanges = new LinkedList<IdRange>();
		List<IdRange> updRanges = new LinkedList<IdRange>();
		List<IdRange> delRanges = new LinkedList<IdRange>();
		
		// Source is empty
		if (typeToMigrateMeta.getSrcMinId() == null) {
			// Delete everything at destination if not empty
			if (typeToMigrateMeta.getDestMinId() != null) {
				IdRange r = new IdRange(typeToMigrateMeta.getDestMinId(), typeToMigrateMeta.getDestMaxId());
				delRanges.add(r);
				logger.debug("Source is empty for " + typeToMigrateMeta.getType() + ", added range " + r + " to delRanges.");
			}
		} else { // Source is not empty
			// Insert everything from destination if empty
			if (typeToMigrateMeta.getDestMinId() == null) {
				IdRange r = new IdRange(typeToMigrateMeta.getSrcMinId(), typeToMigrateMeta.getSrcMaxId());
				insRanges.add(r);
				logger.debug("Destination is empty for " + typeToMigrateMeta.getType() + ", added range " + r + " to insRanges.");
			} else { // Normal case
				Long updatesMinId = Math.max(typeToMigrateMeta.getSrcMinId(), typeToMigrateMeta.getDestMinId());
				Long updatesMaxId = Math.min(typeToMigrateMeta.getSrcMaxId(), typeToMigrateMeta.getDestMaxId());
				logger.debug("Update box from " + updatesMinId + " to " + updatesMaxId);
				
				if (updatesMinId > updatesMaxId) { // Disjoint
					IdRange r = new IdRange(typeToMigrateMeta.getSrcMinId(), typeToMigrateMeta.getSrcMaxId());
					insRanges.add(r);
					logger.debug("Added range " + r + " to insRanges.");
					r = new IdRange(typeToMigrateMeta.getDestMinId(), typeToMigrateMeta.getDestMaxId());
					delRanges.add(r);
					logger.debug("Added range " + r + " to delRanges.");
				} else {
					// Inserts and deletes ranges (these are ranges that do not overlap between source and destination
					// so either insert or delete depending where they occur
					if (typeToMigrateMeta.getSrcMinId() < updatesMinId) {
						IdRange r = new IdRange(typeToMigrateMeta.getSrcMinId(), updatesMinId-1);
						insRanges.add(r);
						logger.debug("Added range " + r + " to insRanges.");
					}
					if (typeToMigrateMeta.getSrcMaxId() > updatesMaxId) {
						IdRange r = new IdRange(updatesMaxId+1, typeToMigrateMeta.getSrcMaxId());
						insRanges.add(r);
						logger.debug("Added range " + r + " to insRanges.");
					}
					if (typeToMigrateMeta.getDestMinId() < updatesMinId) {
						IdRange r = new IdRange(typeToMigrateMeta.getDestMinId(), updatesMinId-1);
						delRanges.add(r);
						logger.debug("Added range " + r + " to delRanges.");
					}
					if (typeToMigrateMeta.getDestMaxId() > updatesMaxId) {
						IdRange r = new IdRange(updatesMaxId+1, typeToMigrateMeta.getDestMaxId());
						delRanges.add(r);
						logger.debug("Added range " + r + " to delRanges.");
					}
					
					// Update ranges
					updRanges.addAll(findUpdDeltaRanges(sourceClient, destinationClient, typeToMigrateMeta.getType(), salt, updatesMinId, updatesMaxId, batchSize));
				}

			}
		}
		
		// Update ranges
		
		deltas.setInsRanges(insRanges);
		deltas.setUpdRanges(updRanges);
		deltas.setDelRanges(delRanges);
		return deltas;
	}
	
	private List<IdRange> findUpdDeltaRanges(SynapseAdminClient srcClient, SynapseAdminClient destClient, MigrationType type, String salt, long minId, long maxId, long batchSize) throws SynapseException, JSONObjectAdapterException {
		List<IdRange> l = new LinkedList<IdRange>();
		MigrationRangeChecksum srcCrc32 = srcClient.getChecksumForIdRange(type, salt, minId, maxId);
		MigrationRangeChecksum destCrc32 = destClient.getChecksumForIdRange(type, salt, minId, maxId);
		//log.info("Computed range checksums from " + minId + " to " + maxId + ": (" + srcCrc32.getChecksum() + ", " + destCrc32.getChecksum() + ").");
		if (srcCrc32.equals(destCrc32)) {
			return l;
		} else {
			if (maxId - minId < batchSize) {
				IdRange r = new IdRange(minId, maxId);
				l.add(r);
				return l;
			} else { // Split
				long minId1 = minId;
				long maxId1 = (minId+maxId)/2;
				long minId2 = maxId1+1;
				long maxId2 = maxId;
				l.addAll(findUpdDeltaRanges(srcClient, destClient, type, salt, minId1, maxId1, batchSize));
				l.addAll(findUpdDeltaRanges(srcClient, destClient, type, salt, minId2, maxId2, batchSize));
				return l;
			}
		}
	}
	
}
