package org.sagebionetworks.tool.migration.v3;

import java.util.Iterator;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.tool.migration.v3.stream.RowWriter;

/**
 * Builds the deltas between the two stacks.
 * 
 * @author John
 *
 */
public class DeltaBuilder implements Callable<DeltaCounts> {

	Iterator<RowMetadata> sourceIterator;
	Iterator<RowMetadata> destIterator;
	RowWriter<RowMetadata> toCreate;
	RowWriter<RowMetadata> toUpdate;
	RowWriter<RowMetadata> toDelete;
	Long maxSourceId;


	public DeltaBuilder(Iterator<RowMetadata> sourceIterator, Iterator<RowMetadata> destIterator,
		RowWriter<RowMetadata> toCreate, RowWriter<RowMetadata> toUpdate, RowWriter<RowMetadata> toDelete,
		Long maxSourceId) {
		super();
		if(sourceIterator == null) throw new IllegalArgumentException("Source cannot be null");
		if(destIterator == null) throw new IllegalArgumentException("Destination cannot be null");
		this.sourceIterator = sourceIterator;
		this.destIterator = destIterator;
		this.toCreate = toCreate;
		this.toUpdate = toUpdate;
		this.toDelete = toDelete;
		this.maxSourceId = maxSourceId;
	}



	@Override
	public DeltaCounts call() throws Exception {
		// Walk both iterators
		long createCount = 0;
		long updateCount = 0;
		long deleteCount = 0;
		
		RowMetadata sourceRow = null;
		RowMetadata destRow = null;
		boolean nextSource = true;
		boolean nextDest = true;
		do{
			if(nextSource){
				sourceRow = sourceIterator.next();
				nextSource = false;
			}
			if(nextDest){
				destRow = destIterator.next();
				nextDest = false;
			}

			long sourceId = -1l;
			long destId = -1l;
			if(sourceRow != null){
				sourceId = sourceRow.getId();
				if ((maxSourceId != null) && (sourceId > maxSourceId)) {
					sourceId = -1L;
					sourceRow = null;
				}
			}
			if(destRow != null){
				destId = destRow.getId();
			}
			if(sourceRow != null && destRow == null){
				// Need to create the source object
				toCreate.write(sourceRow);
				createCount++;
				nextSource = true;
				nextDest = false;
				continue;
			}
			if(destRow != null && sourceRow == null){
				// Need to delete the destination object
				toDelete.write(destRow);
				deleteCount++;
				nextSource = false;
				nextDest = true;
				continue;
			}

			// Case where objects are in the destination but not the source.
			if(destId > sourceId){
				// need to create the source
				toCreate.write(sourceRow);
				createCount++;
				nextSource = true;
				nextDest = false;
				continue;
			}
			// The case where objects are in the source but not the destination.
			if(sourceId > destId){
				// We need to delete the destination
				toDelete.write(destRow);
				deleteCount++;
				nextDest = true;
				nextSource = false;
				continue;
			}
			// If the ids are equal then we compare the etags
			if(sourceId == destId){
				// Is either null?
				if(sourceRow != null && destRow != null){
					// Null etag check
					if(sourceRow.getEtag() == null){
						if(destRow.getEtag() != null){
							toUpdate.write(sourceRow);
							updateCount++;
						}
					}else{
						// Etags are not null so are they equal?
						if(!sourceRow.getEtag().equals(destRow.getEtag())){
							toUpdate.write(sourceRow);
							updateCount++;
						}
					}
				}
				nextSource = true;
				nextDest = true;
				continue;
			}
			
		}while(sourceRow != null || destRow != null);
		
		// done
		return new DeltaCounts(createCount, updateCount, deleteCount);
	}
	
	
	
}
