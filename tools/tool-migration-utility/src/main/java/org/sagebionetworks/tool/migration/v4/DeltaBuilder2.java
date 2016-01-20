package org.sagebionetworks.tool.migration.v4;

import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.tool.migration.v3.SynapseClientFactory;
import org.sagebionetworks.tool.migration.v3.stream.RowWriter;
import org.sagebionetworks.tool.migration.v4.delta.DeltaRanges;
import org.sagebionetworks.tool.migration.v4.delta.IdRange;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;
import org.sagebionetworks.tool.progress.BasicProgress;

public class DeltaBuilder2 {
	
	static private Log log = LogFactory.getLog(DeltaBuilder2.class);

	private RowWriter<RowMetadata> toCreate;
	private RowWriter<RowMetadata> toUpdate;
	private RowWriter<RowMetadata> toDelete;
	private DeltaRanges deltaRanges;
	private TypeToMigrateMetadata type;
	private SynapseClientFactory factory;
	private long batchSize;
	private BasicProgress sourceProgress = new BasicProgress();
	private BasicProgress destProgress = new BasicProgress();
	
	public DeltaBuilder2(SynapseClientFactory factory, long batchSize, TypeToMigrateMetadata typeMeta, DeltaRanges ranges,
			RowWriter<RowMetadata> toCrerate, RowWriter<RowMetadata> toUpdate, RowWriter<RowMetadata> toDelete) {
		this.type = typeMeta;
		this.deltaRanges = ranges;
		this.toCreate = toCreate;
		this.toUpdate = toUpdate;
		this.toDelete = toDelete;
		this.factory = factory;
		this.batchSize = batchSize;
	}
	
	public long addUnconditionalDeltas(List<IdRange> ranges, RowWriter<RowMetadata> out) throws SynapseException {
		long count = 0;
		for (IdRange r: ranges) {
			RangeMetadataIterator it = new RangeMetadataIterator(type.getType(), factory.createNewSourceClient(), batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
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
	
	public DeltaCounts addConditionalDeltas(List<IdRange> ranges) throws SynapseException {
		DeltaCounts counts = new DeltaCounts(0, 0, 0);
		for (IdRange r: ranges) {

			RangeMetadataIterator sourceIt = new RangeMetadataIterator(type.getType(), factory.createNewSourceClient(), batchSize, r.getMinId(), r.getMaxId(), sourceProgress);
			RangeMetadataIterator destIt = new RangeMetadataIterator(type.getType(), factory.createNewDestinationClient(), batchSize, r.getMinId(), r.getMaxId(), destProgress);
			DeltaCounts countsForRange = this.addConditionalDeltasForRange(sourceIt, destIt);
			
			counts.setCreate(counts.getCreate()+countsForRange.getCreate());
			counts.setUpdate(counts.getUpdate()+countsForRange.getUpdate());
			counts.setDelete(counts.getDelete()+countsForRange.getDelete());
		}
				
		log.info("Calculated the following counts for type: "+type.getType().name()+" Counts: "+counts);
		return counts;
	}
	
	public DeltaCounts addConditionalDeltasForRange(RangeMetadataIterator sourceIterator, RangeMetadataIterator destIterator) {
		// Walk both iterators
		long createCount = 0;
		long updateCount = 0;
		long deleteCount = 0;
		
		RowMetadata sourceRow = null;
		RowMetadata destRow = null;
		boolean nextSource = true;
		boolean nextDest = true;
		do {
			if (nextSource) {
				sourceRow = null;
			}
			if (nextDest) {
				destRow = null;
			}
			
			if (nextSource) {
				if (sourceIterator.hasNext()) {
					sourceRow = sourceIterator.next();
				}
				nextSource = false;
			}
			if (nextDest) {
				if (destIterator.hasNext()) {
					destRow = destIterator.next();
				}
				nextDest = false;
			}

			long sourceId = -1l;
			long destId = -1l;
			
			if (sourceRow != null) {
				sourceId = sourceRow.getId();
			}
			if (destRow != null) {
				destId = destRow.getId();
			}
			
			// At source and no more dest: insert and move src pointer
			if (sourceRow != null && destRow == null) {
				// Need to create the source object
				toCreate.write(sourceRow);
				createCount++;
				nextSource = true;
				nextDest = false;
				continue;
			}
			
			// At dest and no more source: delete and move src pointer
			if (destRow != null && sourceRow == null) {
				// Need to delete the destination object
				toDelete.write(destRow);
				deleteCount++;
				nextSource = false;
				nextDest = true;
				continue;
			}

			// Case where objects are in the destination but not the source.
			if (destId > sourceId) {
				// need to create the source
				toCreate.write(sourceRow);
				createCount++;
				nextSource = true;
				nextDest = false;
				continue;
			}
			
			// The case where objects are in the source but not the destination.
			if (sourceId > destId) {
				// We need to delete the destination
				toDelete.write(destRow);
				deleteCount++;
				nextDest = true;
				nextSource = false;
				continue;
			}
			
			// If the ids are equal then we compare the etags
			if (sourceId == destId) {
				// Is either null?
				if (sourceRow != null && destRow != null) {
					// Null etag check
					if (sourceRow.getEtag() == null) {
						if (destRow.getEtag() != null) {
							toUpdate.write(sourceRow);
							updateCount++;
						}
					} else {
						// Etags are not null so are they equal?
						if (!sourceRow.getEtag().equals(destRow.getEtag())) {
							toUpdate.write(sourceRow);
							updateCount++;
						}
					}
				}
				nextSource = true;
				nextDest = true;
				continue;
			}
			
		} while (sourceRow != null || destRow != null);
		
		// done
		return new DeltaCounts(createCount, updateCount, deleteCount);
	}
}
