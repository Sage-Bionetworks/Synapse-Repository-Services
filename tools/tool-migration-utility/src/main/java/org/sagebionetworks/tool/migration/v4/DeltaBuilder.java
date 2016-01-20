package org.sagebionetworks.tool.migration.v4;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.tool.migration.v3.SynapseClientFactory;
import org.sagebionetworks.tool.migration.v3.stream.RowWriter;
import org.sagebionetworks.tool.migration.v4.delta.DeltaRanges;
import org.sagebionetworks.tool.migration.v4.delta.IdRange;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;
import org.sagebionetworks.tool.progress.BasicProgress;

public class DeltaBuilder {
	
	static private Log log = LogFactory.getLog(DeltaBuilder.class);

	private RowWriter<RowMetadata> toCreate;
	private RowWriter<RowMetadata> toUpdate;
	private RowWriter<RowMetadata> toDelete;
	private DeltaRanges deltaRanges;
	private TypeToMigrateMetadata type;
	private long batchSize;
	private BasicProgress sourceProgress = new BasicProgress();
	private BasicProgress destProgress = new BasicProgress();
	
	public DeltaBuilder(long batchSize, TypeToMigrateMetadata typeMeta, DeltaRanges ranges,
			RowWriter<RowMetadata> toCrerate, RowWriter<RowMetadata> toUpdate, RowWriter<RowMetadata> toDelete) {
		this.type = typeMeta;
		this.deltaRanges = ranges;
		this.toCreate = toCreate;
		this.toUpdate = toUpdate;
		this.toDelete = toDelete;
		this.batchSize = batchSize;
	}
	
	public long addUnconditionalDeltas(SynapseAdminClient client, List<IdRange> ranges, RowWriter<RowMetadata> out) throws SynapseException {
		long count = 0;
		for (IdRange r: ranges) {
			RangeMetadataIterator it = new RangeMetadataIterator(type.getType(), client, batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
			RowMetadata sourceRow = null;
			do {
				sourceRow = null;
				if (it.hasNext()) {
					sourceRow = it.next();
					out.write(sourceRow);
					count++;
				}
			} while (sourceRow != null);
		}
		return count;
	}
	
	public DeltaCounts addConditionalDeltas(SynapseAdminClient srcClient, SynapseAdminClient destClient, List<IdRange> ranges) throws Exception {
		DeltaCounts counts = new DeltaCounts(0, 0, 0);
		for (IdRange r: ranges) {
			RangeMetadataIterator sourceIt = new RangeMetadataIterator(type.getType(), srcClient, batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
			RangeMetadataIterator destIt = new RangeMetadataIterator(type.getType(), destClient, batchSize, r.getMinId(), r.getMaxId(), destProgress);
			RangeDeltaBuilder rangeBuilder = new RangeDeltaBuilder(sourceIt, destIt, toCreate, toUpdate, toDelete);
			DeltaCounts countsForRange = rangeBuilder.buildDeltaCounts();
			counts.setCreate(counts.getCreate()+countsForRange.getCreate());
			counts.setUpdate(counts.getUpdate()+countsForRange.getUpdate());
			counts.setDelete(counts.getDelete()+countsForRange.getDelete());
		}
				
		return counts;
	}

}
