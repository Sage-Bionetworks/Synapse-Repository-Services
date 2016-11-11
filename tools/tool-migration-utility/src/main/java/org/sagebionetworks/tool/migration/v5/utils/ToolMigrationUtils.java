package org.sagebionetworks.tool.migration.v5.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.tool.migration.v4.utils.MigrationTypeCountDiff;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;
import org.sagebionetworks.tool.migration.v5.AsyncMigrationWorker;
import org.sagebionetworks.tool.progress.BasicProgress;

public class ToolMigrationUtils {
	
	public static List<TypeToMigrateMetadata> buildTypeToMigrateMetadata(
		List<MigrationTypeCount> srcCounts, List<MigrationTypeCount> destCounts,
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
	
	private static MigrationTypeCount findMetadata(List<MigrationTypeCount> tCounts, MigrationType t) {
		MigrationTypeCount tc = null;
		for (MigrationTypeCount c: tCounts) {
			if (c.getType().equals(t)) {
				tc = c;
				break;
			}
		}
		return tc;
	}
	
	public static List<MigrationTypeCount> getTypeCounts(SynapseAdminClient conn) throws SynapseException, InterruptedException, JSONObjectAdapterException {
		List<MigrationTypeCount> typeCounts = new LinkedList<MigrationTypeCount>();
		List<MigrationType> types = conn.getMigrationTypes().getList();
		for (MigrationType t: types) {
			try {
				MigrationTypeCount c = getTypeCount(conn, t);
				typeCounts.add(c);
			} catch (org.sagebionetworks.client.exceptions.SynapseBadRequestException e) {
				// Unsupported types not added to list 
			}
		}
		return typeCounts;
	}
	
	public static MigrationTypeCount getTypeCount(SynapseAdminClient conn, MigrationType type) throws SynapseException, InterruptedException, JSONObjectAdapterException {
		AsyncMigrationTypeCountRequest req = new AsyncMigrationTypeCountRequest();
		req.setType(type.name());
		BasicProgress progress = new BasicProgress();
		AsyncMigrationWorker worker = new AsyncMigrationWorker(conn, req, 600000, progress);
		AsyncMigrationResponse resp = worker.call();
		AsyncMigrationTypeCountResult res = (AsyncMigrationTypeCountResult)resp;
		return res.getCount();
	}
	
	public static MigrationRangeChecksum getChecksumForIdRange(SynapseAdminClient conn, MigrationType type, String salt, Long minId, Long maxId) throws SynapseException, InterruptedException, JSONObjectAdapterException {
		AsyncMigrationRangeChecksumRequest req = new AsyncMigrationRangeChecksumRequest();
		req.setType(type.name());
		req.setSalt(salt);
		req.setMinId(minId);
		req.setMaxId(maxId);
		BasicProgress progress = new BasicProgress();
		AsyncMigrationWorker worker = new AsyncMigrationWorker(conn, req, 600000, progress);
		AsyncMigrationResponse resp = worker.call();
		AsyncMigrationRangeChecksumResult res = (AsyncMigrationRangeChecksumResult)resp;
		return res.getChecksum();
	}
	
	public static List<RowMetadata> getRowMetadataByRange(SynapseAdminClient conn, MigrationType type, Long minId, Long maxId, Long batchSize, Long offset) throws SynapseException, InterruptedException, JSONObjectAdapterException {
		AsyncMigrationRowMetadataRequest req = new AsyncMigrationRowMetadataRequest();
		req.setType(type.name());
		req.setMinId(minId);
		req.setMaxId(maxId);
		req.setLimit(batchSize);
		req.setOffset(offset);
		BasicProgress progress = new BasicProgress();
		AsyncMigrationWorker worker = new AsyncMigrationWorker(conn, req, 600000, progress);
		AsyncMigrationResponse resp = worker.call();
		AsyncMigrationRowMetadataResult res = (AsyncMigrationRowMetadataResult)resp;
		return res.getRowMetadata().getList();
	}
	
	//	Temporary hacks to get the lists in sync
	//	Get a set of MigrationTypes from a list of MigrationTypeCounts
	public static Set<MigrationType> getTypesFromTypeCounts(List<MigrationTypeCount> typeCounts) {
		Set<MigrationType> s = new HashSet<MigrationType>();
		for (MigrationTypeCount mtc: typeCounts) {
			s.add(mtc.getType());
		}
		return s;
	}
	
	//	Given a list of types, only keep the MigrationTypeCounts that match these types
	public static List<MigrationTypeCount> filterSourceByDestination(List<MigrationTypeCount> srcTypeCounts, Set<MigrationType> destTypes) {
		List<MigrationTypeCount> toKeep = new LinkedList<MigrationTypeCount>();
		for (MigrationTypeCount mtc: srcTypeCounts) {
			if (destTypes.contains(mtc.getType())) {
				toKeep.add(mtc);
			}
		}
		return toKeep;
	}
	
	//	Given a list of types, only keep the MigrationTypes that match
	public static List<MigrationType> filterTypes(List<MigrationType> toFilter, Set<MigrationType> filter) {
		List<MigrationType> toKeep = new LinkedList<MigrationType>();
		for (MigrationType mt: toFilter) {
			if (filter.contains(mt)) {
				toKeep.add(mt);
			}
		}
		return toKeep;
	}
	
	public static List<MigrationTypeCountDiff> getMigrationTypeCountDiffs(List<MigrationTypeCount> srcCounts, List<MigrationTypeCount> destCounts) {
		List<MigrationTypeCountDiff> result = new LinkedList<MigrationTypeCountDiff>();
		Map<MigrationType, Long> mapSrcCounts = new HashMap<MigrationType, Long>();
		for (MigrationTypeCount sMtc: srcCounts) {
			mapSrcCounts.put(sMtc.getType(), sMtc.getCount());
		}
		// All migration types of source should be at destination
		// Note: unused src migration types are covered, they're not in destination results
		for (MigrationTypeCount mtc: destCounts) {
			MigrationTypeCountDiff outcome = 	new MigrationTypeCountDiff(mtc.getType(), (mapSrcCounts.containsKey(mtc.getType()) ? mapSrcCounts.get(mtc.getType()) : null), mtc.getCount());
			result.add(outcome);
		}
		return result;
	}
	
}
