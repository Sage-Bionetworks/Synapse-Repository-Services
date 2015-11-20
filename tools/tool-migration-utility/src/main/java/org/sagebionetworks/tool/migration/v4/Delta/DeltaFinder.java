package org.sagebionetworks.tool.migration.v4.Delta;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DeltaFinder {
	
	private SynapseAdminClient sourceClient;
	private SynapseAdminClient destinationClient;
	private MigrationType migrationType;
	private int batchSize;
	private long minId;
	private long maxId;
	
	public DeltaFinder(SynapseAdminClient srClient, SynapseAdminClient destClient, MigrationType type, int bSize, long min, long max) {
		this.sourceClient = sourceClient;
		this.destinationClient = destClient;
		this.migrationType = type;
		this.batchSize = bSize;
		this.minId = min;
		this.maxId = max;
	}
	
	public DeltaRanges findDeltaRanges() {
		List<IdRange> ranges = new LinkedList<IdRange>();
		DeltaRanges deltas = new DeltaRanges(this.migrationType, ranges);
		
		
		// Keep looking until range smaller than batch size
		while (maxId - minId >= batchSize) {
			
		}
		
		if (maxId - minId < batchSize) {
			IdRange r = new IdRange(minId, maxId);
			ranges.add(r);
		}
		
		return deltas;
	}
	
	public void findDelta() {
		
	}

}
