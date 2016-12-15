package org.sagebionetworks.tool.migration.v4;

import java.util.List;
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

	private RowWriter<RowMetadata> toCreate;
	private RowWriter<RowMetadata> toUpdate;
	private RowWriter<RowMetadata> toDelete;
	private SynapseClientFactory factory;
	private DeltaRanges deltaRanges;
	private TypeToMigrateMetadata type;
	private long batchSize;
	private BasicProgress sourceProgress = new BasicProgress();
	private BasicProgress destProgress = new BasicProgress();
	
	public DeltaBuilder(SynapseClientFactory factory, long batchSize, TypeToMigrateMetadata typeMeta, DeltaRanges ranges,
			RowWriter<RowMetadata> toCreate, RowWriter<RowMetadata> toUpdate, RowWriter<RowMetadata> toDelete) {
		this.type = typeMeta;
		this.deltaRanges = ranges;
		this.toCreate = toCreate;
		this.toUpdate = toUpdate;
		this.toDelete = toDelete;
		this.batchSize = batchSize;
		this.factory = factory;
	}
	
	public long addInsertsFromSource() throws SynapseException {
		SynapseAdminClient srcClient = factory.createNewSourceClient();
		long deleteCount = DeltaBuilder.addUnconditionalDeltas(srcClient, type, batchSize, sourceProgress, this.deltaRanges.getInsRanges(), this.toCreate);
		return deleteCount;
	}
	
	public long addDeletesAtDestination() throws SynapseException {
		SynapseAdminClient destClient = factory.createNewDestinationClient();
		long deleteCount = DeltaBuilder.addUnconditionalDeltas(destClient, type, batchSize, destProgress, this.deltaRanges.getDelRanges(), this.toDelete);
		return deleteCount;
	}
	
	public DeltaCounts addDifferencesBetweenSourceAndDestination() throws Exception {
		SynapseAdminClient srcClient = factory.createNewSourceClient();
		SynapseAdminClient destClient = factory.createNewDestinationClient();
		List<IdRange> ranges = this.deltaRanges.getUpdRanges();
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

	public static long addUnconditionalDeltas(SynapseAdminClient client, TypeToMigrateMetadata type, long batchSize, BasicProgress progress, List<IdRange> ranges, RowWriter<RowMetadata> out) throws SynapseException {
		long count = 0;
		for (IdRange r: ranges) {
			RangeMetadataIterator it = new RangeMetadataIterator(type.getType(), client, batchSize, r.getMinId(), r.getMaxId(), progress);
			RowMetadata row = null;
			do {
				row = null;
				if (it.hasNext()) {
					row = it.next();
					if (row == null) {
						throw new IllegalStateException("Row should not be null!");
					}
					out.write(row);
					count++;
				}
			} while (row != null);
		}
		return count;
	}
	
}
