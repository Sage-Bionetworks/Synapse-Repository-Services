package org.sagebionetworks.tool.migration.v4.Delta;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.tool.migration.v4.SynapseClientFactory;

public class DeltaFinder {
	
	private SynapseClientFactory clientFactory;
	private MigrationType migrationType;
	private int batchSize;
	private long minId;
	private long maxId;
	
	public DeltaFinder(SynapseClientFactory factory, MigrationType type, int bSize, long min, long max) {
		this.clientFactory = factory;
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
